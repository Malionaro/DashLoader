package dev.quantumfusion.dashloader.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * WIP skeleton: client config for the DashLoader Forge 1.16.5 port.
 *
 * <p>No era counterpart exists at the base commit (era config moved to
 * {@code dashloader-core} months later, Nov 2021) — this is new Forge-side
 * scaffolding using the standard 1.16.5 {@link ForgeConfigSpec} API so a
 * future slice has somewhere to read {@code enableCache}/{@code debug} from.
 * Values are not consumed by any behaviour yet.
 */
public final class DashLoaderConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CACHE;
    public static final ForgeConfigSpec.BooleanValue DEBUG;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("dashloader");
        ENABLE_CACHE = builder
                .comment("Enable cached model loading when a cache is present (no-op in skeleton).")
                .define("enableCache", true);
        DEBUG = builder
                .comment("Enable extra DashLoader debug logging (no-op in skeleton).")
                .define("debug", false);
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
