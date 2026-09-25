package dev.quantumfusion.dashloader.forge.font;

import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Forge 1.16.5 stand-in for modern {@code FontModule}
 * ({@code fabric-26.3}).
 *
 * <p>PERMANENT skip (see {@code PORTING_NOTES.md} — documented here and
 * there, as required). Precise reason (javap-verified against the mapped
 * snapshot jar, not the pre-1.13 ascii/unicode-page assumption):
 * <ul>
 *   <li>1.16.5 DOES have a provider system ({@code Font} with
 *       {@code List<IGlyphProvider> glyphProviders} + {@code List<FontTexture>
 *       textures}), but providers are TTF/JSON-driven ({@code IGlyphInfo}
 *       with {@code uploadGlyph(x, y)} writing straight to GPU) — there is no
 *       CPU-side glyph atlas model to snapshot. {@code FontTexture} holds only
 *       {@code textureLocation}, render types, {@code colored} and a packing
 *       {@code Entry} tree (javap: no {@code NativeImage} field); the pixels
 *       live in GL memory.</li>
 *   <li>The only readback ({@code NativeImage#downloadFromTexture}) needs a
 *       live GL context at LOAD time (reload-listener thread has none safely)
 *       and would re-upload anyway on first text render — caching saves
 *       nothing while risking glyph UV mismatches (the {@code Entry} packing
 *       order is allocation-order dependent) and text corruption.</li>
 *   <li>There is no mappable hook: modern shortcuts {@code FontManager}
 *       reload; the 1.16.5 counterpart ({@code FontResourceManager}) builds
 *       {@code Font} instances eagerly from TTF streams with no cacheable
 *       intermediate, and {@code FontRenderer} resolves fonts per-frame via
 *       {@code Function<ResourceLocation, Font>}.</li>
 * </ul>
 *
 * <p>Missing-font fallback equivalent: this module always falls back to
 * vanilla font loading (every font is "missing" from the cache), exactly like
 * modern's per-entry fallback for uncacheable fonts. {@link #isActive()}
 * still reads the config flag so the UI toggle is honest: enabling it logs a
 * one-time warning and changes nothing.
 */
public final class FontModule {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-font");
    private static boolean warned;

    private FontModule() {
    }

    /** Reads the Forge config equivalent of modern {@code Option.CACHE_FONT}. */
    public static boolean isActive() {
        boolean active = DashLoaderConfig.CACHE_FONTS.get();
        if (active && !warned) {
            warned = true;
            LOGGER.warn("Font caching is a documented PERMANENT skip on 1.16.5 "
                    + "(legacy FontRenderer, no FontStorage/providers) — using vanilla fonts.");
        }
        return false;
    }

    public static void reset() {
    }

    public static Data save() {
        return new Data(Collections.<String>emptyList());
    }

    public static void load(Data data) {
        // No-op: every font falls back to vanilla loading.
    }

    /** Snapshot data: always empty on this version. */
    public static final class Data {
        public final List<String> fonts;

        public Data(List<String> fonts) {
            this.fonts = fonts;
        }
    }
}
