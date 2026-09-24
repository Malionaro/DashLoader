# DashLoader Forge 1.16.5 — Porting Notes (vs `fabric-26.3`)

Reference for every behavior below: branch `fabric-26.3`. MCP names were
verified per-symbol with `javap` against
`forge-1.16.5-36.2.42_mapped_snapshot_20210309-1.16.5.jar`.

Mod version: `5.1.0-beta.9.1-1.16.5` (same beta line as the other versions).

## Cache layout (parity: equivalent by design)

Modern: `./dashloader-cache/client/<MOD_HASH>/<packHash>/` with Hyphen
binary fragments. This port: `<gamedir>/dashloader-cache/client/<MOD_HASH>/<packHash>/`
(`FMLPaths.GAMEDIR` is the Forge equivalent of the Fabric game-dir lookup)
with Gson JSON files (`models.json`, `sprites.json`, `stitch.json`,
`splashes.json`, `cache-info.json` sidecar). The JSON-vs-binary format
difference is intentional — Hyphen cannot come up on this toolchain (see
permanent skips). Hashes match modern semantics:

- `MOD_HASH`: MD5 (uppercase) of the index-sorted `modId&version` list —
  `cache/DashCacheBackend.java` (`computeModHash`), modern `DashLoader#MOD_HASH`.
- Pack hash: MD5 (uppercase) of `name/title/description` per enabled pack —
  `cache/DashCacheBackend.java` (`computePackHash`), modern
  `ReloadableResourceManagerImplMixin`. Simplification: the dedicated
  `server`-pack zip-path special case is not replicated; server packs still
  contribute name/title/description.

## Mappable behaviors — all ported

| Modern behavior | 1.16.5 equivalent | Location |
|---|---|---|
| Per-entry skip resilience (models) | `try/catch` per model, uncacheable → missing list, vanilla fallback | `model/ModelModule.java:100-152`, `model/ModelModule.java` (`buildLoadedModels`) |
| Per-entry skip resilience (sprites) | `try/catch` per sprite on save + load | `sprite/SpriteContentModule.java` (`save`/`load`) |
| Per-entry skip resilience (stitch/splash) | per-atlas `try/catch` on load; splash `null`-safe load | `sprite/stitch/SpriteStitcherModule.java` (`load`), `splash/SplashModule.java` (`load`) |
| Keep-first duplicates (stitch) | same atlas stitched twice keeps first | `sprite/stitch/SpriteStitcherModule.java` (`save`), `mixin/StitcherCaptureMixin.java` |
| Keep-first duplicates (sprites) | `SAVE.containsKey` guard at staging | `mixin/AtlasTextureStitchMixin.java` (`dashloader$stageSpriteContents`) |
| Stitch param validation | `matches(maxWidth, maxHeight, mipLevel)`; mismatch → vanilla re-stitch + log | `sprite/stitch/DashTextureStitcher.java` (`ExportedData#matches`), `mixin/AtlasTextureStitchMixin.java` (`dashloader$newStitcher`) |
| Missing-font fallback | every font falls back to vanilla (module always reports inactive) | `font/FontModule.java` |
| Toast during SAVE | background `dashloader-save` thread + `DashToast` progress | `mixin/ResourceLoadProgressGuiMixin.java` (`startBackgroundSave`) |
| Toast look | 200x40, animated lines, progress bar + glow, status + percent, fun fact, FAILED on crash, DONE 2s / CRASHED 10s hide | `ui/toast/DashToast.java`, `ui/DrawerUtil.java`, `ui/Color.java` |
| Loading overlay dismiss | `setLoadingGui(null)` at reload-complete + SAVE driver | `mixin/ResourceLoadProgressGuiMixin.java` |
| Multipart selectors staged | unbaked `Selector` + owner staged at `bakeModel` RETURN | `mixin/MultipartBakeMixin.java` (`SAVE_MULTIPART`) |
| Config screen | toggle buttons bound to `ForgeConfigSpec`, incl. master switch | `ui/DashConfigScreen.java`, `DashLoaderConfig.java` |
| Splashes served | LOAD replaces prepared list with cached texts | `mixin/SplashesCacheMixin.java` |
| Models installed | TAIL of `ModelManager.apply` overwrites `modelRegistry` | `mixin/ModelManagerCacheMixin.java` (`dashloader$installAndSave`) |
| Stitch packing reused | `NEW Stitcher` redirect → `DashTextureStitcher` | `mixin/AtlasTextureStitchMixin.java` (`dashloader$newStitcher`) |

## Partial skips (mappable in principle, simplified — each logged, none silent)

1. **Multipart selectors**: unbaked `Selector` condition trees (vanilla
   `AndCondition` / `OrCondition` / `PropertyValueCondition` / `TRUE` /
   `FALSE`, read via the condition accessor mixins) are staged at bake time
   (`mixin/MultipartBakeMixin.java` → `ModelModule.SAVE_MULTIPART`) and
   serialized as tagged JSON (`cache/CacheGson.java` selector adapter);
   predicates rebuild via `getPredicate` on LOAD. Part models that are not
   staged top models are snapshotted inline under synthetic
   `/__dashloader_part_<n>` ids (skipped at registry install). Remaining
   vanilla fallbacks (each logged): models without staged selectors (e.g.
   replaced post-bake), modded `ICondition` implementations, uncacheable
   part models.
2. **Sprite pixel serving**: cached stitch *positions* are reused, but pixel
   decoding still runs vanilla — there is no clean 1.16.5 hook into the
   private `getStitchedSprites` path
   (`mixin/AtlasTextureStitchMixin.java:53-57`). Sprite JSON round-trips and
   populates `SpriteContentModule.LOAD` for future work.
3. **Animation metadata**: restored sprites are static; rebuilding
   `AnimationMetadataSection` from frame pairs is unwired
   (`sprite/DashSpriteContents.java:32-41`, `75-79`).
4. **`ItemCameraTransforms`**: carried field-by-field as float triples via the
   Gson adapter (`cache/CacheGson.java`); `ItemOverrideList` resets to `EMPTY`
   on restore — overrides need `ModelBakery` context
   (`model/DashBasicBakedModel.java:44-51`, `:113`).
5. **1.21-only quad data**: `lightEmission` does not exist on the 1.16.5
   `BakedQuad` constructor and is dropped
   (`model/DashBakedQuad.java:27-37`).

## PERMANENT skips (genuinely unmappable — documented here AND in code)

1. **Font caching** (`font/FontModule.java:14-30`): 1.16.5 has no
   `FontStorage`/glyph providers — text goes through the legacy
   `FontRenderer` whose glyph cache is a GPU-uploaded atlas with no
   snapshottable CPU-side model, and `FontResourceManager` builds renderers
   eagerly with no cacheable intermediate. Always vanilla fallback.
2. **GPU atlas / shader caches** (modern `Option.CACHE_ATLASES`,
   `CACHE_SHADER`): 1.16.5 `SpriteMap` upload and GL shader programs are GPU
   objects with no serializable form on this toolchain. Not implemented.
3. **Hyphen binary format**: `dashloader-core` 3.0-SNAPSHOT was never
   published to a reachable Maven and Hyphen needs newer Java; the cache
   format is Gson JSON instead (same `Data` POJO shapes, different bytes —
   caches are NOT interchangeable with modern).

## Production refmap / SRG — what remains

All mixins use `remap = false` (dev-correct: the FG4 dev workspace is
MCP-named). The Mixin annotation processor runs with the `convertTsrgToSrg`
plumbing in `build.gradle`, but because every mixin declares
`remap = false` the AP intentionally emits an almost-empty refmap
(`build/mixin/dashloader-1.16.5-refmap.json` carries only class names, and
`build/mixin/out.srg` is empty). That is expected, not a crash — but it
means **production (SRG-runtime) mixin application is unverified**: method
and field targets (`ModelManager#modelRegistry`,
`Splashes#possibleSplashes`, `Stitcher#maxWidth/maxHeight/mipmapLevelStitcher`,
`TextureAtlasSprite#frames`, `AtlasTexture$SheetData` fields, and the
`DashTextureStitcher` overrides of `addSprite/doStitch/getCurrentWidth/...`)
would need SRG names plus a working refmap (MixinGradle is unresolvable in
2026: gone from Sponge Maven, absent from Central). Fixing that is heroics
beyond this port; the jar is dev/instance-ready, not production-verified.
Touch points: `mixin/ModelManagerCacheMixin.java:59-66`,
`mixin/AtlasTextureStitchMixin.java:59-72`,
`mixin/StitcherCaptureMixin.java`, `mixin/accessor/*`, `build.gradle`
(`convertTsrgToSrg`, `JavaCompile` AP args).

## Remaining gaps (with file:line)

- Sprite pixels decode vanilla on LOAD (positions cached):
  `src/main/java/dev/quantumfusion/dashloader/forge/mixin/AtlasTextureStitchMixin.java:53-57`
- Restored sprites are non-animated:
  `src/main/java/dev/quantumfusion/dashloader/forge/sprite/DashSpriteContents.java:75-79`
- Item overrides reset on restore:
  `src/main/java/dev/quantumfusion/dashloader/forge/model/DashBasicBakedModel.java:108-113`
- Production SRG remapping unverified (dev-only `remap = false`):
  `src/main/java/dev/quantumfusion/dashloader/forge/mixin/ModelManagerCacheMixin.java:59-66`,
  `build.gradle:131-175`
