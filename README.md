<p align="left"><img src="https://scontent-ham3-1.xx.fbcdn.net/v/t39.30808-1/326409933_859165191807363_6166716551764762023_n.png?stp=dst-png_s480x480&_nc_cat=110&ccb=1-7&_nc_sid=2d3e12&_nc_ohc=-Q3j4wjZncIQ7kNvwGc4Fad&_nc_oc=Adn-w27ekBhMAdOpP5UXgUkhgp1BbuAP-6LitE1yNevNE6Po_FfmQ3f8KTf1NXN7gx8&_nc_zt=24&_nc_ht=scontent-ham3-1.xx&_nc_gid=kU_nKR8-KFGM_ljD07KfHQ&oh=00_AfdyGkzFW7_u0zzVQY1fLrDP3oSdC_EoCGZ0QTE4louZCQ&oe=68FBF032" alt="W121 Logo" width="100"/></p>

# The Word One to One JSON Converter

A Java desktop utility to convert [The Word One to One](https://www.theword121.com/) episode files from their JSON translation format into user-friendly DOCX and HTML documents.

This tool was created to provide a simple way to preview and proofread translated episodes outside of the official CMS.

<img width="70%" alt="GUI Light Theme" src="https://github.com/user-attachments/assets/7c505df0-05ad-467f-b669-33fee94374d3" />
<img width="70%" alt="GUI Dark Theme" src="https://github.com/user-attachments/assets/6d8751d4-3ef8-431e-98fc-39f1d3e3e6ee" />




## Key Features

-   **Batch Conversion**: Convert individual JSON files or an entire folder of JSON files at once.
-   **App String Injection**: If a `.txt` or `.json` file containing the mobile app's UI strings is provided, these elements (which are absent from the episode JSON) will be correctly inserted into the output. Otherwise, they are omitted.
-   **Dual Output Formats**:
    -   **HTML**: Supports both light and dark modes for comfortable viewing.

        <img width="600" alt="HTML Light" src="https://github.com/user-attachments/assets/3cb30543-8639-4544-9c39-5a8c5905ce0e" />
        <img width="600" alt="HTML Dark" src="https://github.com/user-attachments/assets/e9f6e4d8-0c39-4783-a422-c1f0e63974af" />
        
    -   **DOCX**: Allows selection of a spell-checking language for easier proofreading in Microsoft Word and other editors.

        <img width="600" alt="DOCX" src="https://github.com/user-attachments/assets/9c446748-5088-43de-ad34-e372075072c0" />
-   **Flexible Output Location**: Save converted files to a specific folder or place them alongside the original input files.
-   **Native Look and Feel**: The application includes several themes to integrate visually with different operating systems (Windows, macOS, Linux).

## Installation

Download the latest assets for your operating system from the [Releases page](https://github.com/kosivantsov/w121_json_converter/releases).

The packaged installers for macOS, Windows, and Linux are the recommended method as they include the required Java runtime and do not require you to have Java installed.

### Packaged Installers

*   **macOS**: Download the `JsonConverter-macOS.zip` archive. After extracting it, drag `The Word121 JSON Converter.app` to your `/Applications` folder.
    *   **Note**: As the application is not from an identified developer, you must manually approve it.
        1.  Right-click the app icon and select "Open." A warning dialog will appear. Select "Open Anyway".
        2.  Go to `System Settings` > `Privacy & Security`.
        3.  Scroll down to the security section, where you will find a message about the app being blocked. Click the "Open Anyway" button.
        4.  You can now launch the application.

*   **Windows**: Download the `JsonConverter-Windows.zip` archive. Extract the contents and run the `The Word121 JSON Converter.exe` file. This is a portable version that doesn't require installation.
    *   **Note**: Windows SmartScreen might block the application. If this happens, click "More info" and then "Run anyway."

*   **Linux**: Download the `JsonConverter-Linux-DEB.zip` archive and extract the contents. Install it on a Debian-based system (e.g., Ubuntu) with `sudo dpkg -i the-word121-json-converter_<version>_amd64.deb`.
    *   The executable will be installed as `/opt/the-word121-json-converter/bin/The Word121 JSON Converter`.

### Using the JAR File

If you have Java 17 or newer installed on your system, you can use the cross-platform JAR file.

1.  Download `JsonConverter-<version>.jar.zip` from the [Releases page](https://github.com/kosivantsov/w121_json_converter/releases) and extract the archive.
2.  Launch the resulting `JsonConverter-<version>.jar` file by either double-clicking it or running the following command in your terminal:

```
java -jar JsonConverter-<version>.jar
```

## Usage

1.  **Launch** the application.
2.  **Select Conversion Mode**:
    -   For converting an entire folder of `.json` files, check the box labeled **"Convert all JSON files in the selected folder"**.
    -   For converting a single file, leave this box unchecked.

3.  **Choose Your Input**:
    -   Click the **"Select Folder..."** or **"Select File..."** button to choose your input.
    -   If converting a whole folder, you can either select the directory itself or select any `.json` file within it.

4.  **Provide App Strings (Optional)**:
    -   If you have a `.txt` or `.json` file with the UI strings from The Word One to One app, specify it in the "Strings File" field. This will correctly insert those UI elements into your output documents.
    -   If the provided file is a `.txt`, you can check the **"Save the strings file in JSON"** box to create a permanent, converted `.json` version for future use.

5.  **Configure Output**:
    -   Choose your desired output format: **DOCX** or **HTML**.
    -   The **"Language"** dropdown for spell-checking is only available for DOCX output.
    -   **"Dark Mode"** is only available for HTML output.
    -   By default, converted files are saved next to their originals. To choose a different location, check **"Specify output folder"** and select a directory.

6.  Click the **"Run Conversion"** button to begin.

## Building from Source

To build the project yourself, you will need:

-   JDK 17
-   Git

1.  **Clone the repository:**
    ```
    git clone https://github.com/kosivantsov/w121_json_converter.git
    cd w121_json_converter
    ```

2.  **Build the cross-platform JAR using the Gradle wrapper:**
    -   On macOS/Linux:
        ```
        ./gradlew shadowJar
        ```
    -   On Windows:
        ```
        gradlew.bat shadowJar
        ```

    The runnable JAR file will be located at `build/libs/JsonConverter-1.0.jar`.
    
3.  **Build an application bundle for your operating system using the Gradle wrapper:**
    -  On macOS/Linux:
       ```
       ./gradlew jpackage
       ```
    -  On Windows:
       ```
       gradlew.bat jpackage
       ```
       
    The compiled application will be located at `build/jpackage/`.

## Legal Notice

The application icon and the name "The Word One to One" are the property of [The Word One to One](https://www.theword121.com/). This is an unofficial, third-party utility created to assist with the translation workflow.
