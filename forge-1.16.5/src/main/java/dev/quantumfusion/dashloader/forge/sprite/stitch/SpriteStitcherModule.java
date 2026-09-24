package dev.quantumfusion.dashloader.forge.sprite.stitch;

import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Forge 1.16.5 port of modern {@code SpriteStitcherModule}
 * ({@code fabric-1.21.4}).
 *
 * <p>Same responsibilities: stage finished stitch runs on SAVE
 * ({@link #STITCHERS_SAVE}, one entry per atlas id), snapshot them into
 * {@link Data}, restore them into {@link #STITCHERS_LOAD} for
 * {@link DashTextureStitcher} to consume.
 *
  * <p>Hooks: {@code StitcherCaptureMixin} captures finished stitchers into
  * {@link #STITCHERS_SAVE} (keep-first) and {@code AtlasTextureStitchMixin}
  * swaps in a {@link DashTextureStitcher} on load when stitch parameters
  * match.
 * @author Malionaro
 */public final class SpriteStitcherModule {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-stitch");

    /** SAVE-stage: finished stitch runs keyed by atlas id. Mirrors modern {@code STITCHERS_SAVE}. */
    public static final Map<ResourceLocation, DashTextureStitcher.Data> STITCHERS_SAVE = new LinkedHashMap<>();
    /** LOAD-stage: exported stitch data keyed by atlas id. Mirrors modern {@code STITCHERS_LOAD}. */
    public static final Map<ResourceLocation, DashTextureStitcher.ExportedData> STITCHERS_LOAD = new LinkedHashMap<>();

    private SpriteStitcherModule() {
    }

    /** Gated on the Forge config equivalent of modern {@code Option.CACHE_SPRITE_STITCHING}. */
    public static boolean isActive() {
        return DashLoaderConfig.CACHE_STITCHING.get();
    }

    public static void reset() {
        STITCHERS_SAVE.clear();
        STITCHERS_LOAD.clear();
    }

    public static Data save() {
        Map<String, DashTextureStitcher.Data> out = new LinkedHashMap<>(STITCHERS_SAVE.size());
        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<ResourceLocation, DashTextureStitcher.Data> entry : STITCHERS_SAVE.entrySet()) {
            String key = entry.getKey().toString();
            // Same atlas can be stitched twice in one boot (double reload):
            // keep the first result, like modern.
            if (out.containsKey(key)) {
                duplicates.add(key);
                continue;
            }
            out.put(key, entry.getValue());
        }
        for (String dup : duplicates) {
            LOGGER.info("Duplicate stitcher {}, keeping first result.", dup);
        }
        LOGGER.info("Stitch snapshot: {} atlases.", out.size());
        return new Data(out);
    }

    public static void load(Data data) {
        STITCHERS_LOAD.clear();
        if (data == null || data.stitchers == null) {
            return;
        }
        for (Map.Entry<String, DashTextureStitcher.Data> entry : data.stitchers.entrySet()) {
            try {
                STITCHERS_LOAD.put(new ResourceLocation(entry.getKey()), entry.getValue().export());
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping unrestorable cached stitcher {}: {}", entry.getKey(), e.getMessage());
            }
        }
        LOGGER.info("Stitch restore: {} atlases.", STITCHERS_LOAD.size());
    }

    /** Empty snapshot used when the module is disabled (keeps the JSON shape stable). */
    public static Data emptyData() {
        return new Data(new LinkedHashMap<String, DashTextureStitcher.Data>());
    }

    /** Snapshot data. Keys are atlas id strings. */
    public static final class Data {
        public final Map<String, DashTextureStitcher.Data> stitchers;

        public Data(Map<String, DashTextureStitcher.Data> stitchers) {
            this.stitchers = stitchers;
        }
    }
}
