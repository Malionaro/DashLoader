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
 * there, as required):
 * <ul>
 *   <li>Modern caches {@code FontStorage} glyph providers
 *       ({@code TrueTypeFont}, {@code UnihexFont}, {@code BitmapFont}, ...).
 *       1.16.5 has no {@code FontStorage} and no provider system — text goes
 *       through the legacy {@code FontRenderer} ({@code fontRenderer} on
 *       {@code Minecraft}), whose glyph cache is a GPU-uploaded
 *       {@code NativeImage} atlas with no snapshottable CPU-side model.</li>
 *   <li>There is no mappable hook: modern shortcuts {@code FontManager}
 *       reload; the 1.16.5 counterpart ({@code FontResourceManager}) builds
 *       {@code FontRenderer} instances eagerly from TTF streams with no
 *       intermediate cacheable representation.</li>
 * </ul>
 *
 * <p>Missing-font fallback equivalent: this module always falls back to
 * vanilla font loading (every font is "missing" from the cache), exactly like
 * modern's per-entry fallback for uncacheable fonts. {@link #isActive()}
 * still reads the config flag so the UI toggle is honest: enabling it logs a
 * one-time warning and changes nothing.
 * @author Malionaro
 */public final class FontModule {
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
