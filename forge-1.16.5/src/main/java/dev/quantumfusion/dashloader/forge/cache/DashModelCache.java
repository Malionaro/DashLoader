package dev.quantumfusion.dashloader.forge.cache;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.Map;

/**
 * WIP vertical slice: model-cache data holder (stub, no serialisation yet).
 *
 * <p>Era reference: {@code def-fabric-1.17/.../data/serialize/mapping/DashModelData.java}
 * plus {@code BakedModelManagerOverride.apply} (lines 77-112), which installs
 * {@code atlasManager}, {@code models} and {@code stateLookup} from
 * {@code VanillaData}. The 1.16.5 Forge counterparts, verified via
 * {@code javap} against the mapped snapshot jar, are:
 * <ul>
 *   <li>{@code ModelBakery#getTopBakedModels()} -&gt;
 *       {@code Map<ResourceLocation, IBakedModel>} (era:
 *       {@code ModelLoader.getBakedModelMap()})</li>
 *   <li>{@code ModelBakery#getStateModelIds()} -&gt;
 *       {@code Object2IntMap<BlockState>} (era:
 *       {@code ModelLoader.getStateLookup()})</li>
 *   <li>{@code ModelBakery#uploadTextures(TextureManager, IProfiler)} -&gt;
 *       {@code SpriteMap} (era: {@code ModelLoader.upload(...)} -&gt;
 *       {@code SpriteAtlasManager})</li>
 *   <li>{@code ModelBakery#MODEL_MISSING} (era:
 *       {@code ModelLoader.MISSING_ID})</li>
 * </ul>
 *
 * <p>What is missing for a working slice (all out of scope for this spike):
 * <ol>
 *   <li>Hyphen-era serialisation of {@code IBakedModel} implementations
 *       ({@code SimpleBakedModel} vs era {@code BasicBakedModel},
 *       {@code MultipartBakedModel}, {@code WeightedBakedModel} — same class
 *       tree in 1.16.5, different member names).</li>
 *   <li>{@code dashloader-core} 3.0-SNAPSHOT-era sources — the artifact the
 *       era build resolves ({@code gradle.properties} at the base commit)
 *       was never published to a reachable maven; without it there is no
 *       cache format, no {@code DashMappings}, no bootstrap.</li>
 *   <li>Font slice is harder: 1.16.5 uses the legacy {@code FontRenderer}
 *       (yarn {@code TextRenderer} did not exist yet); era
 *       {@code DashTrueTypeFont/DashUnicodeFont} target glyph classes with
 *       no 1:1 counterpart.</li>
 * </ol>
 */
public final class DashModelCache {
    private DashModelCache() {
    }

    /**
     * @return cached baked models keyed by model id, or an empty map when no
     * cache is present (always empty in this skeleton).
     */
    public static Map<ResourceLocation, IBakedModel> getModels() {
        // WIP: deserialise DashModelData here once Hyphen works on Java 8.
        return Collections.emptyMap();
    }

    /**
     * @return the bakery that produced the vanilla models (pass-through for
     * now; the era code nulled the loader when a cache was loaded).
     */
    public static ModelBakery orVanilla(ModelBakery bakery) {
        return bakery;
    }
}
