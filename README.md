# The Word One to One JSON Converter

A Java desktop utility to convert [The Word One to One](https://www.theword121.com/) episode files from their JSON translation format into user-friendly DOCX and HTML documents.

This tool was created to provide a simple way to preview and proofread translated episodes outside of the official CMS.

<img width="90%" alt="Main App" src="https://github.com/user-attachments/assets/41548b41-2102-4a93-948f-8cfe0b013751" />


## Key Features

-   **Batch Conversion**: Convert individual JSON files or an entire folder of JSON files at once.
-   **App String Injection**: If a `.txt` or `.json` file containing the mobile app's UI strings is provided, these elements (which are absent from the episode JSON) will be correctly inserted into the output. Otherwise, they are omitted.
-   **Dual Output Formats**:
    -   **HTML**: Supports both light and dark modes for comfortable viewing.
      <img width="600" alt="HTML Output" src="https://github.com/user-attachments/assets/92d4d285-016b-4397-93af-4c252fa72f05" />

    -   **DOCX**: Allows selection of a spell-checking language for easier proofreading in Microsoft Word and other editors.
      <img width="600" alt="DOCX Output" src="https://github.com/user-attachments/assets/216426ab-54db-4a1e-8c5c-6d8b2e4e03fe" />
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
