package dev.quantumfusion.dashloader.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * WIP skeleton entrypoint for the DashLoader Forge 1.16.5 port.
 *
 * <p>Era reference: the Fabric code this spike is based on
 * ({@code def-fabric-1.17/.../def/DashLoader.java} at the base commit)
 * initialises caching from a {@code MinecraftClient} mixin after resource
 * reload. The Forge equivalent will hook client setup / resource reload
 * listeners here instead of Fabric entrypoints + SpongePowered Mixins.
 * No caching behaviour is ported yet.
 */
@Mod(DashLoaderForge.MOD_ID)
public class DashLoaderForge {
    public static final String MOD_ID = "dashloader";

    private static final Logger LOGGER = LogManager.getLogger();

    public DashLoaderForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("DashLoader Forge 1.16.5 port skeleton: common setup (no caching yet)");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        LOGGER.info("DashLoader Forge 1.16.5 port skeleton: client setup (no caching yet)");
    }
}
