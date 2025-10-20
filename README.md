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

You can download the latest pre-packaged release for your operating system from the **[Releases page](https://github.com/kosivantsov/w121_json_converter/releases)**.

-   **macOS**: Download the `.zip` file, extract it, and you will find the `The Word121 JSON Converter.app` bundle.
-   **Windows**: Download the `.zip` containing the application folder and run the `.exe` file.
-   **Linux**: Download the `.deb` package, which can be installed on Debian-based systems.

No Java installation is required, as the necessary runtime is bundled with the application.

## Usage

1.  Launch the application.
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
