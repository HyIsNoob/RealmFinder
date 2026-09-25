# RealmFinder

Bring your photographs to life. Inspired by the indie puzzle game *Viewfinder*, RealmFinder lets you capture slices of the Minecraft world with a camera and stamp them back into reality from your own perspective.

Current release target: **RealmFinder 1.1.0 for Fabric 1.21.1**. Fabric and Forge 1.20.1 ports now build and start, but still need in-game testing before release. The NeoForge 1.21.1 port also needs its full gameplay smoke test. See the [port status](docs/PORT_STATUS.md) before distributing another loader build.

All crafting layouts and ingredient counts: [Công thức chế tạo RealmFinder](docs/CRAFTING_RECIPES.md).

---

## How to Play

1. Grab a Viewfinder Camera from the RealmFinder creative tab, or craft it with iron ingots, a glass pane, redstone and a copper ingot. The tab also contains Empty Photographs, the Photo Album, and the Photo Display Stand. Craft four Empty Photographs from four paper and one ink sac. The Photo Album and Photo Display Stand also have crafting recipes.
2. Keep Empty Photographs in your inventory and Right-click with the Camera to photograph a building, terrain, or animals. In Survival, each successful capture uses one blank and one of the Camera's 100 shots. Creative mode uses neither. Camera clicks on blocks and living entities take a picture instead of interacting with them.
3. Hold the Photograph in your hand to see the real-time perspective overlay appear on your screen.
4. Line up your view and Right-click to stamp the photograph into reality!
5. Made a mistake? Press Z anytime to undo and recover your photo.

---

## Features

### Reshape Your World
Snap a photo of a bridge, rotate around, and stamp it across a canyon. What you see through the photograph becomes real blocks in the world.

### Capture Living Mobs
Living mobs and armor stands in your photo frame are captured and recreated when you stamp the photo into the world. Their equipment, held items, health, effects, variants and inventories are copied. Villagers retain their profession, level, experience and trades. Tamed companions retain their owner and other saved state even if the original later dies. Every recreated mob receives a fresh entity UUID. Position, velocity, leash, AI memories tied to old locations and nested passengers are not copied. In survival, each Photograph can be placed once unless you undo that placement.

### Perspective Zoom
Hold Ctrl or Shift and scroll your mouse wheel (or use the [ and ] bracket keys) while holding a Photograph to adjust its placement distance. At 0.5x the whole captured structure moves farther away without stretching gaps between blocks. Far placement is additive to avoid carving a large area. At 2x or 3x the blocks appear closer. Each captured block remains one real Minecraft block.

While holding the Camera, use Ctrl + scroll to zoom the viewfinder from 1x up to the configured maximum before taking a photograph. The captured zoom is stored with the Photograph, so the preview and placed blocks use the same target distance.

### Undo Recent Placement
Mistakes happen. Press Z to reverse your last placement while you are in the same dimension. Undo loads the placement chunks if you moved away. Opening a placed door, changing a block's state, or losing a projected block to placement physics does not prevent Undo. Undo still refuses when a placed position now contains a different non-air block. Blocks return to their previous state, spawned entities disappear, and a consumed Photograph returns to your inventory. Undo history is cleared when the server stops.

### Photo Album
Right-click the Photo Album item to open a two-page gallery. Store up to 18 photographs on the left page and inspect full-size live photo previews with capture dates, block counts, and mob counts on the right page.

### Photo Display Stand
Place a wooden display stand in your world to show your best shots in 3D. Right-click with a photo to mount it, or right-click with an empty hand to take it back. The stand is decorative; place the Photograph itself to materialize it.

Captured chests and mobs retain their stored or equipped items when placed from a Photograph. This intentionally duplicates those items.

---

## Controls

| Action | Control |
| --- | --- |
| Take photograph | Right-click with Camera |
| Stamp photo into world | Right-click with Photograph |
| Additive stamp (no carving) | Shift + Right-click with Photograph |
| Snap angle lock (90 degrees) | Hold Shift while holding Photograph |
| Zoom Camera before capture | Ctrl + Mouse Scroll with Camera |
| Change Photograph placement distance | Ctrl / Shift + Mouse Scroll (or [ and ] keys) |
| Undo last placement | Press Z |
| Open RealmFinder Settings | Press O (rebindable in Controls) |
| Open Photo Album | Right-click with Photo Album |
| Mount / take photo on easel | Right-click Photo Stand |

---

## Requirements

| Minecraft | Loader | Java | Extra dependency |
| --- | --- | --- | --- |
| 1.21.1 | Fabric Loader 0.19.5+ | 21 | Fabric API |
| 1.21.1 | NeoForge 21.1.251 | 21 | None |
| 1.20.1 | Fabric Loader 0.16.9+ | 17 | Fabric API 0.92.2+1.20.1 |
| 1.20.1 | Forge 47.3.0+ | 17 | None |

Use the JAR matching both your Minecraft version and loader. The 1.20.1 ports still require gameplay testing; see [port status](docs/PORT_STATUS.md).

## Modpack configuration

Press **O** in a world to open RealmFinder Settings. Hover over any option for a short explanation. Gameplay rules are saved in `config/realmfinder.json` and apply to the whole server; only operators can edit them in multiplayer. Personal settings are saved in `config/realmfinder-client.json`. Existing config files still load, and values outside the supported ranges are clamped. The old `maxCameraZoom` field in `realmfinder.json` is read once when creating client preferences; camera zoom is now controlled by the client file.
