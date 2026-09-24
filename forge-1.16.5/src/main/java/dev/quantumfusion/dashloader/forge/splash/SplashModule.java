package dev.quantumfusion.dashloader.forge.splash;

import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge 1.16.5 port of modern {@code SplashModule}
 * ({@code fabric-1.21.4}).
 *
 * <p>Vanilla counterpart: {@code net.minecraft.client.util.Splashes}
 * (a {@code ReloadListener<List<String>>} serving
 * {@code getSplashText()}), fed by {@code texts/splashes.txt}. Same
 * responsibilities as modern: stage the splash list on SAVE
 * ({@link #TEXTS}, mirroring modern {@code TEXTS}), snapshot it into
 * {@link Data}, restore it on LOAD for {@code SplashesCacheMixin} to serve.
 *
 * <p>This module is serialization-trivial (plain strings) and needs no
 * Hyphen support.
 * @author Malionaro
 */public final class SplashModule {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-splash");

    /** Staged splash texts. Mirrors modern {@code SplashModule.TEXTS}. */
    public static final List<String> TEXTS = new ArrayList<>();
    /** Restored splash texts for the mixin to serve. */
    public static final List<String> LOADED = new ArrayList<>();

    private SplashModule() {
    }

    /** Gated on the Forge config equivalent of modern {@code Option.CACHE_SPLASH_TEXT}. */
    public static boolean isActive() {
        return DashLoaderConfig.CACHE_SPLASHES.get();
    }

    public static void reset() {
        TEXTS.clear();
        LOADED.clear();
    }

    public static Data save() {
        LOGGER.info("Splash snapshot: {} texts.", TEXTS.size());
        return new Data(new ArrayList<>(TEXTS));
    }

    /** Empty snapshot used when the module is disabled (keeps the JSON shape stable). */
    public static Data emptyData() {
        return new Data(new ArrayList<String>());
    }

    public static void load(Data data) {
        LOADED.clear();
        if (data != null && data.splashList != null) {
            LOADED.addAll(data.splashList);
        }
        LOGGER.info("Splash restore: {} texts.", LOADED.size());
    }

    /** Snapshot data: the raw splash lines. */
    public static final class Data {
        public final List<String> splashList;

        public Data(List<String> splashList) {
            this.splashList = splashList;
        }
    }
}
