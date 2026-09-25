# RealmFinder: version and loader support plan

## Goal and current release

Ship one artifact per Minecraft version and loader while keeping the camera → photograph → perspective placement → undo loop consistent. The current release target is **RealmFinder 1.1.0, Fabric 1.21.1**. NeoForge 1.21.1 has a buildable port pending in-game validation; the other targets remain planned.

| Minecraft | Fabric | Forge | NeoForge |
| --- | --- | --- | --- |
| 1.20.1 | Planned | Planned | — |
| 1.21.1 | 1.1.0 release target | — | Builds; gameplay test pending |
| 1.21.4 | Planned | — | Planned |

Each cell needs its own tested JAR and dependency metadata. Keep the mod ID `realmfinder`, block/item registry IDs, recipe IDs, and photo data format stable where possible. Version the snapshot format explicitly before changing its serialization; migrating an existing world or photograph must be tested, not assumed.

## Porting strategy

1. **Freeze and verify Fabric 1.21.1.** Build the distributable JAR, verify its embedded `fabric.mod.json`, run the Vietnamese [gameplay smoke test](../../GAMEPLAY_SMOKE_TEST.md) in a fresh world and an upgraded save, then repeat camera, pet capture, placement and undo on a dedicated server with two players. Fix release blockers here before copying implementation to other targets.
2. **Extract only code that is genuinely shared.** Start with snapshot data, serialization rules, perspective math, placement validation policy and undo conflict policy. Keep registration, event hooks, networking, UI, keybinds, client rendering, and world mutation behind small version/loader adapters. Preserve the existing Fabric implementation until a shared module compiles and passes its tests. Avoid a broad rewrite.
3. **Port to NeoForge 1.21.1.** This isolates loader differences while Minecraft's game version is unchanged. Replace Fabric registration/events/network hooks with NeoForge equivalents; compile and run a dedicated server before bringing over client UI and rendering. Compare preview geometry and actual placement using the same saved photographs.
4. **Port Fabric 1.21.4, then NeoForge 1.21.4.** Handle changed game APIs and 1.21.4 item model definitions in version-specific resources. Verify all item/block models, the creative tab, album, photo stand, rendering, and packet codecs in each build.
5. **Port Fabric 1.20.1, then Forge 1.20.1.** Work backward from the stable snapshot format. Adapt older item data/serialization, registration, recipes, and networking; set the appropriate Java toolchain for the 1.20.1 builds. Forge's `DeferredRegister` is the starting point for registry adapters. Do not downgrade the 1.21 branches to accommodate older APIs.

Keep dependencies and resource formats version-specific. Name output artifacts with mod version, Minecraft version and loader to prevent installing the wrong JAR. Maintain a support matrix in the README and release page; version `1.1.0` is the mod version, not a promise that all planned ports are complete.

## Gates for every target

- [ ] Clean compile and automated tests with the target's Java toolchain; verify JAR metadata, loader ID, mod version and Minecraft version range.
- [ ] Launch a client and dedicated server; connect two clients with only the matching RealmFinder build and required dependencies.
- [ ] Check creative tab contents, recipes, camera zoom/capture limits, blank-photo consumption, album, photo stand facing/model and localisation.
- [ ] Compare preview with placed blocks at near, far, 2x and 3x scales, including horizontal photos placed vertically, doors and block entities.
- [ ] Capture and place ordinary entities and tamed companions; check ownership, new entity identity, and copied equipment/inventory according to server settings.
- [ ] Undo immediately, after block-state changes, after switching dimension, after chunk unload/reload, and after server restart. Check that true block removal conflicts are still reported clearly.
- [ ] Load a world and photograph made by an earlier supported build; document migrations or explicit incompatibilities.
- [ ] Publish a per-target changelog and tested dependency list only after the in-game gates pass.

## API references to consult while porting

- [Fabric creative tabs, 1.21.1](https://docs.fabricmc.net/1.21.1/develop/items/custom-creative-tabs)
- [Forge registries, 1.20.1](https://docs.minecraftforge.net/en/1.20.1/concepts/registries/)
- [NeoForge item models, 1.21.4](https://docs.neoforged.net/docs/1.21.4/resources/client/models/items/)
- [NeoForge 1.21.4 migration primer](https://docs.neoforged.net/primer/docs/1.21.4/)
