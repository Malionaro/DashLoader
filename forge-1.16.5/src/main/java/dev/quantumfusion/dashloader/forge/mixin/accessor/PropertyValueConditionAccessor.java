package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.client.renderer.model.multipart.PropertyValueCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the property key/value of a {@link PropertyValueCondition} for
 * SAVE staging/serialization.
 *
 * <p>1.16.5 MCP field names verified via {@code javap} against the mapped
 * Forge jar: {@code key} / {@code value} (private final {@code String}s,
 * no public getters).
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline (see {@code PORTING_NOTES.md}).
 */
@Mixin(value = PropertyValueCondition.class, remap = false)
public interface PropertyValueConditionAccessor {
    @Accessor("field_188125_d")
    String getKey();

    @Accessor("field_188126_e")
    String getValue();
}
