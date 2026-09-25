# RealmFinder NeoForge 1.21.1 port

This standalone Gradle project builds RealmFinder 1.1.0 for NeoForge 1.21.1. It compiles shared gameplay and assets directly from the repository root; the Java files under this directory replace Fabric-specific integration code. Keep the same `realmfinder` IDs and snapshot format in both loaders.

From this directory on Windows:

```powershell
..\..\gradlew.bat build --console=plain
..\..\gradlew.bat runClient --console=plain
..\..\gradlew.bat runServer --console=plain
```

Output: `build/libs/realmfinder-neoforge-1.21.1-1.1.0.jar`.

The unit test task runs 24 tests. Three test classes that call vanilla `Bootstrap` directly run only in the Fabric project because NeoForge requires a full mod loader context for that bootstrap. The client loaded resources without a startup crash and the dedicated server reached `Done`; in-game capture, placement, undo, multiplayer and existing-save compatibility still need manual smoke tests before this port is released.
