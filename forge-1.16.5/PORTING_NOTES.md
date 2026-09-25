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
| Multipart selectors staged | unbaked `Selector` + owner staged at `bakeModel` RETURN + apply-TAIL bakery fallback | `mixin/MultipartBakeMixin.java` (`SAVE_MULTIPART`), `mixin/ModelManagerCacheMixin.java` (`stageMultipartFallback`), `mixin/accessor/MultipartAccessor.java` |
| Item overrides preserved | predicate maps + target model ids, rebuilt via `RestoredItemOverrideList`, per-override skip | `model/DashBasicBakedModel.java` (`buildOverrides`), `model/DashItemOverride.java`, `model/RestoredItemOverrideList.java`, `mixin/accessor/ItemOverrideAccessor.java`, `mixin/accessor/ItemOverrideListAccessor.java` |
| Stitch source dims | SOURCE dims stored + compared, PACKED dims kept for loader | `sprite/stitch/DashTextureSlot.java`, `sprite/stitch/DashTextureStitcher.java` (`Slot`, `Data.capture/export`, `addSprite`) |
| Config screen | toggle buttons bound to `ForgeConfigSpec`, incl. master switch | `ui/DashConfigScreen.java`, `DashLoaderConfig.java` |
| Splashes served | LOAD replaces prepared list with cached texts | `mixin/SplashesCacheMixin.java` |
| Models installed | TAIL of `ModelManager.apply` overwrites `modelRegistry` | `mixin/ModelManagerCacheMixin.java` (`dashloader$installAndSave`) |
| Stitch packing reused | `NEW Stitcher` redirect → `DashTextureStitcher` | `mixin/AtlasTextureStitchMixin.java` (`dashloader$newStitcher`) |

## Partial skips (mappable in principle, simplified — each logged, none silent)

1. **Multipart selectors**: unbaked `Selector` condition trees (vanilla
   `AndCondition` / `OrCondition` / `PropertyValueCondition` / `TRUE` /
   `FALSE`, read via the condition accessor mixins) are staged at bake time
   (`mixin/MultipartBakeMixin.java` → `ModelModule.SAVE_MULTIPART`) with an
   apply-TAIL bakery fallback (`ModelManagerCacheMixin#stageMultipartFallback`
   via `MultipartAccessor#getStateContainer` + `Multipart#getSelectors`, for
   boots where the cache was IDLE during baking) and serialized as tagged
   JSON (`cache/CacheGson.java` selector adapter); predicates rebuild via
   `getPredicate` on LOAD. Expected log on vanilla SAVE:
   `Model snapshot: 9520 basic, 0 weighted, <multipart> multipart`
   with multipart greater than 0 (fences/walls/etc). Part models that are not
   staged top models are snapshotted inline under synthetic
   `/__dashloader_part_<n>` ids (skipped at registry install). Remaining
   vanilla fallbacks (each logged): models without staged selectors (e.g.
   replaced post-bake), modded `ICondition` implementations, uncacheable
   part models.
2. **Weighted models**: round-trip retained (`WeightedBakedModelAccessor` +
   `WeightedModelAccessor` + `WeightedRandomItemAccessor`, reflective
   `WeightedModel` rebuild in `DashWeightedBakedModel#toVanilla`). Vanilla
   1.16.5 has no weighted *top* models with default packs (weights live
   inside `VariantList` baking), so SAVE logs
   `No weighted models found (vanilla has none as top models) — weighted
   round-trip retained.` and moves on.
3. **Item overrides**: `DashBasicBakedModel` stores `List<DashItemOverride>`
   (predicate id strings + target model-id strings, resolved through staged
   top models). SAVE skips override targets that are not staged top models
   with a warning (`Skipping override target not in cache`); LOAD rebuilds via
   `RestoredItemOverrideList` (vanilla threshold matching replicated with
   public `ItemModelsProperties#func_239417_a_`), skipping unresolvable
   targets with `Skipping unrestorable override target` /
   `Restored x/y item overrides`. Basics build in two passes so forward
   references (bow pulling variants) resolve.
4. **Sprite stitch source dims**: `DashTextureSlot` + `DashTextureStitcher.Slot`
   carry PACKED dims (for `ISpriteLoader#load`) AND SOURCE dims
   (`Info#getSpriteWidth/Height`, for the changed-dimensions check).
   `Data.capture` stores both; `addSprite` compares source-vs-source so
   animated entity strips (packed height = source x frames) no longer force a
   per-boot `changed dimensions ... falling back` for unchanged packs. Old
   4-field caches deserialize with source 0 and keep legacy packed comparison.
   Expected: no `changed dimensions` fallback on second boot with unchanged packs.
5. **Sprite pixel serving**: cached stitch *positions* are reused, but pixel
   decoding still runs vanilla — there is no clean 1.16.5 hook into the
   private `getStitchedSprites` path
   (`mixin/AtlasTextureStitchMixin.java:53-57`). Sprite JSON round-trips and
   populates `SpriteContentModule.LOAD` for future work.
3. **Animation metadata**: restored sprites are static; rebuilding
   `AnimationMetadataSection` from frame pairs is unwired
   (`sprite/DashSpriteContents.java:32-41`, `75-79`).
4. **`ItemCameraTransforms`**: carried field-by-field as float triples via the
   Gson adapter (`cache/CacheGson.java`).
5. **1.21-only quad data**: `lightEmission` does not exist on the 1.16.5
   `BakedQuad` constructor and is dropped
   (`model/DashBakedQuad.java:27-37`).

## PERMANENT skips (genuinely unmappable — documented here AND in code)

1. **Font caching** (`font/FontModule.java`): 1.16.5 DOES have a provider
   system (`Font{glyphProviders, textures}`, `IGlyphProvider`, `FontTexture`),
   but it is GPU-only and uncacheable with minimal risk (javap-verified):
   `FontTexture` holds only `textureLocation`/render types/`colored`/packing
   `Entry` (no `NativeImage` field); glyph pixels upload straight to GL via
   `IGlyphInfo#uploadGlyph(x, y)`; the only readback
   (`NativeImage#downloadFromTexture`) needs a live GL context that the
   reload-listener LOAD path has none of safely, and repacking is
   allocation-order dependent (UV mismatches = text corruption). The
   task's "ascii + unicode page NativeImages" matches pre-1.13, not 1.16.5.
   `FontResourceManager` builds `Font`s eagerly from TTF streams with no
   cacheable intermediate. Always vanilla fallback.
2. **GPU atlas / shader caches** (modern `Option.CACHE_ATLASES`,
   `CACHE_SHADER`): 1.16.5 `SpriteMap` upload and GL shader programs are GPU
   objects with no serializable form on this toolchain. Not implemented.
3. **Hyphen binary format**: `dashloader-core` 3.0-SNAPSHOT was never
   published to a reachable Maven and Hyphen needs newer Java; the cache
   format is Gson JSON instead (same `Data` POJO shapes, different bytes —
   caches are NOT interchangeable with modern).

## Production refmap / SRG — static verification (2026-09-25)

All mixins use `remap = false` (dev-correct: the FG4 dev workspace is
MCP-named). Every `@Shadow`/`@Accessor` target and every `method=` string was
`javap`-checked against BOTH jars:

- dev/mapped: `forge-1.16.5-36.2.42_mapped_snapshot_20210309-1.16.5.jar` (MCP names — all match)
- production: `forge-1.16.5-36.2.42-srg.jar` (SRG names — dev MCP strings do NOT match at runtime)

Result: dev workspace verified; production (SRG runtime) mixin application
would fail for every MCP-named target below without a working refmap. The
Mixin AP runs with the `convertTsrgToSrg` plumbing in `build.gradle`, but
because every mixin declares `remap = false` it intentionally emits an
almost-empty refmap (`build/mixin/dashloader-1.16.5-refmap.json` carries only
class names, `build/mixin/out.srg` empty). MixinGradle is unresolvable in
2026 (gone from Sponge Maven, absent from Central), so the jar is
dev/instance-ready, not production-verified. No code change can fix this
without the refmap pipeline; the table is the strongest static check possible.

### Field targets (MCP dev name → SRG production name)

| Target class | MCP (`@Shadow`/`@Accessor`) | SRG (`-srg.jar` javap) | Status |
|---|---|---|---|
| `ModelManager` | `modelRegistry` | `field_174958_a` | dev OK / prod needs refmap |
| `Multipart` (unbaked) | `selectors` | `field_188139_a` | dev OK / prod needs refmap |
| `Multipart` (unbaked) | `stateContainer` | `field_188140_b` | dev OK / prod needs refmap |
| `MultipartBakedModel` | `selectors` | `field_188626_f` (task acceptance field) | dev OK / prod needs refmap |
| `Selector` | `condition` | `field_188167_a` | dev OK / prod needs refmap |
| `AndCondition` | `conditions` | `field_188121_c` | dev OK / prod needs refmap |
| `OrCondition` | `conditions` | `field_188127_c` | dev OK / prod needs refmap |
| `PropertyValueCondition` | `key` / `value` | `field_188125_d` / `field_188126_e` | dev OK / prod needs refmap |
| `WeightedBakedModel` | `models` | `field_177565_b` | dev OK / prod needs refmap |
| `WeightedBakedModel$WeightedModel` | `model` | `field_185281_b` | dev OK / prod needs refmap |
| `WeightedRandom$Item` | `itemWeight` | `field_76292_a` (protected in SRG, public in MCP) | dev OK / prod needs refmap |
| `TextureAtlasSprite` | `frames` | `field_195670_c` | dev OK / prod needs refmap |
| `AtlasTexture$SheetData` | `sprites` / `width` / `height` / `mipmapLevel` | `field_217808_d` / `field_217806_b` / `field_217807_c` / `field_229224_d_` | dev OK / prod needs refmap |
| `Stitcher` | `maxWidth` / `maxHeight` / `mipmapLevelStitcher` | `field_94316_e` / `field_94313_f` / `field_147971_a` (order by ctor assign order; javap-verified set) | dev OK / prod needs refmap |
| `ResourceLoadProgressGui` | `mc` / `asyncReloader` / `fadeOutStart` | `field_212974_b` / `field_212975_c` / `field_212979_g` (`fadeOutStart` is the long set with `-1L` check; `field_212980_h` is the other long) | dev OK / prod needs refmap |
| `ItemOverride` | `mapResourceValues` | `field_188029_b` (`location` → `field_188028_a`) | dev OK / prod needs refmap |
| `ItemOverrideList` | `overrideBakedModels` (`overrides` → `field_188023_b`) | `field_209582_c` | dev OK / prod needs refmap |

### Method targets (`method=` strings → SRG)

| Mixin | MCP `method=` string | SRG (`-srg.jar` javap) | Status |
|---|---|---|---|
| `MultipartBakeMixin` | `bakeModel` (any desc) | `func_225613_a_(ModelBakery, Function, IModelTransform, ResourceLocation)` | dev OK / prod needs refmap |
| `ModelManagerCacheMixin` | `prepare(... )ModelBakery` | `func_212854_a_(IResourceManager, IProfiler)` | dev OK / prod needs refmap |
| `ModelManagerCacheMixin` | `apply(ModelBakery, ... )V` | `func_212853_a_(ModelBakery, IResourceManager, IProfiler)` | dev OK / prod needs refmap |
| `AtlasTextureStitchMixin` | `<init>` | `<init>(ResourceLocation)` (both jars) | OK both (ctor name stable) |
| `AtlasTextureStitchMixin` | `stitch` (+ `NEW Stitcher` redirect) | `func_229220_a_(IResourceManager, Stream, IProfiler, int)`; `Stitcher.<init>(III)` stable | dev OK / prod needs refmap for `stitch` |
| `StitcherCaptureMixin` | `doStitch` | `func_94305_f()` | dev OK / prod needs refmap |
| `ResourceLoadProgressGuiMixin` | `render(MatrixStack;IIF)V` | `func_230430_a_(MatrixStack, int, int, float)` | dev OK / prod needs refmap |
| `SplashesCacheMixin` | `apply(List;... )V` | `func_212853_a_(List, IResourceManager, IProfiler)` | dev OK / prod needs refmap |
| `DashTextureStitcher` overrides | `addSprite` / `doStitch` / `getCurrentWidth` / `getCurrentHeight` / `getStitchSlots` | `func_229211_a_` / `func_94305_f` / `func_110935_a` / `func_110936_b` / `func_229209_a_` | dev OK / prod needs refmap (subclass overrides resolve via refmap) |

Touch points: `mixin/*`, `mixin/accessor/*`, `build.gradle`
(`convertTsrgToSrg`, `JavaCompile` AP args).

## Remaining gaps (with file:line)

- Sprite pixels decode vanilla on LOAD (positions cached):
  `src/main/java/dev/quantumfusion/dashloader/forge/mixin/AtlasTextureStitchMixin.java`
- Restored sprites are non-animated:
  `src/main/java/dev/quantumfusion/dashloader/forge/sprite/DashSpriteContents.java`
- Production SRG remapping unverified at runtime (statically audited above,
  dev-only `remap = false`): `mixin/*`, `build.gradle:135-179`
