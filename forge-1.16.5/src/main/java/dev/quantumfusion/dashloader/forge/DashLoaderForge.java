package dev.quantumfusion.dashloader.forge;

import dev.quantumfusion.dashloader.forge.cache.DashCacheBackend;
import dev.quantumfusion.dashloader.forge.ui.DashConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entrypoint for the DashLoader Forge 1.16.5 port.
 *
 * <p>Era reference: the Fabric code this port is based on
 * ({@code fabric-1.21.4}) initialises caching from a
 * {@code MinecraftClient} mixin after resource reload. The Forge
 * equivalents used here:
 * <ul>
 *   <li>Fabric entrypoints -&gt; {@code @Mod} + mod event bus listeners.</li>
 *   <li>Fabric {@code ModMenu} config screen -&gt; vanilla
 *       {@link DashConfigScreen} registered via
 *       {@code ExtensionPoint.CONFIGGUIFACTORY} (no ModMenu on Forge).</li>
 *   <li>Fabric game-dir / environment lookups -&gt; {@link ModList} and
 *       {@code FMLPaths} (see {@link DashCacheBackend}).</li>
 *   <li>Client-only code paths are guarded with {@link DistExecutor} so the
 *       dedicated server never loads client classes.</li>
 * </ul>
 */
@Mod(DashLoaderForge.MOD_ID)
public class DashLoaderForge {
    public static final String MOD_ID = "dashloader";

    private static final Logger LOGGER = LogManager.getLogger();

    public DashLoaderForge() {
        DashLoaderConfig.register();
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, screen) -> new DashConfigScreen(screen));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        DistExecutor.runWhenOn(Dist.CLIENT,
                () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff));
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("DashLoader Forge port: {} mods present, cache backend at {}",
                ModList.get().size(), DashCacheBackend.getCacheDir());
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        LOGGER.info("DashLoader Forge port: client setup (model/sprite/splash/toast modules ported, fonts skipped — see report).");
    }
}
