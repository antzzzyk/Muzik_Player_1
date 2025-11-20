# Building a Windows `.exe`

These steps use the JDK `jpackage` tool and the JavaFX Windows SDK.

## Prerequisites
- Windows machine with JDK 17+ installed (includes `jpackage`).
- JavaFX SDK for Windows (e.g., `javafx-sdk-21.0.2`) extracted locally.
- Optional: an `.ico` file for the app icon.

Set an environment variable pointing to the JavaFX SDK folder:
```bat
set JAVAFX_SDK=C:\path\to\javafx-sdk-21.0.2
```

## 1) Compile the application
From the repository root:
```bat
mkdir out\classes
javac --module-path %JAVAFX_SDK%\lib --add-modules javafx.controls,javafx.media ^
      -d out\classes src\main\java\org\example\ugplayer\*.java
```

## 2) Package a runnable JAR
```bat
jar --create --file out\MuzikPlayer.jar -C out\classes .
```

## 3) Create the installer / EXE
`jpackage` will build a self-contained Windows installer or portable `.exe` using the compiled JAR and JavaFX runtime libraries:
```bat
jpackage --type exe ^
  --name MuzikPlayer ^
  --input out ^
  --main-jar MuzikPlayer.jar ^
  --main-class org.example.ugplayer.MainUI ^
  --module-path %JAVAFX_SDK%\lib ^
  --add-modules javafx.controls,javafx.media,javafx.graphics ^
  --win-shortcut --win-menu ^
  --icon path\to\icon.ico
```

### Notes
- Include a `songs` folder next to the executable (or bundle songs inside the installer) so the player can locate your audio files.
- If you prefer an MSI installer, change `--type exe` to `--type msi`.
- You can add `--runtime-image` to reuse a pre-created Java runtime image if desired.
