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
 * <p>Hook TODO: in 1.16.5 stitching happens inside
 * {@code AtlasTexture#stitch}, which builds its own {@code Stitcher}
 * internally — capturing the finished stitcher (for
 * {@link DashTextureStitcher.Data#capture}) and swapping in a
 * {@link DashTextureStitcher} on load both need an
 * {@code AtlasTexture.stitch} hook, not wired yet. Until then
 * {@link #save()} snapshots whatever was staged (empty in this slice).
 */
public final class SpriteStitcherModule {
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
        for (Map.Entry<String, DashTextureStitcher.Data> entry : data.stitchers.entrySet()) {
            STITCHERS_LOAD.put(new ResourceLocation(entry.getKey()), entry.getValue().export());
        }
        LOGGER.info("Stitch restore: {} atlases.", STITCHERS_LOAD.size());
    }

    /** Snapshot data. Keys are atlas id strings. */
    public static final class Data {
        public final Map<String, DashTextureStitcher.Data> stitchers;

        public Data(Map<String, DashTextureStitcher.Data> stitchers) {
            this.stitchers = stitchers;
        }
    }
}
