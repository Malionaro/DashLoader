package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the decoded frames of a stitched sprite for SAVE staging.
 *
 * <p>1.16.5 MCP field name verified via {@code javap} against the mapped
 * Forge jar: {@code frames} ({@code protected final NativeImage[]} on
 * {@code TextureAtlasSprite}).
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline — same pending item as
 * {@code ModelManagerCacheMixin} (see {@code PORTING_NOTES.md}).
 */
@Mixin(value = TextureAtlasSprite.class, remap = false)
public interface TextureAtlasSpriteAccessor {
    @Accessor("field_195670_c")
    NativeImage[] getFrames();
}
