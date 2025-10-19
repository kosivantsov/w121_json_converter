import com.formdev.flatlaf.FlatLaf;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonConverterGui extends JFrame implements AboutHandler {

    private final JRadioButton docxRadioButton;
    private final JRadioButton htmlRadioButton;
    private final JCheckBox processAllCheckbox;
    private final JCheckBox enableOutputCheckbox;
    private final JCheckBox darkModeCheckbox;
    private final JTextField inputField;
    private final JTextField stringsField;
    private final JTextField outputField;
    private final JButton inputBrowseButton;
    private final JButton outputBrowseButton;
    private final JButton stringsBrowseButton;
    private final JButton runButton;
    private final JComboBox<String> languageComboBox;
    private final JTextArea logArea;
    private final JComboBox<ThemeInfo> themeComboBox;
    // CORRECTED: Added the new checkbox
    private final JCheckBox saveStringsAsJsonCheckbox;

    private Class<?> docxScriptClass;
    private Class<?> htmlScriptClass;

    private static final Preferences prefs = Preferences.userNodeForPackage(JsonConverterGui.class);
    private String lastUsedDirectory;

    private static class ThemeInfo {
        String name;
        String className;

        ThemeInfo(String name, String className) {
            this.name = name;
            this.className = className;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public JsonConverterGui() {
        super(AppConfig.getAppTitle());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 650); // Increased height for new checkbox
        setLayout(new BorderLayout(10, 10));
        
        lastUsedDirectory = prefs.get("lastUsedDirectory", System.getProperty("user.home"));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        ThemeInfo[] themes = {
            new ThemeInfo("Arc Light", "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme"),
            new ThemeInfo("Arc Dark", "com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme"),
            new ThemeInfo("macOS Light", "com.formdev.flatlaf.themes.FlatMacLightLaf"),
            new ThemeInfo("macOS Dark", "com.formdev.flatlaf.themes.FlatMacDarkLaf"),
            new ThemeInfo("Carbon", "com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme"),
            new ThemeInfo("Cobalt 2", "com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme"),
            new ThemeInfo("Cyan Light", "com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme"),
            new ThemeInfo("Dracula", "com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme"),
            new ThemeInfo("Gradianto Deep Ocean", "com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme"),
            new ThemeInfo("Gruvbox Dark Hard", "com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme"),
            new ThemeInfo("Light Owl", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatLightOwlIJTheme"),
            new ThemeInfo("Monocai", "com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme"),
            new ThemeInfo("Nord", "com.formdev.flatlaf.intellijthemes.FlatNordIJTheme"),
            new ThemeInfo("One Dark", "com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme"),
            new ThemeInfo("Solarized Light", "com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme"),
            new ThemeInfo("Solarized Dark", "com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme")
        };
        themeComboBox = new JComboBox<>(themes);

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(new JLabel("Conversion Format:"), gbc);
        docxRadioButton = new JRadioButton("Convert to DOCX", true);
        htmlRadioButton = new JRadioButton("Convert to HTML");
        ButtonGroup formatGroup = new ButtonGroup();
        formatGroup.add(docxRadioButton);
        formatGroup.add(htmlRadioButton);
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioPanel.add(docxRadioButton);
        radioPanel.add(htmlRadioButton);
        gbc.gridx = 1; formPanel.add(radioPanel, gbc);
        darkModeCheckbox = new JCheckBox("Dark Mode");
        gbc.gridx = 2;
        formPanel.add(darkModeCheckbox, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Input:"), gbc);
        inputField = new JTextField(35);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2; formPanel.add(inputField, gbc);
        inputBrowseButton = new JButton("Browse...");
        gbc.gridx = 3; gbc.weightx = 0; gbc.gridwidth = 1; formPanel.add(inputBrowseButton, gbc);

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Strings File (Optional):"), gbc);
        stringsField = new JTextField(35);
        gbc.gridx = 1; gbc.gridwidth = 2; formPanel.add(stringsField, gbc);
        stringsBrowseButton = new JButton("Browse...");
        gbc.gridx = 3; gbc.gridwidth = 1; formPanel.add(stringsBrowseButton, gbc);
        
        // CORRECTED: Added the new checkbox for saving strings as JSON
        saveStringsAsJsonCheckbox = new JCheckBox("Save the strings file in JSON");
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3; formPanel.add(saveStringsAsJsonCheckbox, gbc);
        saveStringsAsJsonCheckbox.setVisible(false); // Initially hidden

        gbc.gridx = 0; gbc.gridy = 4; formPanel.add(new JLabel("Output:"), gbc);
        outputField = new JTextField(35);
        gbc.gridx = 1; gbc.gridwidth = 2; formPanel.add(outputField, gbc);
        outputBrowseButton = new JButton("Browse...");
        gbc.gridx = 3; gbc.gridwidth = 1; formPanel.add(outputBrowseButton, gbc);

        gbc.gridx = 0; gbc.gridy = 5; formPanel.add(new JLabel("Language:"), gbc);
        String[] languages = { "af-ZA", "am-ET", "ar-SA", "as-IN", "az-Latn-AZ", "be-BY", "bg-BG", "bn-IN", "bs-Latn-BA", "ca-ES", "cs-CZ", "cy-GB", "da-DK", "de-DE", "el-GR", "en-GB", "en-US", "es-ES", "es-MX", "et-EE", "eu-ES", "fa-IR", "fi-FI", "fil-PH", "fr-CA", "fr-FR", "ga-IE", "gd-GB", "gl-ES", "gu-IN", "ha-Latn-NG", "he-IL", "hi-IN", "hr-HR", "hu-HU", "hy-AM", "id-ID", "ig-NG", "is-IS", "it-IT", "ja-JP", "ka-GE", "kk-KZ", "km-KH", "kn-IN", "ko-KR", "kok-IN", "ku-Arab-IQ", "ky-KG", "lb-LU", "lo-LA", "lt-LT", "lv-LV", "mi-NZ", "mk-MK", "ml-IN", "mn-MN", "mr-IN", "ms-MY", "mt-MT", "nb-NO", "ne-NP", "nl-NL", "nn-NO", "nso-ZA", "or-IN", "pa-IN", "pl-PL", "prs-AF", "pt-BR", "pt-PT", "quc-Latn-GT", "quz-PE", "ro-RO", "ru-RU", "rw-RW", "sd-Arab-PK", "si-LK", "sk-SK", "sl-SI", "sq-AL", "sr-Cyrl-BA", "sr-Cyrl-RS", "sr-Latn-RS", "sv-SE", "sw-KE", "ta-IN", "te-IN", "tg-Cyrl-TJ", "th-TH", "ti-ET", "tk-TM", "tn-ZA", "tr-TR", "tt-RU", "ug-CN", "uk-UA", "ur-PK", "uz-Latn-UZ", "vi-VN", "wo-SN", "xh-ZA", "yo-NG", "zh-CN", "zh-TW", "zu-ZA"};
        languageComboBox = new JComboBox<>(languages);
        languageComboBox.setEditable(true);
        languageComboBox.setSelectedItem("en-GB");
        gbc.gridx = 1; gbc.gridwidth = 3; formPanel.add(languageComboBox, gbc);
        
        processAllCheckbox = new JCheckBox("Convert all JSON files in the folder");
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 4; formPanel.add(processAllCheckbox, gbc);
        
        enableOutputCheckbox = new JCheckBox("Specify output folder (otherwise, output is saved next to input)");
        gbc.gridy = 7; formPanel.add(enableOutputCheckbox, gbc);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        runButton = new JButton("Run Conversion");
        // CORRECTED: Button size updated
        runButton.setPreferredSize(new Dimension(500, runButton.getPreferredSize().height + 4));

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        
        JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        runPanel.add(runButton);
        
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        settingsPanel.add(new JLabel("Theme:"));
        settingsPanel.add(themeComboBox);
        
        JButton resetButton = new JButton("Reset Settings");
        settingsPanel.add(resetButton);
        
        bottomPanel.add(runPanel, BorderLayout.NORTH);
        bottomPanel.add(settingsPanel, BorderLayout.SOUTH);
        
        add(formPanel, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setupGroovyScripts();
        loadAppIcon();
        
        // CORRECTED: Added DocumentListener to dynamically show/hide the new checkbox
        stringsField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void update() {
                saveStringsAsJsonCheckbox.setVisible(stringsField.getText().trim().toLowerCase().endsWith(".txt"));
            }
        });

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_ABOUT)) {
                Desktop.getDesktop().setAboutHandler(this);
            }
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                    var dockIconUrl = getClass().getClassLoader().getResource("JsonConverter.png");
                    if (dockIconUrl != null) {
                        Image dockIcon = defaultToolkit.getImage(dockIconUrl);
                        taskbar.setIconImage(dockIcon);
                    }
                }
            }
        } catch (Exception e) {
            log("Warning: Could not set platform-specific integration. " + e.getMessage());
        }

        themeComboBox.addActionListener(e -> {
            ThemeInfo selectedTheme = (ThemeInfo) themeComboBox.getSelectedItem();
            if (selectedTheme != null) {
                changeTheme(selectedTheme.className);
            }
        });
        
        resetButton.addActionListener(e -> resetSettings());

        String lastTheme = prefs.get("theme", "");
        boolean themeSet = false;
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].className.equals(lastTheme)) {
                themeComboBox.setSelectedIndex(i);
                themeSet = true;
                break;
            }
        }
        if (!themeSet) {
             themeComboBox.setSelectedIndex(0);
        }

        htmlRadioButton.addActionListener(e -> darkModeCheckbox.setVisible(true));
        docxRadioButton.addActionListener(e -> darkModeCheckbox.setVisible(false));
        darkModeCheckbox.setVisible(false);

        processAllCheckbox.addActionListener(e -> updateFileChooserBehavior());
        enableOutputCheckbox.addActionListener(e -> updateOutputBrowseEnabled());
        inputBrowseButton.addActionListener(e -> openFileChooser(inputField, "input"));
        stringsBrowseButton.addActionListener(e -> openFileChooser(stringsField, "strings"));
        outputBrowseButton.addActionListener(e -> openFileChooser(outputField, "output"));
        runButton.addActionListener(e -> runConversion());
        
        updateFileChooserBehavior();
        updateOutputBrowseEnabled();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void resetSettings() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "This will reset the saved theme and last used directory. Are you sure?",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                prefs.clear();
                prefs.flush();

                String defaultLafClass = "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme";
                UIManager.setLookAndFeel(defaultLafClass);
                FlatLaf.updateUI();
                
                themeComboBox.setSelectedIndex(0);
                
                lastUsedDirectory = System.getProperty("user.home");
                inputField.setText("");
                stringsField.setText("");
                outputField.setText("");
                
                log("All settings have been reset to their defaults.");

            } catch (BackingStoreException | UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                log("Error resetting settings: " + ex.getMessage());
            }
        }
    }

    @Override
    public void handleAbout(AboutEvent e) {
        showAboutDialog();
    }

    private void showAboutDialog() {
        String version = "1.0.0"; 
        String vendorLine = "Kos Ivantsov"; 

        try (InputStream manifestStream = getClass().getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
            if (manifestStream != null) {
                java.util.jar.Manifest manifest = new java.util.jar.Manifest(manifestStream);
                java.util.jar.Attributes attributes = manifest.getMainAttributes();
                String implVersion = attributes.getValue("Implementation-Version");
                String implVendor = attributes.getValue("Implementation-Vendor");

                if (implVersion != null && !implVersion.isEmpty()) {
                    version = implVersion;
                }
                if (implVendor != null && !implVendor.isEmpty()) {
                    vendorLine = implVendor;
                }
            }
        } catch (IOException ex) {
            log("Warning: Could not read manifest file. " + ex.getMessage());
        }

        JDialog aboutDialog = new JDialog(this, "About The Word 121 Json Converter", true);
        aboutDialog.setLayout(new BorderLayout(15, 15));
        aboutDialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        java.net.URL iconURL = getClass().getClassLoader().getResource("JsonConverter.png");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            Image image = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(image));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            aboutDialog.add(iconLabel, BorderLayout.WEST);
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));

        JLabel appNameLabel = new JLabel("The Word 121 Json Converter");
        appNameLabel.setFont(new Font(appNameLabel.getFont().getName(), Font.BOLD, 16));
        
        JLabel versionLabel = new JLabel("Version: " + version);

        appNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(appNameLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        textPanel.add(versionLabel);
        
        String[] vendorParts = vendorLine.split("\\|");
        JLabel copyrightLabel = new JLabel("Copyright © 2025 " + vendorParts[0].trim());
        copyrightLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(copyrightLabel);

        if (vendorParts.length > 1) {
            JLabel companyLabel = new JLabel(vendorParts[1].trim());
            companyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(companyLabel);
        }

        aboutDialog.add(textPanel, BorderLayout.CENTER);
        aboutDialog.pack();
        aboutDialog.setResizable(false);
        aboutDialog.setLocationRelativeTo(this);
        aboutDialog.setVisible(true);
    }
    
    private void changeTheme(String className) {
        try {
            UIManager.setLookAndFeel(className);
            FlatLaf.updateUI();
            prefs.put("theme", className);
        } catch (Exception ex) {
            log("Failed to change theme: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        String defaultTheme = "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme";
        String theme = prefs.get("theme", defaultTheme);
        try {
            UIManager.setLookAndFeel(theme);
        } catch (Exception ex) {
            System.err.println("Failed to set initial theme: " + ex.getMessage());
            try { UIManager.setLookAndFeel(defaultTheme); } catch (Exception e) { e.printStackTrace(); }
        }
        
        SwingUtilities.invokeLater(JsonConverterGui::new);
    }

    private static String guitxtToJson(String guitxt) {
        Pattern pattern = Pattern.compile("Key:\\s*\"([^\"]+)\",\\s*Value:\\s*\"([^\"]+)\"\\s*;?");
        Matcher matcher = pattern.matcher(guitxt);
        Map<String, String> map = new LinkedHashMap<>();
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2));
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(map);
    }
    
    private void updateFileChooserBehavior() {
        inputBrowseButton.setText(processAllCheckbox.isSelected() ? "Select Folder..." : "Select File...");
    }

    private void openFileChooser(JTextField targetField, String fieldType) {
        JFileChooser chooser = new JFileChooser();
        String currentPath = targetField.getText().trim();
        
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            chooser.setCurrentDirectory(currentFile.isDirectory() ? currentFile : currentFile.getParentFile());
        } else {
            chooser.setCurrentDirectory(new File(lastUsedDirectory));
        }

        if ("input".equals(fieldType)) {
            chooser.setFileSelectionMode(processAllCheckbox.isSelected() ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
            if (!processAllCheckbox.isSelected()) {
                chooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));
            }
        } else if ("strings".equals(fieldType)) {
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileNameExtensionFilter("Strings Files (*.json, *.txt)", "json", "txt"));
        } else { 
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        
        if ("output".equals(fieldType) && !processAllCheckbox.isSelected() && !inputField.getText().trim().isEmpty()) {
            String inputFileName = new File(inputField.getText().trim()).getName();
            String baseName = FilenameUtils.getBaseName(inputFileName);
            String extension = docxRadioButton.isSelected() ? ".docx" : ".html";
            chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), baseName + extension));
        }

        int result;
        if ("output".equals(fieldType) && !processAllCheckbox.isSelected()) {
            result = chooser.showSaveDialog(this);
        } else {
            result = chooser.showOpenDialog(this);
        }

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            targetField.setText(selectedFile.getAbsolutePath());
            lastUsedDirectory = selectedFile.getParent();
            prefs.put("lastUsedDirectory", lastUsedDirectory);
        }
    }
    
    private void setupGroovyScripts() {
        log("Locating compiled Groovy scripts...");
        try {
            docxScriptClass = Class.forName("JsonToDocxScript");
            htmlScriptClass = Class.forName("JsonToHtmlScript");
            log("Scripts located successfully.");
        } catch (ClassNotFoundException e) {
            String errorMsg = "FATAL ERROR: Could not find a required script class.\n" + e.getMessage();
            log(errorMsg);
            JOptionPane.showMessageDialog(this, errorMsg, "Fatal Error", JOptionPane.ERROR_MESSAGE);
            runButton.setEnabled(false);
        }
    }
    
    // CORRECTED: Updated the conversion logic to handle the new checkbox
    private void runConversion() {
        runButton.setEnabled(false);
        logArea.setText("");
        log("--- Starting New Conversion ---");

        final String inputPath = inputField.getText().trim();
        final String initialStringsPath = stringsField.getText().trim();
        final boolean saveStrings = saveStringsAsJsonCheckbox.isSelected();
        final String lang = (String) languageComboBox.getSelectedItem();
        final boolean isBatchMode = processAllCheckbox.isSelected();
        final boolean isHtmlMode = htmlRadioButton.isSelected();
        final boolean useDarkMode = darkModeCheckbox.isSelected();
        final boolean specifyOutput = enableOutputCheckbox.isSelected();
        final String outputDir = outputField.getText().trim();

        if (inputPath.isEmpty()) {
            log("ERROR: Input path must be provided.");
            runButton.setEnabled(true);
            return;
        }

        new Thread(() -> {
            String effectiveStringsPath = initialStringsPath;
            try {
                if (!initialStringsPath.isEmpty() && initialStringsPath.toLowerCase().endsWith(".txt")) {
                    log("Converting strings file: " + new File(initialStringsPath).getName());
                    String txtContent = new String(Files.readAllBytes(Paths.get(initialStringsPath)), StandardCharsets.UTF_8);
                    String jsonContent = guitxtToJson(txtContent);

                    File outputFile;
                    if (saveStrings) {
                        // Save permanently next to the source .txt file
                        String jsonOutputPath = FilenameUtils.removeExtension(initialStringsPath) + ".json";
                        outputFile = new File(jsonOutputPath);
                        log("Saving converted strings to: " + outputFile.getName());
                    } else {
                        // Use a temporary file
                        outputFile = File.createTempFile("temp_strings_", ".json");
                        outputFile.deleteOnExit();
                        log("Using temporary strings JSON: " + outputFile.getName());
                    }
                    
                    try (Writer writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
                        writer.write(jsonContent);
                    }
                    effectiveStringsPath = outputFile.getAbsolutePath();
                }

                if (isBatchMode) {
                    File inputFile = new File(inputPath);
                    String dirPath = inputFile.isDirectory() ? inputFile.getAbsolutePath() : inputFile.getParent();
                    File inputDir = new File(dirPath);

                    File[] files = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
                    if (files == null || files.length == 0) {
                        log("No .json files found in the specified directory: " + dirPath);
                        return;
                    }
                    log("Found " + files.length + " JSON files to process in " + dirPath);
                    for (File file : files) {
                        processSingleFile(file, effectiveStringsPath, lang, isHtmlMode, useDarkMode, specifyOutput, outputDir);
                    }
                } else {
                    processSingleFile(new File(inputPath), effectiveStringsPath, lang, isHtmlMode, useDarkMode, specifyOutput, outputDir);
                }
                log("--- Conversion Finished ---");
            } catch (Exception e) {
                log("An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
            } finally {
                SwingUtilities.invokeLater(() -> runButton.setEnabled(true));
            }
        }).start();
    }
    
    private void processSingleFile(File inputFile, String stringsPath, String lang, boolean isHtml, boolean useDarkMode, boolean specifyOutput, String outputDir) {
        try {
            Class<?> scriptClass = isHtml ? htmlScriptClass : docxScriptClass;
            String extension = isHtml ? ".html" : ".docx";

            if (scriptClass == null) {
                log("ERROR: No script available for the selected format.");
                return;
            }

            String outputFilePath;
            if (specifyOutput) {
                if (outputDir.isEmpty()) {
                    log("ERROR: Please specify an output directory when the checkbox is enabled.");
                    return;
                }
                String baseName = FilenameUtils.getBaseName(inputFile.getName());
                outputFilePath = new File(outputDir, baseName + extension).getAbsolutePath();
            } else {
                outputFilePath = new File(inputFile.getParent(), FilenameUtils.getBaseName(inputFile.getName()) + extension).getAbsolutePath();
            }

            log("Processing: " + inputFile.getName() + " -> " + new File(outputFilePath).getName());
            Object scriptInstance = scriptClass.getDeclaredConstructor().newInstance();
            setScriptField(scriptClass, scriptInstance, "inputPath", inputFile.getAbsolutePath());
            setScriptField(scriptClass, scriptInstance, "stringsPath", stringsPath);
            setScriptField(scriptClass, scriptInstance, "outputPath", outputFilePath);
            
            if (!isHtml) {
                setScriptField(scriptClass, scriptInstance, "lang", lang);
            } else {
                setScriptField(scriptClass, scriptInstance, "darkMode", useDarkMode);
            }

            scriptClass.getMethod("run").invoke(scriptInstance);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String errorMessage = (cause != null) ? cause.getMessage() : e.getMessage();
            log("Failed to process " + inputFile.getName() + ": " + errorMessage);
            e.printStackTrace();
        }
    }
    
    private void setScriptField(Class<?> scriptClass, Object instance, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        if (value != null) {
            Field field = scriptClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        }
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void updateOutputBrowseEnabled() {
        boolean enabled = enableOutputCheckbox.isSelected();
        outputField.setEnabled(enabled);
        outputBrowseButton.setEnabled(enabled);
    }
    

    private void loadAppIcon() {
        try {
            java.net.URL iconURL = getClass().getClassLoader().getResource("JsonConverter.png");
            if (iconURL != null) {
                Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);
                setIconImage(icon);
            } else {
                log("Info: 'JsonConverter.png' not found for window icon. Using default.");
            }
        } catch (Exception e) {
            log("Warning: Could not load window icon. " + e.getMessage());
        }
    }
}
