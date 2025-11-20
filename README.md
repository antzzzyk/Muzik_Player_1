# Muzik Player

A simple JavaFX-based music player featuring a full UI and a compact iPod-style mini player. The main window loads audio files from a `songs` directory (resources or working directory) and supports adding/removing tracks, theming, and volume controls. The mini player stays in sync with playback and shows metadata, progress, and quick navigation.

## Running
The project expects JavaFX on the classpath. Compile and launch the `org.example.ugplayer.MainUI` application class with your preferred build tooling (e.g., Maven, Gradle, or `javac/java` with the appropriate `--module-path` and `--add-modules` arguments for JavaFX).

## Windows EXE packaging
Use the step-by-step guide in [`packaging/windows/BUILD_EXE.md`](packaging/windows/BUILD_EXE.md) to compile the app and build a self-contained `.exe` (or MSI) with `jpackage` and the JavaFX Windows SDK.

## Testing
Automated tests are not bundled with this project. When a status mentions **"Testing not run"**, it simply means no test suite was executed in that environment (for example, because the JavaFX toolchain was unavailable). To manually verify the app:

- Run the JavaFX application locally and exercise playback, next/previous, volume, and theming controls.
- Try adding and deleting songs via the menu to confirm the library refreshes correctly.
- Open the mini player to check that metadata, progress, and play/pause sync with the main player.
