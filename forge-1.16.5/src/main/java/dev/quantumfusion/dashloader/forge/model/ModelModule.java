package dev.quantumfusion.dashloader.forge.model;

import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import net.minecraft.client.renderer.model.SimpleBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Forge 1.16.5 port of modern {@code ModelModule}
 * ({@code fabric-1.21.4}).
 *
 * <p>What is kept from modern:
 * <ul>
  *   <li>Staging maps for the SAVE pass (populated from
  *       {@link ModelBakery#getTopBakedModels()}, the 1.16.5 counterpart of
  *       modern {@code ModelLoader} baked-model maps).</li>
 *   <li>{@code instanceof} dispatch over the three cacheable
 *       implementations (basic / multipart / weighted); anything else
 *       (modded models) lands on the missing list with a warning and falls
 *       back to vanilla — same policy as modern.</li>
 *   <li>A {@link Data} snapshot object holding per-kind Dash models plus
 *       missing-model ids.</li>
 * </ul>
 *
 * <p>What is simplified (Hyphen-out, documented):
 * <ul>
 *   <li>Modern splits unbaked parts vs block/item models with bake-setting
 *       variants; this port snapshots the <em>baked top models</em> keyed by
 *       model id, which is the 1.16.5-visible unit
 *       ({@code ModelBakery#getTopBakedModels()}).</li>
 *   <li>Model cross-references use plain {@code String} ids (registry id
 *       strings) instead of Hyphen int pointers. Multipart part models and
 *       weighted entry models that are themselves top-level snapshots are
 *       resolved through the same maps; nested non-top-level models are
 *       reported missing (vanilla fallback).</li>
 *   <li>Multipart unbaked {@code Selector} lists must be supplied by the
 *       caller of {@link Data} creation (modern keeps them in
 *       {@code MULTIPART_PREDICATES}; wiring that staging to the 1.16.5
 *       baking path is TODO).</li>
  *   <li>Installing restored models back into the bakery happens in
  *       {@code ModelManagerCacheMixin} (TAIL of {@code apply} overwrites
  *       {@code modelRegistry} entries with {@link #buildLoadedModels}).</li>
 * </ul>
 */
public final class ModelModule {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-model");

    /** SAVE-stage: top baked models keyed by model id. Mirrors modern {@code BAKED_MODEL_PARTS}. */
    public static final Map<ResourceLocation, IBakedModel> SAVE_TOP_MODELS = new LinkedHashMap<>();

    /** LOAD-stage: deserialized snapshot waiting to be installed (see {@link #buildLoadedModels}). */
    private static volatile Data LOAD_DATA;

    private ModelModule() {
    }

    /** Gated on the Forge config equivalent of modern {@code Option.CACHE_MODEL_LOADER}. */
    public static boolean isActive() {
        return DashLoaderConfig.CACHE_MODELS.get();
    }

    public static void reset() {
        SAVE_TOP_MODELS.clear();
    }

    /** Clears the LOAD snapshot to free memory (called on cache reset). */
    public static void clearLoad() {
        LOAD_DATA = null;
    }

    public static boolean hasLoad() {
        Data data = LOAD_DATA;
        return data != null
                && data.basicModels != null
                && (!data.basicModels.isEmpty()
                        || (data.weightedModels != null && !data.weightedModels.isEmpty()));
    }

    /** Empty snapshot used when the module is disabled (keeps the JSON shape stable). */
    public static Data emptyData() {
        return new Data(new LinkedHashMap<String, DashBasicBakedModel>(),
                new LinkedHashMap<String, DashMultipartBakedModel>(),
                new LinkedHashMap<String, DashWeightedBakedModel>(),
                new ArrayList<String>());
    }

    /**
     * Snapshot staged top models into a {@link Data} object.
     * Multipart part models resolve through the staged top models themselves.
     */
    public static Data save() {
        Map<String, DashBasicBakedModel> basic = new LinkedHashMap<>();
        Map<String, DashMultipartBakedModel> multipart = new LinkedHashMap<>();
        Map<String, DashWeightedBakedModel> weighted = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        // Nested model-id strings for weighted/multipart part references:
        // every staged top model is addressable by its id string.
        final Map<IBakedModel, String> idsByModel = new java.util.IdentityHashMap<>();
        for (Map.Entry<ResourceLocation, IBakedModel> entry : SAVE_TOP_MODELS.entrySet()) {
            idsByModel.put(entry.getValue(), entry.getKey().toString());
        }
        java.util.function.Function<IBakedModel, String> modelIds = part -> {
            String id = idsByModel.get(part);
            if (id == null) {
                throw new IllegalArgumentException("Nested model is not a staged top model: "
                        + part.getClass().getName());
            }
            return id;
        };

        for (Map.Entry<ResourceLocation, IBakedModel> entry : SAVE_TOP_MODELS.entrySet()) {
            String key = entry.getKey().toString();
            IBakedModel model = entry.getValue();
            if (model == null) {
                continue;
            }
            try {
                if (model instanceof SimpleBakedModel) {
                    basic.put(key, DashBasicBakedModel.toDash((SimpleBakedModel) model));
                } else if (model instanceof WeightedBakedModel) {
                    weighted.put(key, DashWeightedBakedModel.toDash((WeightedBakedModel) model, modelIds));
                } else if (model instanceof MultipartBakedModel) {
                    // Unbaked selectors are not staged yet (see class javadoc);
                    // record as missing so vanilla baking still covers it.
                    missing.add(key);
                    LOGGER.debug("Multipart model {} needs staged selectors; using vanilla fallback for now.", key);
                } else {
                    missing.add(key);
                    if (missing.size() <= 3) {
                        LOGGER.warn("Skipping uncacheable model {} ({}).", key, model.getClass().getName());
                    }
                }
            } catch (RuntimeException e) {
                missing.add(key);
                if (missing.size() <= 3) {
                    LOGGER.warn("Skipping uncacheable model {} ({}): {}", key, model.getClass().getName(), e.getMessage());
                }
            }
        }

        LOGGER.info("Model snapshot: {} basic, {} weighted, {} multipart-deferred, {} missing.",
                basic.size(), weighted.size(), multipart.size(), missing.size());
        return new Data(basic, multipart, weighted, missing);
    }

    /**
     * Stores the deserialized snapshot for later installation.
     * Mirrors modern {@code ModelModule#load} (which fills
     * {@code BLOCK_STATE_UNBAKED}); installation happens in
     * {@code ModelManagerCacheMixin} once a sprite lookup is available.
     */
    public static void load(Data data) {
        if (data == null) {
            return;
        }
        LOAD_DATA = data;
        int basic = data.basicModels == null ? 0 : data.basicModels.size();
        int weighted = data.weightedModels == null ? 0 : data.weightedModels.size();
        LOGGER.info("Model restore staged: {} basic, {} weighted.", basic, weighted);
    }

    /**
     * Rebuilds vanilla models from the LOAD snapshot.
     *
     * <p>Per-entry skip resilience (modern parity): any model that fails to
     * restore is skipped with a warning and left for vanilla baking instead
     * of aborting the whole install. Basic models are restored first so
     * weighted entries referencing them resolve; unresolvable references are
     * skipped. Multipart entries are always skipped (selectors are a
     * documented partial skip — see {@code PORTING_NOTES.md}).
     */
    public static Map<ResourceLocation, IBakedModel> buildLoadedModels(
            Function<ResourceLocation, TextureAtlasSprite> spriteLookup) {
        Data data = LOAD_DATA;
        if (data == null) {
            return Collections.emptyMap();
        }
        Map<ResourceLocation, IBakedModel> out = new LinkedHashMap<>();
        if (data.basicModels != null) {
            for (Map.Entry<String, DashBasicBakedModel> entry : data.basicModels.entrySet()) {
                try {
                    out.put(new ResourceLocation(entry.getKey()),
                            entry.getValue().toVanilla(spriteLookup));
                } catch (RuntimeException e) {
                    LOGGER.warn("Skipping unrestorable cached model {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
        if (data.weightedModels != null) {
            for (Map.Entry<String, DashWeightedBakedModel> entry : data.weightedModels.entrySet()) {
                try {
                    final Map<ResourceLocation, IBakedModel> built = out;
                    WeightedBakedModel model = entry.getValue().toVanilla(key -> {
                        IBakedModel part = built.get(new ResourceLocation(key));
                        if (part == null) {
                            throw new IllegalArgumentException("Referenced model not restored: " + key);
                        }
                        return part;
                    });
                    out.put(new ResourceLocation(entry.getKey()), model);
                } catch (RuntimeException e) {
                    LOGGER.warn("Skipping unrestorable cached model {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
        if (data.multipartModels != null && !data.multipartModels.isEmpty()) {
            LOGGER.warn("Skipping {} cached multipart models (selector restore is a documented partial skip).",
                    data.multipartModels.size());
        }
        LOGGER.info("Model restore built: {} models.", out.size());
        return out;
    }

    /** Snapshot data. Keys are model id strings; values are Dash models. */
    public static final class Data {
        public final Map<String, DashBasicBakedModel> basicModels;
        public final Map<String, DashMultipartBakedModel> multipartModels;
        public final Map<String, DashWeightedBakedModel> weightedModels;
        public final List<String> missingModels;

        public Data(Map<String, DashBasicBakedModel> basicModels,
                Map<String, DashMultipartBakedModel> multipartModels,
                Map<String, DashWeightedBakedModel> weightedModels,
                List<String> missingModels) {
            this.basicModels = basicModels;
            this.multipartModels = multipartModels;
            this.weightedModels = weightedModels;
            this.missingModels = missingModels;
        }
    }
}
