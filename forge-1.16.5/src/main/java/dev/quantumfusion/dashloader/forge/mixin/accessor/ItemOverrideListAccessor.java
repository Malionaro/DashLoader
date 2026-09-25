package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Exposes the baked override targets of an {@link ItemOverrideList} for SAVE
 * staging.
 *
 * <p>1.16.5 MCP field name verified via {@code javap} against the mapped
 * snapshot jar: {@code overrideBakedModels} (private final
 * {@code List<IBakedModel>}, no public getter; {@code getOverrides()} is
 * public).
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline (see {@code PORTING_NOTES.md}).
 */
@Mixin(value = ItemOverrideList.class, remap = false)
public interface ItemOverrideListAccessor {
    @Accessor("field_209582_c")
    List<IBakedModel> getOverrideBakedModels();
}
