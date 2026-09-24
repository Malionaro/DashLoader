package dev.quantumfusion.dashloader.forge.model;

import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import net.minecraft.client.renderer.model.SimpleBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Forge 1.16.5 port of modern {@code ModelModule}
 * ({@code fabric-1.21.4}).
 *
 * <p>What is kept from modern:
 * <ul>
 *   <li>Staging maps for the SAVE pass (populated from
 *       {@link ModelBakery#getTopBakedModels()}, the 1.16.5 counterpart of
 *       modern {@code ModelLoader} baked-model maps — see
 *       {@code DashModelCache} for the full bakery mapping table).</li>
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
 *   <li>Installing restored models back into the bakery
 *       ({@code ModelBakery} fields are private) needs a
 *       {@code ModelManager.apply} hook — hook point documented in
 *       {@code ModelManagerCacheMixin}; not wired yet.</li>
 * </ul>
 */
public final class ModelModule {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-model");

    /** SAVE-stage: top baked models keyed by model id. Mirrors modern {@code BAKED_MODEL_PARTS}. */
    public static final Map<ResourceLocation, IBakedModel> SAVE_TOP_MODELS = new LinkedHashMap<>();

    private ModelModule() {
    }

    /** Gated on the Forge config equivalent of modern {@code Option.CACHE_MODEL_LOADER}. */
    public static boolean isActive() {
        return DashLoaderConfig.CACHE_MODELS.get();
    }

    public static void reset() {
        SAVE_TOP_MODELS.clear();
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
