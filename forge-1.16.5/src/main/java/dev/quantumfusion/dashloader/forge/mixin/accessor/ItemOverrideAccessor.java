package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.client.renderer.model.ItemOverride;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes the predicate map of an {@link ItemOverride} for SAVE staging.
 *
 * <p>1.16.5 MCP field name verified via {@code javap} against the mapped
 * snapshot jar: {@code mapResourceValues} (private final
 * {@code Map<ResourceLocation, Float>}, no public getter;
 * {@code getLocation()} is public).
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline (see {@code PORTING_NOTES.md}).
 */
@Mixin(value = ItemOverride.class, remap = false)
public interface ItemOverrideAccessor {
    @Accessor("mapResourceValues")
    Map<ResourceLocation, Float> getPredicateMap();
}
