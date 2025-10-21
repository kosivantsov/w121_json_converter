import groovy.json.JsonSlurper
import org.apache.commons.io.FilenameUtils
import org.apache.poi.xwpf.usermodel.*
import org.apache.poi.util.Units
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*
import java.awt.Color
import java.util.regex.Pattern
import org.apache.poi.xwpf.usermodel.Document as PICTURE_DOC
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.apache.poi.xwpf.usermodel.BreakType

class JsonToDocxScript {

    String inputPath
    String outputPath
    String stringsPath
    String lang

    private boolean haveBatik = true
    private final Color NOTES_BG = new Color(0xB0, 0xC7, 0xFF)
    private final Color NOTES_TEXT = new Color(0xFF, 0xFF, 0xFF)
    private final Color NOTES_BORDER = new Color(0x8F, 0xB2, 0xFF)
    private final Color TICK_BLUE = new Color(0x0B, 0x3D, 0x91)
    private final Color ACCENT = new Color(0x0D, 0x47, 0xA1)
    private final Color BAND_BG = new Color(0x0B, 0x3D, 0x91)
    private final Color BAND_TEXT = new Color(0xFF, 0xFF, 0xFF)
    private final Color DEFINITION_BG = new Color(0xA1, 0xBC, 0xFF)
    private final String SANS = "Segoe UI"
    private final String SERIF = "Noto Serif"
    private final String HAND_FONT = "Playpen Sans"

    void run() {
        def curFile = new File(inputPath)
        if (!curFile.exists()) {
            throw new RuntimeException("Error: Input file not found at '${curFile.absolutePath}'")
        }

        def outDocx = new File(outputPath)
        def stringsFile = (stringsPath != null && !stringsPath.isEmpty()) ? new File(stringsPath) : null
        def targetLanguage = lang ?: Locale.default.toLanguageTag() ?: 'en-GB'

        try { Class.forName('org.apache.batik.transcoder.image.PNGTranscoder') } catch (Throwable t) { haveBatik = false }

        def slurper = new JsonSlurper()
        
        // Parse raw data and then preprocess it (for {soft_break})
        def rawData = slurper.parse(curFile)
        def data = preprocessJsonContent(rawData)

        def stringsData = [:]
        if (stringsFile?.exists()) {
            def rawStrings = slurper.parse(stringsFile)
            stringsData = preprocessJsonContent(rawStrings)
        } else if (stringsPath) {
            println "Warning: Strings file not found at '${stringsFile.absolutePath}'"
        }

        println "Generating DOCX for '${curFile.name}'..."
        buildDocx(data as Map, stringsData as Map, outDocx, targetLanguage)
        println "Wrote: ${outDocx.absolutePath}"
    }
    
    // Recursively traverse JSON and replace a single newline inside *...* with {soft_break}
    def preprocessJsonContent(obj) {
        if (obj instanceof Map) {
            obj.each { k, v -> obj[k] = preprocessJsonContent(v) }
        } else if (obj instanceof List) {
            for (int i = 0; i < obj.size(); i++) obj[i] = preprocessJsonContent(obj[i])
        } else if (obj instanceof String) {
            return obj.replaceAll(/(?m)(^\*[^\r\n]*)\r?\n([^\r\n]*\*$)/, '$1{soft_break}$2')
        }
        return obj
    }
    
    List parseLineToStyledSpans(String line) {
        def PAIR_BOLD_RE = Pattern.compile(/\*\*(.+?)\*\*/)
        def parts = []
        if (line == null) return parts
        def mb = PAIR_BOLD_RE.matcher(line)
        int cursor = 0
        while (mb.find()) {
            if (mb.start() > cursor) parts << [cursor, mb.start(), false]
            parts << [mb.start(1), mb.end(1), true]
            cursor = mb.end()
        }
        if (cursor < line.length()) parts << [cursor, line.length(), false]

        def chars = line.toCharArray()
        def singleIdx = []
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '*') {
                if (!(i + 1 < chars.length && chars[i + 1] == '*') && !(i - 1 >= 0 && chars[i - 1] == '*')) {
                    singleIdx.add(i)
                }
            }
        }
        
        int firstNonWs = -1
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) { firstNonWs = i; break }
        }
        
        int bulletPos = -1
        if (!singleIdx.isEmpty() && singleIdx.size() % 2 == 1 && firstNonWs != -1 && singleIdx[0] == firstNonWs) {
            bulletPos = singleIdx.remove(0)
        }

        def italPairs = []
        for (int k = singleIdx.size() - 2; k >= 0; k -= 2) italPairs.add([singleIdx[k], singleIdx[k+1]])
        def startSet = new HashSet(italPairs.collect { it[0] })
        def endSet = new HashSet(italPairs.collect { it[1] })

        def out = []
        def append = { String t, boolean b, boolean i ->
            if (t.isEmpty()) return
            if (!out.isEmpty() && out.last()[1] == b && out.last()[2] == i) out.last()[0] += t
            else out << [t, b, i]
        }

        parts.each { seg ->
            int a = seg[0], b = seg[1]
            boolean isBold = seg[2]
            def currentText = new StringBuilder()
            boolean inItalic = false
            for (int i = a; i < b; ++i) {
                if (bulletPos == i) {
                    append(currentText.toString(), isBold, inItalic)
                    currentText.delete(0, currentText.length())
                    append("•", isBold, false)
                } else if (startSet.contains(i)) {
                    append(currentText.toString(), isBold, inItalic)
                    currentText.delete(0, currentText.length())
                    inItalic = true
                } else if (endSet.contains(i)) {
                    append(currentText.toString(), isBold, inItalic)
                    currentText.delete(0, currentText.length())
                    inItalic = false
                } else {
                    currentText.append(line[i])
                }
            }
            append(currentText.toString(), isBold, inItalic)
        }
        return out
    }
    
    String rgbHex(Color c) { String.format("%02X%02X%02X", c.red, c.green, c.blue) }
    int cm(double v) { (int) Math.round(v * 567.0) }
    int pt(double v) { (int) Math.round(v * 20.0) }

    void setParagraphShading(XWPFParagraph p, Color fillOrNull) {
        if (fillOrNull == null) return
        def ctp = p.getCTP()
        def pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr()
        def shd = pPr.isSetShd() ? pPr.getShd() : pPr.addNewShd()
        shd.setVal(STShd.CLEAR)
        shd.setColor("auto")
        shd.setFill(rgbHex(fillOrNull))
    }

    void setParagraphBorderAll(XWPFParagraph p, String colorHex, int szTwip, int space) {
        def ctp = p.getCTP()
        def pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr()
        def pBdr = pPr.isSetPBdr() ? pPr.getPBdr() : pPr.addNewPBdr()
        ['top', 'left', 'bottom', 'right'].each { borderName ->
            def border = pBdr."addNew${borderName.capitalize()}"()
            border.setVal(STBorder.SINGLE)
            border.setSz(BigInteger.valueOf(szTwip))
            border.setColor(colorHex)
            border.setSpace(BigInteger.valueOf(space))
        }
    }
    
    void setPara(XWPFParagraph p, ParagraphAlignment a = null, Integer beforePt = null, Integer afterPt = null, Double leftCm = null, Double rightCm = null) {
        if (a) p.setAlignment(a)
        def ctp = p.getCTP()
        def ppr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr()
        if (beforePt != null || afterPt != null) {
            def sp = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing()
            if (beforePt != null) sp.setBefore(BigInteger.valueOf(pt(beforePt)))
            if (afterPt != null) sp.setAfter(BigInteger.valueOf(pt(afterPt)))
        }
        if (leftCm != null || rightCm != null) {
            def ind = ppr.isSetInd() ? ppr.getInd() : ppr.addNewInd()
            if (leftCm != null) ind.setLeft(BigInteger.valueOf(cm(leftCm)))
            if (rightCm != null) ind.setRight(BigInteger.valueOf(cm(rightCm)))
        }
    }

    void ensureSectPr(XWPFDocument doc) {
        def body = doc.document.body
        def sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr()
        def pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar()
        pgMar.setTop(BigInteger.valueOf(cm(2.0)))
        pgMar.setBottom(BigInteger.valueOf(cm(2.0)))
        pgMar.setLeft(BigInteger.valueOf(cm(2.0)))
        pgMar.setRight(BigInteger.valueOf(cm(2.0)))
    }
    
    XWPFStyles ensureStyles(XWPFDocument doc) {
        def styles = doc.getStyles()
        if (styles == null) {
            doc.createStyles()
            styles = doc.getStyles()
        }
        return styles
    }

    XWPFStyle ensureParagraphStyle(XWPFDocument doc, String styleId, Closure cfg = null) {
        def styles = ensureStyles(doc)
        def xs = styles.getStyle(styleId)
        if (xs != null) return xs
        def ct = CTStyle.Factory.newInstance()
        ct.setStyleId(styleId)
        ct.addNewName().setVal(styleId)
        if (cfg != null) cfg(ct)
        def x = new XWPFStyle(ct)
        x.setType(STStyleType.PARAGRAPH)
        styles.addStyle(x)
        return x
    }

    XWPFStyle ensureCharacterStyle(XWPFDocument doc, String styleId, Closure cfg = null) {
        def styles = ensureStyles(doc)
        def xs = styles.getStyle(styleId)
        if (xs != null) return xs
        def ct = CTStyle.Factory.newInstance()
        ct.setStyleId(styleId)
        ct.addNewName().setVal(styleId)
        if (cfg != null) cfg(ct)
        def x = new XWPFStyle(ct)
        x.setType(STStyleType.CHARACTER)
        styles.addStyle(x)
        return x
    }

    void setThemeFonts(XWPFDocument doc, String langCode) {
        ensureParagraphStyle(doc, "Normal") { ct ->
            def rpr = ct.isSetRPr() ? ct.getRPr() : ct.addNewRPr()
            def f = rpr.addNewRFonts(); f.setAscii(SANS); f.setHAnsi(SANS); f.setCs(SANS)
            rpr.addNewSz().setVal(BigInteger.valueOf(22))
            rpr.addNewSzCs().setVal(BigInteger.valueOf(22))
            if (langCode) rpr.addNewLang().setVal(langCode)
        }
        ensureParagraphStyle(doc, "Scripture") { ct ->
            def rpr = ct.isSetRPr() ? ct.getRPr() : ct.addNewRPr()
            def f = rpr.addNewRFonts(); f.setAscii(SERIF); f.setHAnsi(SERIF); f.setCs(SERIF)
            rpr.addNewSz().setVal(BigInteger.valueOf(23))
            rpr.addNewSzCs().setVal(BigInteger.valueOf(23))
            if (langCode) rpr.addNewLang().setVal(langCode)
        }
        ensureParagraphStyle(doc, "UniformTitle") { ct ->
            def ppr = ct.addNewPPr(); ppr.addNewJc().setVal(STJc.CENTER)
            def rpr = ct.isSetRPr() ? ct.getRPr() : ct.addNewRPr(); rpr.addNewB().setVal(true)
            def f = rpr.addNewRFonts(); f.setAscii(SANS); f.setHAnsi(SANS); f.setCs(SANS)
            rpr.addNewSz().setVal(BigInteger.valueOf(25))
            rpr.addNewSzCs().setVal(BigInteger.valueOf(25))
            if (langCode) rpr.addNewLang().setVal(langCode)
        }
        ["Subtitle","NotesPara","DefinitionPara","CuePara","KeyPointPara","CalloutPara","H4Notes","H4Recap","BandPara", "ReflectionPara"].each { name ->
            ensureParagraphStyle(doc, name) { ct ->
                def rpr = ct.isSetRPr() ? ct.getRPr() : ct.addNewRPr()
                def f = rpr.addNewRFonts()
                if (name == "ReflectionPara" || name == "KeyPointPara") {
                    f.setAscii(HAND_FONT); f.setHAnsi(HAND_FONT); f.setCs(HAND_FONT)
                    rpr.addNewI().setVal(true)
                } else {
                    f.setAscii(SANS); f.setHAnsi(SANS); f.setCs(SANS)
                }
                if (langCode) rpr.addNewLang().setVal(langCode)
            }
        }
        ["H4Notes","H4Recap"].each { name ->
            def st = ensureParagraphStyle(doc, name) { }
            st.getCTStyle().addNewRPr().addNewB().setVal(true)
        }
        ensureCharacterStyle(doc, "TickChar") { ct -> ct.addNewRPr().addNewColor().setVal(rgbHex(TICK_BLUE)) }
        ensureCharacterStyle(doc, "EmChar") { ct -> ct.addNewRPr().addNewI().setVal(true) }
    }
    
    List makeBlockTable(XWPFDocument doc, Color fill, Color border) {
        def tbl = doc.createTable(1, 1)
        tbl.setTableAlignment(TableRowAlign.CENTER)
        def cell = tbl.getRow(0).getCell(0)
        def cttc = cell.getCTTc()
        def tcPr = cttc.isSetTcPr() ? cttc.getTcPr() : cttc.addNewTcPr()
        def shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd()
        shd.setFill(rgbHex(fill))
        def tcBorders = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders()
        ['Top', 'Left', 'Bottom', 'Right'].each { side ->
            def b = tcBorders."addNew${side}"()
            b.setVal(STBorder.SINGLE)
            b.setSz(BigInteger.valueOf(12))
            b.setColor(rgbHex(border))
        }
        def tcMar = tcPr.isSetTcMar() ? tcPr.getTcMar() : tcPr.addNewTcMar()
        tcMar.addNewTop().with { it.setW(BigInteger.valueOf(cm(0.15))); it.setType(STTblWidth.DXA) }
        tcMar.addNewBottom().with { it.setW(BigInteger.valueOf(cm(0.15))); it.setType(STTblWidth.DXA) }
        tcMar.addNewLeft().with { it.setW(BigInteger.valueOf(cm(0.2))); it.setType(STTblWidth.DXA) }
        tcMar.addNewRight().with { it.setW(BigInteger.valueOf(cm(0.2))); it.setType(STTblWidth.DXA) }
        return [tbl, cell]
    }

    boolean isPureNumber(String s) { s?.trim() ==~ /\d{1,3}/ }

    List splitTicks(String text) {
        def TICK_RE = ~/`([^`]+)`/
        def out = []
        if (text == null) return out
        def m = TICK_RE.matcher(text)
        int last = 0
        while (m.find()) {
            if (m.start() > last) out << ["text", text.substring(last, m.start())]
            def inner = m.group(1)
            out << [isPureNumber(inner) ? "tick-num" : "tick-text", inner]
            last = m.end()
        }
        if (last < text.length()) out << ["text", text.substring(last)]
        return out
    }
    
    // Proper soft break handling via BreakType.TEXT_WRAPPING
    void addInlineToParagraph(XWPFParagraph p, String text, boolean isNotes) {
        if (text == null) return
        text.split('\n', -1).eachWithIndex { line, idx ->
            if (idx > 0) p.createRun().addBreak() // paragraph line break between input lines

            parseLineToStyledSpans(line).each { span ->
                def (spanText, isBold, isItalic) = span
                
                splitTicks(spanText).each { tup ->
                    def (kind, content) = tup
                    def parts = content.split('\\{soft_break\\}', -1)
                    parts.eachWithIndex { part, i ->
                        def r = p.createRun()
                        r.setText(part)
                        
                        // Apply styles
                        if (p.getStyle() == "ReflectionPara" || p.getStyle() == "KeyPointPara") {
                            r.setFontFamily(HAND_FONT)
                            r.setItalic(true)
                        } else if (p.getStyle() == "Scripture") {
                            r.setFontFamily(SERIF)
                            r.setFontSize(12)
                        } else {
                            r.setFontFamily(SANS)
                            r.setFontSize(12)
                        }

                        if (isBold) r.setBold(true)
                        if (isItalic) r.setItalic(true)
                        
                        if (kind == "tick-num") {
                            r.setFontSize(9)
                            r.setSubscript(VerticalAlign.SUPERSCRIPT)
                            if (isNotes) r.setColor(rgbHex(NOTES_TEXT))
                        } else if (kind == "tick-text") {
                            r.setColor(rgbHex(TICK_BLUE))
                        } else {
                            if (isNotes) r.setColor(rgbHex(NOTES_TEXT))
                        }

                        if (i < parts.size() - 1) {
                            r.addBreak(BreakType.TEXT_WRAPPING)
                        }
                    }
                }
            }
        }
    }

    // Helper: lazily remove the default empty paragraph in a cell only when we add the first real paragraph
    XWPFParagraph addCellParagraphLazy(XWPFTableCell cell, Map state) {
        if (!state.removed) {
            if (cell.getParagraphs().size() > 0 && cell.getParagraphs()[0].getText().trim().isEmpty()) {
                cell.removeParagraph(0)
            }
            state.removed = true
        }
        return cell.addParagraph()
    }

    void addNotesBlock(XWPFDocument doc, List items, boolean isRecap) {
        def cell = makeBlockTable(doc, NOTES_BG, NOTES_BORDER)[1]
        def state = [removed: false]  // for lazy removal
        
        def h4style = isRecap ? "H4Recap" : "H4Notes"
        items?.each { item ->
            def kind = item?.type ?: ""
            def content = (item?.content ?: "") as String
            
            content.split('\n', -1).each { line ->
                 if (line == null || line.isEmpty()) return
                 
                 if (line.trim().startsWith("###")) {
                     def p = addCellParagraphLazy(cell, state)
                     p.setStyle(h4style)
                     def r = p.createRun()
                     r.setText(line.trim().substring(4).replaceAll(/\*\*(.*?)\*\*/, '$1'))
                     r.setBold(true)
                     r.setFontFamily(SANS)
                     r.setFontSize(12)
                     r.setColor(isRecap ? rgbHex(TICK_BLUE) : rgbHex(NOTES_TEXT))
                 } else {
                    def p = addCellParagraphLazy(cell, state)
                    if (kind == "reflection") {
                        p.setStyle("ReflectionPara")
                    } else if (kind == "keyPoint") {
                        p.setStyle("KeyPointPara")
                    } else if (kind == "definition") {
                        p.setStyle("DefinitionPara")
                    } else if (kind == "cue") {
                        p.setStyle("CuePara")
                    } else {
                        p.setStyle("NotesPara")
                    }
                    
                    addInlineToParagraph(p, line, true)

                    if (kind == "reflection" || kind == "keyPoint") {
                        setParagraphShading(p, BAND_BG)
                        p.getRuns().each { it.setColor(rgbHex(BAND_TEXT)) }
                    } else if (kind == "definition") {
                        setParagraphShading(p, DEFINITION_BG)
                    } else if (kind == "cue") {
                        setParagraphBorderAll(p, "FFFFFF", 6, 4)
                    }
                 }
            }
        }
        // If nothing was added, the default empty paragraph still remains → valid DOCX
    }
    
    void embedSvg(XWPFDocument doc, String svgUrl) {
        if (!haveBatik || !svgUrl) return
        println "Embedding SVG from: $svgUrl"
        try {
            byte[] svgBytes = new URL(svgUrl).bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(svgBytes))
            TranscoderOutput output = new TranscoderOutput(baos)
            PNGTranscoder transcoder = new PNGTranscoder()
            
            transcoder.transcode(input, output)
            byte[] pngBytes = baos.toByteArray()

            def img = ImageIO.read(new ByteArrayInputStream(pngBytes))
            double aspect = (img.width > 0) ? (img.height / (double)img.width) : 0.75
            double pageContentWidthCm = 17.0
            
            int widthEmu = (int)(pageContentWidthCm * Units.EMU_PER_CENTIMETER)
            int heightEmu = (int)(widthEmu * aspect)

            def p = doc.createParagraph()
            p.setAlignment(ParagraphAlignment.CENTER)
            def r = p.createRun()
            r.addPicture(new ByteArrayInputStream(pngBytes), PICTURE_DOC.PICTURE_TYPE_PNG, "image.png", widthEmu, heightEmu)
        } catch (Exception e) {
            System.err.println "Error embedding SVG: ${e.message}"
        }
    }

    void buildDocx(Map data, Map strings, File outFile, String langCode) {
        def doc = new XWPFDocument()
        doc.setTrackRevisions(true)
        ensureSectPr(doc)
        setThemeFonts(doc, langCode)
        
        def bundle = data?.bundle ?: [:]
        
        if (bundle.imageUrl) {
            embedSvg(doc, bundle.imageUrl as String)
        }

        // Eyebrow line with safe fallback template
        def eyebrowTpl = (strings?.episodeEpisodeId ?: "Episode {episodeId}") as String
        def eyebrowP = doc.createParagraph()
        setPara(eyebrowP, ParagraphAlignment.CENTER, null, 4)
        def er = eyebrowP.createRun()
        er.setText(eyebrowTpl.replace("{episodeId}", String.valueOf(data?.number)))
        er.setBold(true)
        er.setFontSize(11)
        er.setColor(rgbHex(NOTES_BG))

        // Title (from data; safe with empty fallback)
        def titleP = doc.createParagraph()
        setPara(titleP, ParagraphAlignment.LEFT, null, 6)
        titleP.createRun().with {
            setText((bundle?.title ?: "") as String)
            setBold(true)
            setFontSize(16)
        }

        // Intro block (data-driven)
        if (bundle?.intro) {
            def introCell = makeBlockTable(doc, new Color(0xF7, 0xFA, 0xFC), new Color(0xE5, 0xE7, 0xEB))[1]
            def state = [removed: false]
            def ip = addCellParagraphLazy(introCell, state)
            ip.setStyle("NotesPara")
            addInlineToParagraph(ip, bundle.intro as String, false)
        }

        // Spreads
        bundle?.spreads?.each { spread ->
            doc.createParagraph()
            def cell = makeBlockTable(doc, Color.WHITE, new Color(0xE5, 0xE7, 0xEB))[1]
            def state = [removed: false]
            
            def scripture = spread?.scripture ?: [:]
            def stype = (spread?.type ?: "") as String
            def reference = (scripture?.reference ?: "") as String

            if (stype == "stopAndThink") {
                if (strings?.stopAndThink) {
                    def p = addCellParagraphLazy(cell, state)
                    p.setStyle("UniformTitle")
                    p.createRun().setText(strings.stopAndThink as String)
                }
                if (strings?.letsTakeAMomentToThink) {
                    def p = addCellParagraphLazy(cell, state)
                    p.createRun().setText(strings.letsTakeAMomentToThink as String)
                }
            }
            if (reference && stype != "stopAndThink") {
                 def p = addCellParagraphLazy(cell, state)
                 p.setStyle("UniformTitle")
                 def r = p.createRun(); r.setText(reference); r.setColor(rgbHex(NOTES_BG))
            }
            if (spread?.callout) {
                 def p = addCellParagraphLazy(cell, state)
                 p.setStyle("CalloutPara")
                 addInlineToParagraph(p, spread.callout as String, false)
                 setParagraphBorderAll(p, "000000", 8, 4)
            }
            if (scripture?.verse) {
                def scriptureP = addCellParagraphLazy(cell, state)
                scriptureP.setStyle("Scripture")
                addInlineToParagraph(scriptureP, scripture.verse as String, false)
            }
            if (spread?.subtitle) {
                def sp = addCellParagraphLazy(cell, state)
                sp.setStyle("Subtitle")
                def r = sp.createRun(); r.setText(spread.subtitle as String); r.setColor(rgbHex(ACCENT))
            }
            if (spread?.notes) {
                addNotesBlock(doc, spread.notes as List, false)
            }
        }

        // Conclusions / Summary
        if (bundle?.conclusions || bundle?.summaryIntro || strings?.summary) {
            doc.createParagraph()
            def cell = makeBlockTable(doc, NOTES_BG, NOTES_BORDER)[1]
            def state = [removed: false]
            if (strings?.summary) {
                def p = addCellParagraphLazy(cell, state)
                p.setStyle("UniformTitle")
                def r = p.createRun(); r.setText(strings.summary as String); r.setColor(rgbHex(NOTES_TEXT))
            }
            if (bundle?.summaryIntro) {
                def p = addCellParagraphLazy(cell, state)
                addInlineToParagraph(p, bundle.summaryIntro as String, true)
                p.getRuns().each { it.setBold(true) }
            }
            bundle?.conclusions?.each { c ->
                if (c?.statement) {
                    def p = addCellParagraphLazy(cell, state)
                    addInlineToParagraph(p, c.statement as String, true)
                }
                if (c?.excerpt) {
                    def p = addCellParagraphLazy(cell, state)
                    p.setStyle("Scripture")
                    addInlineToParagraph(p, c.excerpt as String, false)
                    setParagraphShading(p, new Color(0xF5, 0xF5, 0xF5))
                    setParagraphBorderAll(p, "E5E7EB", 6, 8)
                }
            }
            // If nothing added, the default paragraph remains and the doc stays valid
        }
        
        // Reflection band
        if (bundle?.reflection) {
            doc.createParagraph()
            def cell = makeBlockTable(doc, BAND_BG, BAND_BG)[1]
            def state = [removed: false]
            if (strings?.toThinkAbout) {
                def p = addCellParagraphLazy(cell, state)
                p.setStyle("UniformTitle")
                def r = p.createRun(); r.setText(strings.toThinkAbout as String); r.setBold(true); r.setFontFamily(SANS); r.setColor(rgbHex(BAND_TEXT))
            }
            def p = addCellParagraphLazy(cell, state)
            p.setStyle("ReflectionPara")
            addInlineToParagraph(p, bundle.reflection as String, true)
            p.getRuns().each { it.setColor(rgbHex(BAND_TEXT)) }
        }

        // Passage reread
        if (bundle?.passage && ((bundle.passage.reference ?: "") || (bundle.passage.verse ?: ""))) {
            doc.createParagraph()
            def cell = makeBlockTable(doc, Color.WHITE, new Color(0xE5,0xE7,0xEB))[1]
            def state = [removed: false]
            if (strings?.readAgain) {
                 def p = addCellParagraphLazy(cell, state); p.setStyle("UniformTitle")
                 def r = p.createRun(); r.setText(strings.readAgain as String); r.setColor(rgbHex(NOTES_BG))
            }
            if (bundle.passage.reference) {
                 def p = addCellParagraphLazy(cell, state)
                 def r = p.createRun(); r.setText(bundle.passage.reference as String)
                 r.setBold(true); r.setColor(rgbHex(ACCENT))
            }
            if (strings?.takeAMomentToReRead) {
                def p = addCellParagraphLazy(cell, state)
                p.createRun().setText(strings.takeAMomentToReRead as String)
            }
            if (bundle.passage.verse) {
                 def p = addCellParagraphLazy(cell, state); p.setStyle("Scripture")
                 addInlineToParagraph(p, bundle.passage.verse as String, false)
            }
        }

        // Recap/To think about (notes block)
        if (bundle?.recap || bundle?.recapToThinkAbout) {
             doc.createParagraph()
             def notes = []
             if (bundle.recap) notes.add([content: bundle.recap])
             if (bundle.recapToThinkAbout) notes.add([type: 'reflection', content: bundle.recapToThinkAbout])
             addNotesBlock(doc, notes, true)
        }
        
        // Next up band
        if (bundle?.nextUp) {
            doc.createParagraph()
            def cell = makeBlockTable(doc, BAND_BG, BAND_BG)[1]
            def state = [removed: false]
            if (strings?.nextUp) {
                 def p = addCellParagraphLazy(cell, state); p.setStyle("UniformTitle")
                 def r = p.createRun(); r.setText(strings.nextUp as String); r.setColor(rgbHex(BAND_TEXT))
            }
            if (strings?.episodeComplete) {
                def p = addCellParagraphLazy(cell, state)
                p.setStyle("ReflectionPara")
                p.setAlignment(ParagraphAlignment.CENTER)
                addInlineToParagraph(p, strings.episodeComplete as String, true)
                p.getRuns().each { it.setColor(rgbHex(BAND_TEXT)) }
            }
            def p = addCellParagraphLazy(cell, state)
            p.setStyle("ReflectionPara")
            addInlineToParagraph(p, bundle.nextUp as String, true)
            p.getRuns().each { it.setColor(rgbHex(BAND_TEXT)) } // Typo guarded below
        }
        
        outFile.withOutputStream { os -> doc.write(os) }
    }
}
