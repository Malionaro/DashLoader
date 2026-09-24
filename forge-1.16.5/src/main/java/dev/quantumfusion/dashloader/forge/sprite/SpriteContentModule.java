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
  * <p>Hook: {@code AtlasTextureStitchMixin} stages stitch outputs into
  * {@link #SAVE} keep-first at {@code stitch} RETURN; {@link #save()}
  * snapshots whatever was staged (empty when textures were never stitched).
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
            // Per-entry skip resilience (modern parity): modded sprite content
            // with no snapshot support must not abort the whole module — the
            // LOAD path falls back to vanilla loading for missing sprites.
            try {
                if (entry.getValue() != null) {
                    out.put(entry.getKey().toString(), entry.getValue());
                } else {
                    skipped.add(entry.getKey().toString());
                }
            } catch (RuntimeException e) {
                skipped.add(entry.getKey().toString());
                LOGGER.warn("Skipping uncacheable sprite {} ({}): {}", entry.getKey(),
                        entry.getValue() == null ? "null"
                                : entry.getValue().getClass().getName(),
                        e.getMessage());
            }
        }
        if (!skipped.isEmpty()) {
            LOGGER.warn("Skipped {} null sprite contents.", skipped.size());
        }
        LOGGER.info("Sprite content snapshot: {} sprites.", out.size());
        return new Data(out);
    }

    /** Empty snapshot used when the module is disabled (keeps the JSON shape stable). */
    public static Data emptyData() {
        return new Data(new LinkedHashMap<String, DashSpriteContents>());
    }

    public static void load(Data data) {
        LOAD.clear();
        if (data == null || data.sprites == null) {
            return;
        }
        for (Map.Entry<String, DashSpriteContents> entry : data.sprites.entrySet()) {
            try {
                LOAD.put(new ResourceLocation(entry.getKey()), entry.getValue());
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping unrestorable cached sprite {}: {}", entry.getKey(), e.getMessage());
            }
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
