# Distribution Guide
```sh
mvn clean javafx:jlink
```
This will save a JavaFX runtime image in the `target` directory. The runtime image is a self-contained Java application that includes the JavaFX runtime and your application code.

## Building Native Applications

### macOS Distribution (.app and .dmg)
From your macOS machine:

```sh
# Build the runtime image first
mvn clean javafx:jlink

# Create macOS app and DMG installer
mvn jpackage:jpackage
```

This produces:
- A .dmg installer file in target/dist/
- The .app bundle inside the DMG
### Windows Distribution (.exe and installer)
Windows executables can only be built on a Windows system.
```sh
# Build the runtime image first
mvn clean javafx:jlink

# Create macOS app and DMG installer
mvn jpackage:jpackage
```
