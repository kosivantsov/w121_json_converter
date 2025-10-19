import groovy.json.JsonSlurper
import org.apache.commons.io.FilenameUtils
import org.apache.commons.text.StringEscapeUtils
import java.nio.file.Paths
import java.util.regex.Pattern

class JsonToHtmlScript {

    String inputPath
    String outputPath
    String stringsPath
    boolean darkMode

    void run() {
        def data
        try {
            data = new JsonSlurper().parse(new File(inputPath))
        } catch (Exception e) {
            // Errors will be caught and logged by the Java GUI
            throw new RuntimeException("Error loading input JSON: ${e.message}", e)
        }

        def strings = loadStrings(stringsPath)
        String html = buildHtml(data, strings, darkMode)

        try {
            new File(outputPath).text = html
        } catch (Exception e) {
            throw new RuntimeException("Error writing output HTML: ${e.message}", e)
        }
    }

    private def loadStrings(String path) {
        if (path == null || path.trim().isEmpty()) return [:]
        try {
            def file = new File(path)
            return file.exists() ? new JsonSlurper().parse(file) : [:]
        } catch(Exception e) {
            println("Warning: Could not load strings file at ${path}. Proceeding without it.")
            return [:]
        }
    }

    private String determineOutputPath(String input, String output = null) {
        if (output) return output
        def baseName = FilenameUtils.getBaseName(input)
        def parentDir = new File(input).parent
        return new File(parentDir, baseName + ".html").absolutePath
    }
    
    // --- All other helper methods from your script ---
    // (applyInlineMarkers, renderSpread, buildHtml, etc.)
    // They are identical to your source and are included below for completeness.

    boolean isPureVerseNumber(String s) {
        return s?.trim() ==~ /\d{1,3}/
    }

    String unescapeSpecificHtml(String html) {
        if (!html) return ""
        return html.replaceAll("&lt;h4(.*?)&gt;", "<h4>")
                   .replaceAll("&lt;/h4&gt;", "</h4>")
                   .replaceAll("&amp;", "&")
                   .replaceAll('&lt;em&gt;', '<em>').replaceAll('&lt;/em&gt;', '</em>')
                   .replaceAll('&lt;strong&gt;', '<strong>').replaceAll('&lt;/strong&gt;', '</strong>')
                   .replaceAll(/&lt;(p|span)\s+(class\=)&quot;(.*?)&quot;&gt;/, '<$1 $2\"$3\">')
                   .replaceAll(/&lt;\/(p|span)&gt;/, '</$1>')
    }

    String formatTicksPreservingText(String text) {
        if (text == null) return ""
        def tickPattern = ~/`([^`]+)`/
        def matcher = tickPattern.matcher(text)
        def last = 0
        def parts = []
        while (matcher.find()) {
            def before = text.substring(last, matcher.start())
            parts << StringEscapeUtils.escapeHtml4(before)
            def inner = matcher.group(1)
            if (isPureVerseNumber(inner)) {
                parts << "<sup>" + StringEscapeUtils.escapeHtml4(inner) + "</sup>"
            } else {
                parts << "<span class=\"tick-strong\">" + StringEscapeUtils.escapeHtml4(inner) + "</span>"
            }
            last = matcher.end()
        }
        parts << StringEscapeUtils.escapeHtml4(text.substring(last))
        return parts.join("")
    }

    String applyInlineMarkers(String text) {
        if (text == null) return ""
        return text.readLines().collect { line ->
            String currentLine = line

            // Rule 1: Doubled asterisks (**text**) are always processed first and converted to bold.
            currentLine = currentLine.replaceAll(/\*\*(.+?)\*\*/) { _, inner -> "<strong>${inner}</strong>" }

            def trimmedLine = currentLine.trim()
            def startsWithHyphen = trimmedLine.startsWith('-')
            def singleAsteriskCount = currentLine.count('*')
            boolean hasOddAsterisks = (singleAsteriskCount > 0 && singleAsteriskCount % 2 != 0)

            if (startsWithHyphen) {
                // Rule 3: If a line starts with a hyphen, it becomes a bullet.
                def lineContent = trimmedLine.substring(1).trim()
                lineContent = lineContent.replaceAll(/\*(.+?)\*/) { _, inner -> "<em>${inner}</em>" }
                lineContent = lineContent.replace('*', '•') // Clean up any stray single asterisks
                return '<p class="bullet"><span class="b-mark">•</span><span class="b-text">' + lineContent + '</span></p>'

            } else if (hasOddAsterisks) {
                // Rule 2: If a line has an odd number of single asterisks, the first becomes a bullet.
                def lineContent = currentLine.replaceFirst('\\*', '').trim()
                def contentBuilder = new StringBuilder()
                boolean inItalics = false
                lineContent.each { c ->
                    if (c == '*') {
                        contentBuilder.append(inItalics ? '</em>' : '<em>')
                        inItalics = !inItalics
                    } else {
                        contentBuilder.append(c)
                    }
                }
                if (inItalics) contentBuilder.append('</em>') // Close any open italics at the end
                return '<p class="bullet"><span class="b-mark">•</span><span class="b-text">' + contentBuilder.toString() + '</span></p>'

            } else {
                // Default case: Line has an even number of asterisks. They are treated as italic pairs.
                currentLine = currentLine.replaceAll(/\*(.+?)\*/) { _, inner -> "<em>${inner}</em>" }
                currentLine = currentLine.replace('*', '•') // Convert any leftover single asterisks
                return currentLine
            }
        }.join('\n')
    }

    String mdHeadingMinimal(String text, String h4Class = "h4-notes") {
        if (text == null) return ""
        text.readLines().collect { line ->
            if (line ==~ /^\s*#{2,3}\s*.*/) {
                return "<h4 class=\"${h4Class}\">" + StringEscapeUtils.escapeHtml4(line.substring(4)) + "</h4>"
            } else {
                return line
            }
        }.join("\n")
    }

    String tightenHeadings(String html) {
        if (html == null) return ""
        Pattern pattern = Pattern.compile("(</h4>)(?:<br>\\s*){2,}", Pattern.CASE_INSENSITIVE)
        def matcher = pattern.matcher(html)
        StringBuffer sb = new StringBuffer()
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + "<br>")
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    String titleBarHtml(String text) {
        if (!text) return ""
        return "<div class=\"block-title uniform-title\">" + StringEscapeUtils.escapeHtml4(text) + "</div>"
    }

    String renderCallout(String calloutText) {
        if (!calloutText) return ""
        def calloutMarked = applyInlineMarkers(calloutText)
        def calloutHtml = formatTicksPreservingText(calloutMarked)
        calloutHtml = unescapeSpecificHtml(calloutHtml)
        return """
<div class="callout-item">
  <div class="callout-wrap">
    <svg class="callout-outline" viewBox="0 0 600 330" preserveAspectRatio="none" aria-hidden="true" focusable="false">
      <path class="outline" d="M93,33 C181,13 210,15 555,30 C575,32 585,45 588,65 C595,140 595,210 585,285 C582,305 568,315 548,318 C420,325 280,325 60,312 C42,311 30,300 28,282 C22,210 32,76 31,42 C30,8 91,16 118,6"/>
    </svg>
    <span class="callout-text-inner">${calloutHtml}</span>
  </div>
</div>
""".stripIndent().trim()
    }

    String renderNotesGroup(List notes, Map strings) {
        if (!notes) return ""
        def itemsHtml = []
        def satTitle = strings?.stopAndThink ?: ""
        def satSub = strings?.letsTakeAMomentToThink ?: ""
        notes.each { note ->
            def kind = (note?.type ?: "").trim()
            def raw = note?.content ?: ""
            def rawMarked = applyInlineMarkers(raw)
            def processedWithHeadings = mdHeadingMinimal(rawMarked, "h4-notes")
            def processedWithTicks = formatTicksPreservingText(processedWithHeadings)
            
            def processed = processedWithTicks
            processed = processed.replaceAll(/(?<!<\/h4>)\n+/, "<br>")
            processed = tightenHeadings(processed)
            processed = unescapeSpecificHtml(processed)
            
            def cls = "note-item"
            switch(kind) {
                case "definition":
                    cls += " definition"
                    break
                case "keyPoint":
                    itemsHtml << """
<div class="note-item keypoint">
  <div class="keypoint-highlight">${processed}</div>
</div>""".stripIndent().trim()
                    return
                case "cue":
                    cls += " cue"
                    break
                case "comment":
                    cls += " comment"
                    break
                case "reflection":
                    cls += " reflection"
                    break
                case "stopAndThink":
                    cls += " stopthink"
                    break
            }
            if (cls.contains(" cue")) {
                itemsHtml << """
<div class="note-item cue">
  <div class="cue-wrap">
    <svg class="cue-outline" viewBox="0 0 600 330" preserveAspectRatio="none" aria-hidden="true" focusable="false">
      <path class="outline" d="M93,33 C181,13 210,15 555,30 C575,32 585,45 588,65 C595,140 595,210 585,285 C582,305 568,315 548,318 C420,325 280,325 60,312 C42,311 30,300 28,282 C22,210 32,76 31,42 C30,8 91,16 118,6"/>
    </svg>
    <span class="cue-text-inner">${processed}</span>
  </div>
</div>""".stripIndent().trim()
            } else if (cls.contains(" stopthink")) {
                def titleLine = titleBarHtml(satTitle)
                def subLine = satSub ? "<div class=\"sat-sub\">" + StringEscapeUtils.escapeHtml4(satSub) + "</div>" : ""
                itemsHtml << """
<div class="${cls}">
  ${titleLine}
  ${subLine}
  <div class="stopthink-body">${processed}</div>
</div>""".stripIndent().trim()
            } else {
                itemsHtml << """<div class="${cls}">${processed}</div>"""
            }
        }
        def inner = itemsHtml.join("\n")
        return """
<div class="notes-block">
  ${inner}
</div>""".stripIndent().trim()
    }

    String renderSpread(Map spread, Map strings) {
        def spreadType = (spread?.type ?: "").trim()
        def subtitle = spread?.subtitle ?: ""
        def callout = spread?.callout ?: ""
        def scripture = spread?.scripture ?: [:]
        def verseText = scripture?.verse ?: ""
        def reference = scripture?.reference ?: ""

        def headerBits = []
        if (reference.trim()) {
            headerBits << titleBarHtml(reference)
        }
        if (spreadType == "stopAndThink") {
            if (strings?.stopAndThink) headerBits.add(0, titleBarHtml(strings.stopAndThink))
            if (strings?.letsTakeAMomentToThink) headerBits.add(1, "<div class=\"sat-sub\">" + StringEscapeUtils.escapeHtml4(strings.letsTakeAMomentToThink) + "</div>")
        }
        def headerHtml = headerBits.findAll { it } .join("\n  ")

        def calloutHtml = callout ? renderCallout(callout) : ""

        def scriptureHtml = ""
        if (verseText.trim() || reference.trim()) {
            def verseMarked = applyInlineMarkers(verseText)
            def verseHtml = formatTicksPreservingText(verseMarked).replace("\n", "<br>")
            verseHtml = unescapeSpecificHtml(verseHtml)
            scriptureHtml = """
  <div class="scripture">
    <div class="scripture-text">${verseHtml}</div>
  </div>""".stripIndent().replaceAll(/\s+$/, "")
        }

        def subtitleHtml = subtitle ? "<h3 class=\"subtitle\">" + StringEscapeUtils.escapeHtml4(subtitle) + "</h3>" : ""

        def notesHtml = renderNotesGroup(spread?.notes ?: [], strings)

        return """
<section class="spread pill">
  ${headerHtml}
  ${calloutHtml}
${scriptureHtml}
  ${subtitleHtml}
  ${notesHtml}
</section>""".stripIndent().trim()
    }

    String renderConclusionsBlock(Map strings, String summaryIntro, List conclusions) {
        if (!summaryIntro && (conclusions == null || conclusions.isEmpty())) return ""
        def titleHtml = titleBarHtml(strings?.summary)
        def parts = []
        if (summaryIntro) {
            def siMarked = applyInlineMarkers(summaryIntro)
            def siHtml = formatTicksPreservingText(siMarked).replace("\n", "<br>")
            siHtml = unescapeSpecificHtml(siHtml)
            parts << """<div class="conclusions-intro">${siHtml}</div>"""
        }
        if (conclusions) {
            conclusions.each { c ->
                def excerptRaw = c?.excerpt ?: ""
                def excerptMarked = applyInlineMarkers(excerptRaw)
                def excerptHtml = formatTicksPreservingText(excerptMarked).replace("\n", "<br>")
                excerptHtml = unescapeSpecificHtml(excerptHtml)
                def statementHtml = StringEscapeUtils.escapeHtml4(c?.statement ?: "")
                parts << """
<div class="conclusion-item">
  <div class="statement">${statementHtml}</div>
  <div class="excerpt-pill serif">${excerptHtml}</div>
</div>""".stripIndent().trim()
            }
        }
        def inner = parts.join("\n")
        return """
<section class="conclusions-block pill">
  <div class="conclusions-box">
    ${titleHtml}
    ${inner}
  </div>
</section>""".stripIndent().trim()
    }

    String renderPassageBlock(Map strings, Map passage) {
        if (!passage) return ""
        def ref = passage?.reference ?: ""
        def verse = passage?.verse ?: ""
        if (!ref.trim() && !verse.trim()) return ""
        def titleHtml = titleBarHtml(strings?.readAgain)
        def refHtml = StringEscapeUtils.escapeHtml4(ref)
        def mid = strings?.takeAMomentToReRead ?: ""
        def readAgainHtml = mid ? "<div class=\"passage-read-again\">" + StringEscapeUtils.escapeHtml4(mid) + "</div>" : ""
        def verseMarked = applyInlineMarkers(verse)
        def verseHtml = formatTicksPreservingText(verseMarked).replace("\n", "<br>")
        verseHtml = unescapeSpecificHtml(verseHtml)
        return """
<section class="passage-block pill">
  ${titleHtml}
  <h2 class="passage-ref">${refHtml}</h2>
  ${readAgainHtml}
  <div class="passage-verse">${verseHtml}</div>
</section>""".stripIndent().trim()
    }

    String renderRecapBlock(Map strings, String recap, String recapToThinkAbout) {
        if (!recap?.trim() && !recapToThinkAbout?.trim()) return ""

        def recapProcessed = ""
        if (recap?.trim()) {
            def temp = applyInlineMarkers(recap)
            temp = mdHeadingMinimal(temp, "h4-recap")
            temp = formatTicksPreservingText(temp)

            temp = temp.replaceAll(/(\r\n|\n){2,}/, '[[PARA_BREAK]]')
            temp = temp.replaceAll(/(\r\n|\n)/, '<br>')
            temp = temp.replaceAll(/\[\[PARA_BREAK\]\]/, '<p class="para-spacer"></p>')

            recapProcessed = tightenHeadings(temp)
            recapProcessed = unescapeSpecificHtml(recapProcessed)
        }

        def toThinkAboutHtml = ""
        if (recapToThinkAbout?.trim()) {
            def thinkAboutMarked = applyInlineMarkers(recapToThinkAbout)
            def thinkAboutText = formatTicksPreservingText(thinkAboutMarked).replace("\n", "<br>")
            thinkAboutText = unescapeSpecificHtml(thinkAboutText)
            toThinkAboutHtml = """
<div class="recap-think-about">
    ${thinkAboutText}
</div>
            """.stripIndent()
        }

        def mainTitleHtml = titleBarHtml(strings?.whatWeHaveSeenSoFar)

        return """
<section class="recap-block pill">
  <div class="notes-block">
    ${mainTitleHtml}
    ${recapProcessed}
    ${toThinkAboutHtml}
  </div>
</section>""".stripIndent().trim()
    }

    String buildHtml(Map data, Map strings, boolean dark) {
        def topNumber = data.number
        def bundle = data.bundle ?: [:]
        def title = bundle.title ?: ""
        def intro = bundle.intro ?: ""
        def nextUp = bundle.nextUp ?: ""
        def summaryIntro = bundle.summaryIntro ?: ""
        def imageUrl = bundle.imageUrl ?: ""
        def reflection = bundle.reflection ?: ""
        def conclusions = bundle.conclusions ?: []
        def passage = bundle.passage ?: [:]
        def spreads = bundle.spreads ?: []
        def recap = bundle.recap ?: ""
        def recapToThinkAbout = bundle.recapToThinkAbout ?: ""

        def topImageHtml = imageUrl ? """<div class="top-image pill"><img src="${StringEscapeUtils.escapeHtml4(imageUrl)}" alt=""/></div>""" : ""

        def eyebrowTpl = strings.episodeEpisodeId
        def eyebrowText = eyebrowTpl ? eyebrowTpl.replace("{episodeId}", topNumber?.toString() ?: "") : "Episode ${topNumber ?: ''}"

        def introMarked = applyInlineMarkers(intro)
        def introHtml = formatTicksPreservingText(introMarked).replace("\n", "<br>")
        introHtml = unescapeSpecificHtml(introHtml)

        def spreadsHtml = spreads.collect { s -> renderSpread(s, strings) }.join("\n")

        def reflectionHtml = ""
        if (reflection) {
            def reflMarked = applyInlineMarkers(reflection)
            def reflText = formatTicksPreservingText(reflMarked).replace("\n", "<br>")
            reflText = unescapeSpecificHtml(reflText)
            def titleHtml = titleBarHtml(strings.toThinkAbout)
            reflectionHtml = """
<section class="band-block pill">
  <div class="band-inner">
    ${titleHtml}
    ${reflText}
  </div>
</section>""".stripIndent().trim()
        }

        def passageHtml = renderPassageBlock(strings, passage)
        def conclusionsHtml = renderConclusionsBlock(strings, summaryIntro, conclusions)
        def recapHtml = renderRecapBlock(strings, recap, recapToThinkAbout)

        def nextUpHtml = ""
        if (nextUp) {
            def nuMarked = applyInlineMarkers(nextUp)
            def nuText = formatTicksPreservingText(nuMarked).replace("\n", "<br>")
            nuText = unescapeSpecificHtml(nuText)
            def prefixText = strings.episodeComplete ?: ""
            def prefixHtml = prefixText ? "<div class=\"bigger2\">" + StringEscapeUtils.escapeHtml4(prefixText) + "</div>" : ""
            def titleHtml = titleBarHtml(strings.nextUp)
            nextUpHtml = """
<footer class="next-up pill">
  <div class="band-inner">
    ${titleHtml}
    ${prefixHtml}
    ${nuText}
  </div>
</footer>""".stripIndent().trim()
        }

        def htmlTitle = topNumber ? "${topNumber} — ${title}" : title
        def dataTheme = dark ? ' data-theme="dark"' : ""
        def thumbHex = dark ? "#555" : "#aaa"
        def trackHex = dark ? "#222" : "#eee"

        return """<!doctype html>
<html lang="en"${dataTheme}>
<head>
  <meta charset="utf-8">
  <title>${htmlTitle}</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <!-- Fonts -->
  <link href="https://fonts.googleapis.com/css2?family=Playpen+Sans:ital,wght@0,400;0,600;1,400;1,600&family=Caveat:wght@400;600&family=Noto+Serif:ital,wght@0,400;0,600;1,400;1,600&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #fff;
      --fg: #111;
      --muted: #555;
      --accent: #0d47a1;
      --ref: #6b7280;
      --rule: #e5e7eb;
      --notes-bg: #b0c7ff;
      --title-text: #507994;
      --notes-border: #8fb2ff;
      --notes-text: #ffffff; /* CORRECTED: Was #000000, now white for light theme */
      --definition-bg: #a1bcff;
      --band-bg: #0b3d91;
      --band-text: #ffffff;
      --cue-ring: #fff;
      --cue-text: #fff;
      --callout-bg: #fff;
      --callout-text: #000;
      --callout-ring: #000;
      --tick-blue: #0b3d91;
      --hand-font: "Playpen Sans", "Caveat", system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif;
      --sans: system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif;
      --serif: "Noto Serif", Georgia, "Times New Roman", Times, serif;
      --sup-scale: 0.78;
      --pill-radius: 12px;
      --pill-border: 1px solid rgba(0,0,0,0.12);
    }
    :root[data-theme="dark"] {
      --bg: #0f141a;
      --fg: #e6e9ee;
      --muted: #a3a9b2;
      --accent: #6aa7ff;
      --ref: #a3a9b2;
      --rule: #223042;
      --notes-bg: #2a3a57;
      --title-text: #448bab;
      --notes-border: #3a4f75;
      --notes-text: #e9f2ff;
      --definition-bg: #324567;
      --band-bg: #1e2e4a;
      --band-text: #e9f2ff;
      --cue-ring: #e9f2ff;
      --cue-text: #e9f2ff;
      --callout-bg: #2a3a57;
      --callout-text: #e9f2ff;
      --callout-ring: #e9f2ff;
      --tick-blue: #9cc3ff;
      --pill-border: 1px solid rgba(233,242,255,0.18);
    }
    html, body {
      background: var(--bg);
      color: var(--fg);
      margin: 0;
      padding: 0;
      font: 16px/1.6 var(--sans);
    }
    .pill { border-radius: var(--pill-radius); border: var(--pill-border); }
    .top-image { display: block; text-align: center; padding-top: 12px; }
    .top-image img { display: block; width: 30%; max-width: 30%; height: auto; margin: 0 auto; }
    .page { max-width: 880px; margin: 0 auto; padding: 12px 16px 64px; }
    header.page-header { border-bottom: 1px solid var(--rule); margin-bottom: 20px; padding-bottom: 12px; }
    .eyebrow { text-transform: uppercase; font-weight: 700; letter-spacing: .06em; color: var(--notes-bg); font-size: 12px; text-align: center; }
    h1.title { margin: 6px 0 6px; line-height: 1.2; font-size: 28px; }
    .reference { color: var(--ref); font-size: 14px; margin-bottom: 8px; }
    section.intro { border-radius: var(--pill-radius); border: var(--pill-border); padding: 12px 14px; margin: 16px 0; background: color-mix(in oklab, var(--bg), white 6%); }
    section.spread { margin: 28px 0; padding: 12px; border-radius: var(--pill-radius); border: var(--pill-border); }
    h3.subtitle { font-size: 18px; margin: 8px 0 8px; color: var(--accent); }
    .uniform-title {
      background: transparent;
      color: var(--title-text);
      text-align: center;
      font-weight: 700;
      font-style: normal;
      font-family: var(--sans);
      padding: 8px 10px;
      margin-bottom: 8px;
      border-radius: 8px;
    }
    .scripture, .passage-verse, .scripture-text, .excerpt-pill { font-family: var(--serif); }
    .scripture { background: var(--bg); padding: 12px 14px; margin: 8px 0 8px; border-radius: 8px; }
    .scripture-text { white-space: normal; }
    .notes-block {
      background: var(--notes-bg);
      border: 1px solid var(--notes-border);
      color: var(--notes-text);
      border-radius: 12px;
      padding: 14px 16px;
      margin-top: 10px;
    }
    .notes-block h4, .h4-notes, .h4-recap { display: block; margin-top: 1.5em; margin-bottom: -0.5em; }
    .h4-notes { color: var(--notes-text); }
    .h4-recap { color: var(--tick-blue); }
    .note-item { margin: 8px 0; font-family: var(--sans); }
    .note-item.definition { background: var(--definition-bg); color: var(--notes-text); border-radius: 10px; padding: 10px 12px; }
    .note-item.reflection {
      background: var(--band-bg);
      color: var(--band-text);
      font-family: var(--hand-font);
      font-style: italic;
      border-radius: 10px;
      padding: 10px 12px;
    }
    .recap-block .notes-block .para-spacer { margin-top: 0.8em; }
    .recap-think-about {
      background: var(--band-bg);
      color: var(--band-text);
      font-style: italic;
      font-family: var(--hand-font);
      border-radius: 10px;
      padding: 14px 16px;
      margin-top: 20px;
    }
    .note-item.keypoint { display: flex; justify-content: left; margin: 2px 196px 2px 0; }
    .keypoint-highlight {
      position: relative;
      display: inline-block;
      color: var(--band-text);
      font-family: var(--hand-font);
      font-style: italic;
      padding: 2px 200px 2px 10px;
      z-index: 1;
      line-height: 1.8;
    }
    .keypoint-highlight::before {
      content: "";
      position: absolute;
      top: 0;
      bottom: 0;
      left: -4px;
      right: 196px;
      background-color: var(--band-bg);
      transform: skew(-15deg);
      z-index: -1;
    }
    .note-item.cue { display: flex; justify-content: center; color: var(--cue-text); margin: 18px 196px; }
    .cue-wrap { position: relative; display: inline-block; line-height: 1.45; padding: 10px 14px; }
    .cue-text-inner { position: relative; z-index: 1; color: var(--cue-text); font-family: var(--sans); }
    .cue-outline { position: absolute; left: 0; top: 0; pointer-events: none; overflow: visible; }
    .callout-item { display: flex; justify-content: center; margin: 18px 196px; }
    .callout-wrap { position: relative; display: inline-block; line-height: 1.45; padding: 10px 14px; background: transparent; border-radius: 10px; }
    .callout-text-inner { position: relative; z-index: 1; color: var(--callout-text); font-family: var(--sans); }
    .callout-outline { position: absolute; left: 0; top: 0; pointer-events: none; overflow: visible; }
    .callout-outline .outline { fill: none; stroke: var(--callout-ring); stroke-width: 10; stroke-linecap: round; stroke-linejoin: round; }
    .stopthink .sat-sub { text-align: center; background: var(--bg); color: #000; font-family: var(--serif); padding: 6px 8px; border-radius: 8px; }
    .tick-strong { color: var(--tick-blue); font-weight: 500; }
    .conclusions-block { margin: 24px 0; }
    .conclusions-box {
      background: var(--notes-bg);
      border: 1px solid var(--notes-border);
      color: var(--notes-text);
      border-radius: 12px;
      padding: 14px 16px;
    }
    .conclusions-intro { font-weight: 700; margin-bottom: 24px; font-family: var(--sans); }
    .conclusion-item { margin: 12px 0; }
    .conclusion-item .statement { font-weight: 400; }
    .excerpt-pill {
      display: inline-block;
      background: #ffffff;
      color: #000000;
      border-radius: 10px;
      padding: 6px 10px;
      margin-top: 6px;
      font-family: var(--serif), serif;
    }
    .band-block, footer.next-up { border-radius: var(--pill-radius); background: var(--band-bg); color: var(--band-text); font-style: italic; font-family: var(--hand-font); }
    .band-block .band-inner, footer.next-up .band-inner { padding: 14px 16px; }
    .bigger2 { font-size: 18px; font-weight: 700; }
    .passage-block { margin: 20px 0 8px; border-radius: var(--pill-radius); border: var(--pill-border); padding: 12px 14px; }
    .passage-ref { font-size: 22px; margin: 0 0 6px 0; color: var(--accent); font-family: var(--sans); }
    .passage-read-again { color: var(--fg); font-family: var(--sans); margin-bottom: 6px; }
    .passage-verse { white-space: normal; font-family: var(--serif); }
    sup { line-height: 0; vertical-align: super; font-size: calc(1em * var(--sup-scale)); }
    .bullet{ margin: 0.3rem 0; line-height: 1.6; display: flex; align-items: flex-start; }
    .b-mark{ flex: 0 0 1.2rem; text-align: right; margin-right: 0.1rem; }
    .b-text{ flex: 1 1 auto; min-width: 0; padding-left: 0.6rem; }
    .container { overflow: auto; scrollbar-width: thin; -ms-overflow-style: auto; scrollbar-gutter: stable; }
    ::-webkit-scrollbar { width: 12px; height: 12px; }
    ::-webkit-scrollbar-track { background: ${trackHex}; }
    ::-webkit-scrollbar-thumb { background-color: ${thumbHex}; border-radius: 6px; border: 2px solid ${trackHex}; }
    #reload-button { position: fixed; top: 0.75rem; right: 1rem; font-size: 1.5rem; font-weight: bold; color: inherit; text-decoration: none; opacity: 0.4; transition: opacity 0.2s ease-in-out; z-index: 1000; }
    #reload-button:hover { opacity: 1; }
  </style>
</head>
<script>
(function(){
  const BASE_W = 600, BASE_H = 330;
  function parsePathD(d) {
    const tokens = d.trim().replace(/([A-Za-z])/g, ' \$1 ').trim().split(/[\s,]+/);
    const cmds = [];
    let i = 0;
    while (i < tokens.length) {
      const t = tokens[i++], u = t.toUpperCase();
      if (u === 'M' || u === 'L') {
        const x = parseFloat(tokens[i++]), y = parseFloat(tokens[i++]);
        cmds.push({cmd: u, pts: [x, y]});
      } else if (u === 'C') {
        const x1 = parseFloat(tokens[i++]), y1 = parseFloat(tokens[i++]);
        const x2 = parseFloat(tokens[i++]), y2 = parseFloat(tokens[i++]);
        const x  = parseFloat(tokens[i++]), y  = parseFloat(tokens[i++]);
        cmds.push({cmd: 'C', pts: [x1,y1,x2,y2,x,y]});
      } else if (u === 'Z') {
        cmds.push({cmd: 'Z', pts: []});
      }
    }
    return cmds;
  }
  function scaleCmds(cmds, sx, sy) {
    return cmds.map(c => {
      if (c.cmd === 'M' || c.cmd === 'L') {
        const [x,y] = c.pts;
        return {cmd: c.cmd, pts: [x*sx, y*sy]};
      } else if (c.cmd === 'C') {
        const [x1,y1,x2,y2,x,y] = c.pts;
        return {cmd: 'C', pts: [x1*sx,y1*sy,x2*sx,y2*sy,x* sx,y* sy]};
      }
      return {cmd: c.cmd, pts: []};
    });
  }
  function bboxCmds(cmds) {
    let minX=Infinity, minY=Infinity, maxX=-Infinity, maxY=-Infinity;
    for (const c of cmds) {
      for (let i=0;i<c.pts.length;i+=2) {
        const x=c.pts[i], y=c.pts[i+1];
        if (Number.isFinite(x) && Number.isFinite(y)) {
          if (x<minX) minX=x; if (y<minY) minY=y;
          if (x>maxX) maxX=x; if (y>maxY) maxY=y;
        }
      }
    }
    if (!isFinite(minX)) return {minX:0,minY:0,maxX:0,maxY:0};
    return {minX,minY,maxX,maxY};
  }
  function translateCmds(cmds, dx, dy) {
    return cmds.map(c => {
      if (c.pts.length) {
        const out = c.pts.slice();
        for (let i=0;i<out.length;i+=2) { out[i]+=dx; out[i+1]+=dy; }
        return {cmd:c.cmd, pts:out};
      }
      return {cmd:c.cmd, pts:[]};
    });
  }
  function stringifyCmds(cmds) {
    return cmds.map(c => {
      if (c.cmd === 'M' || c.cmd === 'L') {
        const [x,y] = c.pts;
        return `\${c.cmd}\${x.toFixed(2)},\${y.toFixed(2)}`;
      } else if (c.cmd === 'C') {
        const [x1,y1,x2,y2,x,y] = c.pts;
        return `C\${x1.toFixed(2)},\${y1.toFixed(2)},\${x2.toFixed(2)},\${y2.toFixed(2)},\${x.toFixed(2)},\${y.toFixed(2)}`;
      }
      return 'Z';
    }).join(' ');
  }
  function fitOutline(container, options={}) {
    const padL = options.paddingLeft   ?? 10;
    const padR = options.paddingRight  ?? 20;
    const padT = options.paddingTop    ?? 6;
    const padB = options.paddingBottom ?? 18;
    const extraW = options.extraW      ?? 20;
    const extraH = options.extraH      ?? 0;
    const textEl = container.querySelector('.cue-text-inner, .callout-text-inner');
    const svg    = container.querySelector('svg.cue-outline, svg.callout-outline');
    const path   = svg?.querySelector('path.outline');
    if (!textEl || !svg || !path) return;
    const rect = textEl.getBoundingClientRect();
    const targetW = Math.ceil(rect.width)  + padL + padR + extraW;
    const targetH = Math.ceil(rect.height) + padT + padB + extraH;
    svg.setAttribute('width',  targetW);
    svg.setAttribute('height', targetH);
    svg.setAttribute('viewBox', `0 0 \${targetW} \${targetH}`);
    svg.style.position = 'absolute';
    svg.style.left = `\${-padL}px`;
    svg.style.top  = `\${-padT}px`;
    const d0   = path.getAttribute('data-d-original') || path.getAttribute('d');
    const base = parsePathD(d0);
    const sx = targetW / BASE_W, sy = targetH / BASE_H;
    let scaled = scaleCmds(base, sx, sy);
    const bb = bboxCmds(scaled);
    const nudgeX = 18;
    const nudgeY = 5;
    scaled = translateCmds(scaled, -bb.minX + nudgeX, -bb.minY + nudgeY)
    path.setAttribute('d', stringifyCmds(scaled));
    const strokeBase = 2;
    path.style.strokeWidth = ((strokeBase * (sx + sy) / 2)).toFixed(2);
    if (container.classList.contains('callout-wrap')) {
      path.style.stroke = getComputedStyle(document.documentElement).getPropertyValue('--callout-ring').trim();
    } else {
      path.style.stroke = '#ffffff';
    }
    path.style.fill   = 'none';
    path.style.strokeLinecap = 'round';
    path.style.strokeLinejoin = 'round';
  }
  function initOutlineFitting() {
    document.querySelectorAll('.cue-wrap, .callout-wrap').forEach(el => fitOutline(el, {
      paddingLeft: 24,
      paddingRight: 24,
      paddingTop: 6,
      paddingBottom: 18,
      extraW: 20,
      extraH: 0
    }));
    window.addEventListener('resize', () => {
      document.querySelectorAll('.cue-wrap, .callout-wrap').forEach(el => fitOutline(el, {
        paddingLeft: 24,
        paddingRight: 24,
        paddingTop: 6,
        paddingBottom: 18,
        extraW: 20,
        extraH: 0
      }));
    });
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initOutlineFitting);
  } else {
    initOutlineFitting();
  }
})();
</script>
<script>
document.addEventListener('DOMContentLoaded', () => {
  try {
    const reloadButton = document.getElementById('reload-button');
    if (reloadButton) {
        reloadButton.addEventListener('click', (e) => {
          e.preventDefault();
          window.location.reload();
        });
    }
  } catch(e) {
    console.error("Reload button script failed:", e);
  }
});
</script>
<body>
  <a id="reload-button" href="#" title="Reload page">&#x21bb;</a>
  ${topImageHtml}
  <div class="page">
    <header class="page-header">
      <div class="eyebrow">${StringEscapeUtils.escapeHtml4(eyebrowText)}</div>
      <h1 class="title">${StringEscapeUtils.escapeHtml4(title)}</h1>
    </header>
    <section class="intro pill">
      <p>${introHtml}</p>
    </section>
    ${spreadsHtml}
    ${conclusionsHtml}
    ${reflectionHtml}
    ${passageHtml}
    ${recapHtml}
    ${nextUpHtml}
  </div>
</body>
</html>"""
    }
}
