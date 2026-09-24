package dev.quantumfusion.dashloader.forge.cache;

import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * Cache backend stub for the Forge 1.16.5 port.
 *
 * <p>Honesty note (per port instructions, honesty over hacks): Hyphen
 * (modern's serialization library) is deliberately OUT. Modern's cache
 * format is a Hyphen-scanned object graph rooted at per-module
 * {@code Data} classes with an int-pointer object registry
 * ({@code RegistryWriter}/{@code RegistryReader}); bringing that up on the
 * Java 8 / ForgeGradle 4 toolchain would mean either backporting Hyphen +
 * {@code dashloader-core} 3.0-SNAPSHOT (unpublished, unresolvable — see
 * {@code DashModelCache}) or hand-rolling an incompatible format. Both are
 * out of scope for this timebox, so this backend is a documented stub:
 * module {@code Data} objects are well-defined POJOs (see
 * {@code ModelModule.Data}, {@code SpriteContentModule.Data},
 * {@code SpriteStitcherModule.Data}, {@code SplashModule.Data}), but no
 * bytes are written or read yet.
 *
 * <p>What exists:
 * <ul>
 *   <li>{@link #getCacheDir()} — Forge-side location
 *       ({@code FMLPaths.GAMEDIR}, the Forge equivalent of Fabric's
 *       game-dir lookup), so future serialization work has a home.</li>
 *   <li>{@link #hasCache()} — always {@code false}; loaders therefore
 *       always take the vanilla path.</li>
 *   <li>{@link #save()} / {@link #load()} — log + no-op.</li>
 * </ul>
 *
 * <p>TODO (top blockers for a working port, see final report):
 * <ol>
 *   <li>Pick a Java 8-compatible serialization for the module
 *       {@code Data} POJOs (Gson is already on the 1.16.5 classpath) and
 *       implement {@code save}/{@code load} against {@link #getCacheDir()}.</li>
 *   <li>Fix the Mixin refmap pipeline (SRG runtime remapping) so the
 *       {@code ModelManager}/{@code Splashes} hooks work outside dev.</li>
 *   <li>Wire LOAD-side installation (models into the bakery, sprites into
 *       the stitch path) via the documented hook points.</li>
 * </ol>
 */
public final class DashCacheBackend {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-cache");

    private DashCacheBackend() {
    }

    /** Forge equivalent of the Fabric game-dir lookup: {@code <gamedir>/dashloader-cache}. */
    public static Path getCacheDir() {
        return FMLPaths.GAMEDIR.get().resolve("dashloader-cache");
    }

    /** Always {@code false} in this slice — vanilla loading path is used. */
    public static boolean hasCache() {
        return false;
    }

    /** Stub: collects module snapshots later; currently logs and drops them. */
    public static void save() {
        LOGGER.info("DashLoader cache save requested (stub backend: data staged in module SAVE maps, no bytes written).");
    }

    /** Stub: nothing to restore yet. */
    public static void load() {
        LOGGER.info("DashLoader cache load requested (stub backend: no cache present).");
    }
}
