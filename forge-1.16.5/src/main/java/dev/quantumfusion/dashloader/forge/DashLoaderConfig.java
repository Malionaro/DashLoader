package dev.quantumfusion.dashloader.forge;

import dev.quantumfusion.dashloader.forge.ui.DashConfigScreen;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Client config for the DashLoader Forge 1.16.5 port.
 *
 * <p>Forge-side scaffolding using the standard 1.16.5 {@link ForgeConfigSpec}
 * API. The per-module booleans are the Forge equivalent of modern
 * {@code Option} gates ({@code fabric-1.21.4}): each module's
 * {@code isActive()} reads its flag here, and {@link DashConfigScreen} binds
 * them to toggle buttons.
 *
 * <p>There is no era counterpart at the base commit (era config moved to
 * {@code dashloader-core} months later, Nov 2021).
 * @author Malionaro
 */public final class DashLoaderConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CACHE;
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    /** Modern {@code Option.CACHE_MODEL_LOADER}. */
    public static final ForgeConfigSpec.BooleanValue CACHE_MODELS;
    /** Modern {@code Option.CACHE_SPRITE_CONTENT}. */
    public static final ForgeConfigSpec.BooleanValue CACHE_SPRITES;
    /** Modern {@code Option.CACHE_SPRITE_STITCHING}. */
    public static final ForgeConfigSpec.BooleanValue CACHE_STITCHING;
    /** Modern {@code Option.CACHE_SPLASH_TEXT}. */
    public static final ForgeConfigSpec.BooleanValue CACHE_SPLASHES;
    /**
     * Modern {@code Option.CACHE_FONT}. Kept for UI completeness but
     * unsupported — see the FontModule port notes (legacy
     * {@code FontRenderer}, no {@code FontStorage}/providers in 1.16.5).
     */
    public static final ForgeConfigSpec.BooleanValue CACHE_FONTS;
    /** Modern {@code Config.showCachingToast}. */
    public static final ForgeConfigSpec.BooleanValue SHOW_CACHING_TOAST;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("dashloader");
        ENABLE_CACHE = builder
                .comment("Master switch: enable cached loading when a cache is present.")
                .define("enableCache", true);
        DEBUG = builder
                .comment("Enable extra DashLoader debug logging.")
                .define("debug", false);
        CACHE_MODELS = builder
                .comment("Cache baked models (ModelModule).")
                .define("cacheModels", true);
        CACHE_SPRITES = builder
                .comment("Cache sprite contents (SpriteContentModule).")
                .define("cacheSprites", true);
        CACHE_STITCHING = builder
                .comment("Cache atlas stitching (SpriteStitcherModule).")
                .define("cacheStitching", true);
        CACHE_SPLASHES = builder
                .comment("Cache splash texts (SplashModule).")
                .define("cacheSplashes", true);
        CACHE_FONTS = builder
                .comment("Cache fonts. UNSUPPORTED on 1.16.5 (no FontStorage/providers) — kept for UI completeness.")
                .define("cacheFonts", false);
        SHOW_CACHING_TOAST = builder
                .comment("Show the caching progress toast while the cache is written.")
                .define("showCachingToast", true);
        builder.pop();
        CLIENT_SPEC = builder.build();
    }

    private DashLoaderConfig() {
    }

    /** Registers the client spec. Called once from {@link DashLoaderForge}. */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
}
