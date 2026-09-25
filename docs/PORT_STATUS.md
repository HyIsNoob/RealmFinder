# RealmFinder 1.1.0 port status

| Minecraft | Fabric | Forge | NeoForge |
| --- | --- | --- | --- |
| 1.20.1 | Build and client startup checked; gameplay test pending | Build and client startup checked; gameplay test pending | — |
| 1.21.1 | Existing release target | — | Build and startup checked; gameplay test pending |
| 1.21.4 | Deferred | — | Deferred |

The NeoForge 1.21.1 port is in [`ports/neoforge-1.21.1`](../ports/neoforge-1.21.1/README.md). It reuses the current gameplay source and assets, with separate registration, events, networking and client hooks. `gradlew test build` passes in that port (24 tests). The client loaded resources without a startup crash and the dedicated server reached `Done` with NeoForge 21.1.251. Those checks do not verify capture, preview, placement, undo, tamed mobs, album, stand, or multiplayer during play.

Before releasing this port, run the [Vietnamese gameplay smoke test](GAMEPLAY_SMOKE_TEST.md) on both client and a two-player dedicated server. Check photographs made on Fabric in NeoForge and vice versa before claiming cross-loader save compatibility. Do not install the Fabric and NeoForge JARs together.

The new 1.20.1 projects are in [`ports/fabric-1.20.1`](../ports/fabric-1.20.1/) and [`ports/forge-1.20.1`](../ports/forge-1.20.1/). Both target Java 17 and produce separate version 1.1.0 JARs. `test build` passed with 47 tests on each port; both clients loaded to the main menu. The Forge development log confirms its camera zoom and mouse scroll mixins were applied. Both JARs contain the 1.20.1 crafting recipes, photo stand model, and empty photograph model. Dedicated server startup is checked only up to the EULA gate; it has not been accepted in this workspace.

Before distributing either 1.20.1 JAR, run the [Vietnamese gameplay smoke test](GAMEPLAY_SMOKE_TEST.md) in Survival and Creative, then repeat capture, placement, undo, album, stand, settings, and tamed mobs with two players on a dedicated server. Check world save/reload and photographs passed between players. These gameplay behaviors have not been verified by the build or menu startup checks. Do not install the Fabric and Forge JARs together. Work on 1.21.4 is deferred until these 1.20.1 tests are complete.
