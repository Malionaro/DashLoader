package dev.quantumfusion.dashloader.forge.sprite;

import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Forge 1.16.5 port of modern {@code SpriteContentModule}
 * ({@code fabric-1.21.4}).
 *
 * <p>Same responsibilities: stage per-sprite content on SAVE, snapshot it
 * into {@link Data}, restore it into the LOAD map. Keys are sprite ids;
 * values are {@link DashSpriteContents} (the 1.16.5 content-side snapshot —
 * see its javadoc for the {@code SpriteContents} adaptation).
 *
 * <p>Hook TODO: populating {@link #SAVE} needs a hook where 1.16.5 loads
 * sprite pixels (the {@code AtlasTexture} stitch path); not wired yet, so
 * {@link #save()} snapshots whatever was staged (empty in this slice).
 * Missing sprites fall back to vanilla loading — same policy as modern.
 */
public final class SpriteContentModule {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-sprite");

    /** SAVE-stage: sprite contents keyed by sprite id. Mirrors modern {@code SOURCE}. */
    public static final Map<ResourceLocation, DashSpriteContents> SAVE = new LinkedHashMap<>();
    /** LOAD-stage: restored contents for the stitch path to consume. */
    public static final Map<ResourceLocation, DashSpriteContents> LOAD = new LinkedHashMap<>();

    private SpriteContentModule() {
    }

    /** Gated on the Forge config equivalent of modern {@code Option.CACHE_SPRITE_CONTENT}. */
    public static boolean isActive() {
        return DashLoaderConfig.CACHE_SPRITES.get();
    }

    public static void reset() {
        SAVE.clear();
        LOAD.clear();
    }

    public static Data save() {
        Map<String, DashSpriteContents> out = new LinkedHashMap<>(SAVE.size());
        List<String> skipped = new ArrayList<>();
        for (Map.Entry<ResourceLocation, DashSpriteContents> entry : SAVE.entrySet()) {
            if (entry.getValue() != null) {
                out.put(entry.getKey().toString(), entry.getValue());
            } else {
                skipped.add(entry.getKey().toString());
            }
        }
        if (!skipped.isEmpty()) {
            LOGGER.warn("Skipped {} null sprite contents.", skipped.size());
        }
        LOGGER.info("Sprite content snapshot: {} sprites.", out.size());
        return new Data(out);
    }

    public static void load(Data data) {
        LOAD.clear();
        for (Map.Entry<String, DashSpriteContents> entry : data.sprites.entrySet()) {
            LOAD.put(new ResourceLocation(entry.getKey()), entry.getValue());
        }
        LOGGER.info("Sprite content restore: {} sprites.", LOAD.size());
    }

    /** Snapshot data. Keys are sprite id strings. */
    public static final class Data {
        public final Map<String, DashSpriteContents> sprites;

        public Data(Map<String, DashSpriteContents> sprites) {
            this.sprites = sprites;
        }
    }
}
