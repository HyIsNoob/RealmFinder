# RealmFinder stabilization plan

Approved direction: fix correctness in the capture → Photograph → placement → Undo loop before adding content. Preserve the existing mechanic and the current checkout.

1. Add testable limits for client pose, camera parameters and operation sizes; apply them at both server packet entry points.
2. Validate the snapshot's compressed data and decoded shape before any world mutation. Reject malformed, oversized, empty or unsupported photographs.
3. Make placement report success/failure, preflight affected chunks and size, and use the same checks for hand placement and Photo Stand.
4. Identify the clicked hand and Photograph in the stamp packet. Consume one Photograph only after successful placement; record exactly that one item for Undo.
5. Make Undo dimension-aware and conflict-aware. Stop deleting unrelated item entities. Ensure Photo Stand consumes its mounted Photograph when it stamps.
6. Add a truthful placement preview/validation signal based on the transformed snapshot, while keeping the captured PNG as the photograph's visual surface.
7. Run focused regression tests, the full test/build, and document remaining in-game checks.

Implementation status (2026-09-23): packet limits and throttling, snapshot bounds/versioning, shared placement geometry, world outline preview, hand/ID binding, consumption/Undo ownership, multiplayer thumbnails, and Album/Stand fixes are implemented. `test build` passes. An initial `runClient` stalled during Loom configuration and was stopped. A second `runClient --offline` reached Minecraft 1.21.1, logged successful RealmFinder initialization and resource reload, then exited with code 0. Remaining runtime gates: exercise a disposable world and a dedicated server with two clients, and verify rendering, chunk boundaries, Undo conflicts and image sync. The automated tests and client startup do not simulate these gameplay paths.

Implementation will be incremental, with each code change preceded by a focused failing test where a test harness can exercise the behavior.
