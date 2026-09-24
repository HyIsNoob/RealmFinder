# RealmFinder 1.1.0 Settings Design

## Goal

Add an in-game settings screen that lets players choose between a playful ruleset and a more balanced ruleset without editing JSON. Keep multiplayer rules authoritative on the server and keep personal display preferences local to each player.

## Access and layout

- Register **O** as the default `Open RealmFinder Settings` key. Players can rebind it in Minecraft Controls.
- The key opens the screen only while the player is in a world and no other screen is open.
- Split the screen into **Gameplay Rules** and **Personal Settings**.
- Every setting has a short hover tooltip that explains its effect in player-facing language.
- Provide **Save**, **Cancel**, and **Reset to Defaults** buttons.
- Show server-owned settings as disabled for multiplayer users without operator permission, with a tooltip explaining that only a server operator can change them.

## Gameplay rules

The server owns these values and applies them to every player:

- **Copy Container Contents** (default: on): copied chests keep their stored items. When off, photographed containers are placed empty.
- **Copy Mob Equipment & Inventory** (default: on): recreated mobs retain armor, held items and inventories. Other saved state such as health, variant and villager profession remains available.
- **Capture Entities** (default: on): enables or disables capturing living mobs.
- **Require Empty Photograph** (default: on): successful Survival captures consume one Empty Photograph.
- **Use Camera Durability** (default: on): successful Survival captures damage the camera. The camera retains its 100-shot capacity when enabled.
- **Allow Placement Carving** (default: on): normal placement may clear obstacles inside the photographed structure. When off, all placement is additive.
- **Maximum Captured Blocks** (default: 16,384; range: 1–16,384).
- **Maximum Captured Entities** (default: 16; range: 0–16).
- **Undo History Size** (default: 10; range: 1–20).

Changes are validated on the server, saved to `config/realmfinder.json`, and applied immediately. The server rejects updates from players without operator permission. A client requests the current values whenever the screen opens so remote server rules are displayed accurately.

## Personal settings

These values are stored locally and affect only the current player:

- **Confirm Before Undo** (default: off): pressing Undo opens a Yes/No confirmation before sending the request.
- **Maximum Camera Zoom** (default: 4; range: 1–6).
- **Show HUD Hints** (default: on): shows or hides control hints while holding the Camera or a Photograph.

Personal values are saved separately from server gameplay rules so joining a server cannot overwrite the player's preferences.

## Data and networking

- Split the existing combined config into a server gameplay settings object and a client preferences object while continuing to read existing `realmfinder.json` values.
- Add bounded server-to-client settings data, a request packet, and an update packet.
- Validate every numeric range and boolean on the server. Ignore malformed or unauthorized updates.
- After an accepted update, save the server config and send the authoritative values back to the editor.
- The Undo confirmation is entirely client-side; the actual Undo remains server-side.

## Gameplay integration

- Container NBT is captured only when **Copy Container Contents** is enabled. Signs remain unaffected.
- Mob NBT sanitization removes equipment and inventory data when **Copy Mob Equipment & Inventory** is disabled.
- Capture inventory and camera durability checks read their corresponding server rules.
- Placement treats carving as disabled when the server rule is off, even if the client requests it.
- Existing photographs remain readable. A photograph taken while container copying is disabled contains no stored container items.

## Release files

- Set the mod version to **1.1.0**.
- Add `CURSEFORGE_DESCRIPTION.md` with a short English player-facing description.
- Add `CHANGELOG.md` containing a concise 1.1.0 entry that can be pasted into CurseForge.
- Update README and the Vietnamese gameplay smoke test with the settings key and the new gameplay rules.

## Verification

- Unit tests cover defaults, range validation, old-config compatibility, removal of container contents, removal of mob equipment/inventory, and rule decisions.
- Networking tests cover permission and range validation where the APIs allow isolated testing.
- Build the remapped Fabric 1.21.1 JAR and inspect its embedded version.
- Manual smoke tests cover opening with O, rebinding the key, hover tooltips, Save/Cancel/Reset, non-operator read-only behavior, chest copying on/off, mob equipment copying on/off, capture costs, carving, HUD hints, and Undo confirmation.
