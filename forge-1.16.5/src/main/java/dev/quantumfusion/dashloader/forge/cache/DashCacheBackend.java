package dev.quantumfusion.dashloader.forge.cache;

import com.google.gson.Gson;
import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import dev.quantumfusion.dashloader.forge.model.ModelModule;
import dev.quantumfusion.dashloader.forge.splash.SplashModule;
import dev.quantumfusion.dashloader.forge.sprite.SpriteContentModule;
import dev.quantumfusion.dashloader.forge.sprite.stitch.SpriteStitcherModule;
import dev.quantumfusion.dashloader.forge.ui.toast.DashToastState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gson-based cache backend for the Forge 1.16.5 port.
 *
 * <p>Modern reference ({@code fabric-26.3}): {@code CacheImpl} +
 * {@code CacheFactoryImpl} + {@code DashLoaderClient.CACHE}. The modern cache
 * directory layout is {@code ./dashloader-cache/client/<MOD_HASH>/<packHash>/},
 * where {@code MOD_HASH} is the MD5 of the sorted {@code modId+version} list
 * ({@code DashLoader#MOD_HASH}) and {@code packHash} is the MD5 of the
 * selected resource packs ({@code ReloadableResourceManagerImplMixin}).
 * This port keeps that exact directory layout, rooted at
 * {@code FMLPaths.GAMEDIR} (the Forge equivalent of the Fabric game-dir
 * lookup): {@code <gamedir>/dashloader-cache/client/<MOD_HASH>/<packHash>/}.
 *
 * <p>Serialization differs by necessity: modern uses Hyphen (an int-pointer
 * object registry over a binary format), which is out on this toolchain (see
 * {@code CacheGson} and {@code PORTING_NOTES.md}). The same per-module
 * {@code Data} POJOs ({@code ModelModule.Data},
 * {@code SpriteContentModule.Data}, {@code SpriteStitcherModule.Data},
 * {@code SplashModule.Data}) are written as JSON files
 * ({@code models.json}, {@code sprites.json}, {@code stitch.json},
 * {@code splashes.json}) plus a {@code cache-info.json} sidecar.
 *
 * <p>Lifecycle mirrors {@code CacheImpl}: {@link #ensureLoaded()} picks
 * {@link CacheStatus#LOAD} when the directory exists and deserializes into
 * the module LOAD stores, otherwise {@link CacheStatus#SAVE};
 * {@link #save(DashToastState)} snapshots the SAVE stores (updating the toast
 * when non-null); {@link #reset()} returns to {@link CacheStatus#IDLE} and
 * clears staged data to save memory.
 * @author Malionaro
 */public final class DashCacheBackend {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-cache");

    private static final String MODELS_FILE = "models.json";
    private static final String SPRITES_FILE = "sprites.json";
    private static final String STITCH_FILE = "stitch.json";
    private static final String SPLASHES_FILE = "splashes.json";
    private static final String INFO_FILE = "cache-info.json";

    private static final String MC_VERSION = "1.16.5";
    private static final String MOD_VERSION = "5.1.0-beta.9.1-1.16.5";

    private static final Gson GSON = CacheGson.create();

    private static volatile CacheStatus status = CacheStatus.IDLE;
    private static volatile String modHash;
    private static volatile String packHash;
    private static volatile boolean saveStarted;

    private DashCacheBackend() {
    }

    /**
     * Forge equivalent of the Fabric game-dir lookup, parallel to modern
     * {@code Path.of("./dashloader-cache/client/")}.
     */
    public static Path getCacheDir() {
        return FMLPaths.GAMEDIR.get().resolve("dashloader-cache").resolve("client");
    }

    /** Full directory for the current mod+pack combination (modern {@code Cache#getDir()}). */
    public static Path getDir() {
        if (modHash == null || packHash == null) {
            throw new IllegalStateException("Cache hashes have not been computed yet (call ensureLoaded first).");
        }
        return getCacheDir().resolve(modHash).resolve(packHash);
    }

    public static CacheStatus getStatus() {
        return status;
    }

    public static boolean hasCache() {
        return status == CacheStatus.LOAD;
    }

    /** Whether the background SAVE task has been kicked off for this boot. */
    public static boolean isSaveStarted() {
        return saveStarted;
    }

    public static void markSaveStarted() {
        saveStarted = true;
    }

    /**
     * Computes both hashes and enters LOAD (deserializing) or SAVE state.
     * Safe to call repeatedly; only the first call per boot takes effect.
     * Defers (stays IDLE) while the client is unavailable, so early calls
     * from mod construction are harmless.
     */
    public static synchronized void ensureLoaded() {
        if (status != CacheStatus.IDLE) {
            return;
        }
        try {
            modHash = computeModHash();
        } catch (Exception e) {
            LOGGER.warn("Could not compute mod hash, deferring cache init.", e);
            return;
        }
        try {
            packHash = computePackHash();
        } catch (Exception e) {
            // Minecraft (or its pack list) is not ready yet; a later reload
            // hook (ModelManager.prepare) retries.
            LOGGER.debug("Resource packs not ready yet, deferring cache init.");
            return;
        }
        Path dir = getDir();
        if (Files.isDirectory(dir) && Files.exists(dir.resolve(INFO_FILE))) {
            status = CacheStatus.LOAD;
            loadCache(dir);
        } else {
            status = CacheStatus.SAVE;
            LOGGER.info("DashLoader: no cache at {}, will SAVE after reload.", dir);
        }
    }

    /**
     * Modern {@code DashLoader#MOD_HASH} equivalent: MD5 (uppercase hex) of
     * the index-sorted {@code modId&version} list. Verified against
     * {@code ModList#getMods()} / {@code ModInfo#getModId()}/{@code getVersion()}.
     */
    static String computeModHash() {
        List<ModInfo> mods = new ArrayList<>(ModList.get().getMods());
        mods.sort(Comparator.comparing(ModInfo::getModId));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mods.size(); i++) {
            ModInfo info = mods.get(i);
            builder.append(i).append('$').append(info.getModId()).append('&')
                    .append(info.getVersion().toString());
        }
        return md5Hex(builder.toString());
    }

    /**
     * Modern {@code ReloadableResourceManagerImplMixin} equivalent:
     * content-aware pack hash so pack updates invalidate sprite/model caches.
     * Each enabled pack contributes
     * {@code name/title/description} (ordered by name for determinism —
     * modern iterates the pack repository selection order); the MD5
     * (uppercase hex) of the list string is the hash.
     *
     * <p>1.16.5 MCP names verified via {@code javap}:
     * {@code Minecraft#getResourcePackList()} -&gt;
     * {@code ResourcePackList#getEnabledPacks()} -&gt;
     * {@code ResourcePackInfo#getName()/getTitle()/getDescription()}.
     *
     * <p>Simplification vs modern (documented): the dedicated {@code server}
     * pack path special-case (zip path instead of display strings) is not
     * replicated; server packs still contribute their name/title/description,
     * which changes on pack updates like any other pack.
     */
    static String computePackHash() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            throw new IllegalStateException("Minecraft not ready");
        }
        Collection<ResourcePackInfo> packs = minecraft.getResourcePackList().getEnabledPacks();
        List<String> values = new ArrayList<>();
        List<ResourcePackInfo> sorted = new ArrayList<>(packs);
        sorted.sort(Comparator.comparing(ResourcePackInfo::getName));
        for (ResourcePackInfo pack : sorted) {
            String title = pack.getTitle() == null ? "" : pack.getTitle().getString();
            String desc = pack.getDescription() == null ? "" : pack.getDescription().getString();
            values.add(pack.getName() + "/" + title + "/" + desc + "/");
        }
        String hash = md5Hex(values.toString());
        LOGGER.info("DashLoader pack hash: {}", hash);
        return hash;
    }

    /** Snapshot SAVE stores to disk. Updates the toast when non-null. Never throws. */
    public static synchronized boolean save(DashToastState toast) {
        if (status != CacheStatus.SAVE) {
            LOGGER.warn("DashLoader save requested while status is {}, ignoring.", status);
            return false;
        }
        if (modHash == null || packHash == null) {
            LOGGER.error("DashLoader save requested before hashes were computed.");
            return false;
        }
        LOGGER.info("Starting DashLoader caching (Gson backend).");
        long start = System.currentTimeMillis();
        Path dir = getDir();
        try {
            Files.createDirectories(dir);

            progress(toast, 0.05, "models");
            ModelModule.Data models = ModelModule.isActive() ? ModelModule.save() : ModelModule.emptyData();
            writeJson(dir.resolve(MODELS_FILE), models);

            progress(toast, 0.45, "sprites");
            SpriteContentModule.Data sprites = SpriteContentModule.isActive()
                    ? SpriteContentModule.save() : SpriteContentModule.emptyData();
            writeJson(dir.resolve(SPRITES_FILE), sprites);

            progress(toast, 0.65, "stitch");
            SpriteStitcherModule.Data stitch = SpriteStitcherModule.isActive()
                    ? SpriteStitcherModule.save() : SpriteStitcherModule.emptyData();
            writeJson(dir.resolve(STITCH_FILE), stitch);

            progress(toast, 0.85, "splashes");
            SplashModule.Data splashes = SplashModule.isActive() ? SplashModule.save() : SplashModule.emptyData();
            writeJson(dir.resolve(SPLASHES_FILE), splashes);

            writeJson(dir.resolve(INFO_FILE),
                    new CacheInfo(modHash, packHash, MC_VERSION, MOD_VERSION, System.currentTimeMillis()));

            progress(toast, 1.0, "done");
            LOGGER.info("Saved DashLoader cache to {} in {} ms.", dir, System.currentTimeMillis() - start);
            return true;
        } catch (Throwable t) {
            LOGGER.error("Failed caching DashLoader data, removing partial cache.", t);
            removeQuietly(dir);
            return false;
        }
    }

    /** No-toast overload (kept for call sites without a visible toast). */
    public static boolean save() {
        return save(null);
    }

    /** Stub-compatible overload: real load path is {@link #ensureLoaded()}. */
    public static void load() {
        ensureLoaded();
    }

    /** Clears staged data and returns to IDLE (modern {@code Cache#reset()}). */
    public static synchronized void reset() {
        status = CacheStatus.IDLE;
        modHash = null;
        packHash = null;
        saveStarted = false;
        ModelModule.reset();
        SpriteContentModule.reset();
        SpriteStitcherModule.reset();
        SplashModule.reset();
        ModelModule.clearLoad();
    }

    /** Deletes the current cache directory (modern {@code Cache#remove()}). */
    public static void remove() {
        if (modHash != null && packHash != null) {
            removeQuietly(getDir());
        }
    }

    private static void loadCache(Path dir) {
        long start = System.currentTimeMillis();
        try {
            ModelModule.Data models = readJson(dir.resolve(MODELS_FILE), ModelModule.Data.class);
            SpriteContentModule.Data sprites = readJson(dir.resolve(SPRITES_FILE), SpriteContentModule.Data.class);
            SpriteStitcherModule.Data stitch = readJson(dir.resolve(STITCH_FILE), SpriteStitcherModule.Data.class);
            SplashModule.Data splashes = readJson(dir.resolve(SPLASHES_FILE), SplashModule.Data.class);

            if (models != null) {
                ModelModule.load(models);
            }
            if (sprites != null) {
                SpriteContentModule.load(sprites);
            }
            if (stitch != null) {
                SpriteStitcherModule.load(stitch);
            }
            if (splashes != null) {
                SplashModule.load(splashes);
            }
            LOGGER.info("Loaded DashLoader cache from {} in {} ms.", dir, System.currentTimeMillis() - start);
        } catch (Exception e) {
            LOGGER.error("Failed loading DashLoader cache, falling back to SAVE.", e);
            status = CacheStatus.SAVE;
            removeQuietly(dir);
        }
    }

    private static void progress(DashToastState toast, double value, String text) {
        if (toast != null) {
            toast.setProgress(value);
            toast.setText(text);
        }
    }

    private static void writeJson(Path path, Object value) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        }
    }

    private static <T> T readJson(Path path, Class<T> clazz) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, clazz);
        }
    }

    private static void removeQuietly(Path dir) {
        try {
            if (!Files.exists(dir)) {
                return;
            }
            List<Path> paths = new ArrayList<>();
            Files.walk(dir).forEach(paths::add);
            Collections.sort(paths, Collections.reverseOrder());
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            LOGGER.error("Could not remove cache {}", dir, e);
        }
    }

    static String md5Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(Character.forDigit((b >> 4) & 0xF, 16));
                out.append(Character.forDigit(b & 0xF, 16));
            }
            return out.toString().toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }

    /** Sidecar metadata (diagnostics + invalidation sanity checks). */
    public static final class CacheInfo {
        public final String modHash;
        public final String packHash;
        public final String mcVersion;
        public final String modVersion;
        public final long timestamp;

        public CacheInfo(String modHash, String packHash, String mcVersion, String modVersion, long timestamp) {
            this.modHash = modHash;
            this.packHash = packHash;
            this.mcVersion = mcVersion;
            this.modVersion = modVersion;
            this.timestamp = timestamp;
        }
    }

    /** Test-visible config gate mirror (avoids classloading client config in unit checks). */
    static boolean cacheEnabled() {
        try {
            return DashLoaderConfig.ENABLE_CACHE.get();
        } catch (Exception e) {
            return true;
        }
    }

    /** Kept for diagnostics: staged SAVE sizes without touching the disk. */
    public static Map<String, Integer> describeStaged() {
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("topModels", ModelModule.SAVE_TOP_MODELS.size());
        out.put("sprites", SpriteContentModule.SAVE.size());
        out.put("stitchers", SpriteStitcherModule.STITCHERS_SAVE.size());
        out.put("splashes", SplashModule.TEXTS.size());
        return out;
    }
}
