package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Exposes a finished stitch ({@code AtlasTexture$SheetData}) for SAVE staging.
 *
 * <p>1.16.5 MCP field names verified via {@code javap} against the mapped
 * Forge jar: {@code sprites} ({@code List<TextureAtlasSprite>}),
 * {@code width}, {@code height}, {@code mipmapLevel} (all package-private
 * on {@code AtlasTexture$SheetData}).
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline — same pending item as
 * {@code ModelManagerCacheMixin} (see {@code PORTING_NOTES.md}).
 */

/**
 * @author Malionaro
 */
@Mixin(value = AtlasTexture.SheetData.class, remap = false)
public interface AtlasSheetDataAccessor {
    @Accessor("sprites")
    List<TextureAtlasSprite> getSprites();

    @Accessor("width")
    int getWidth();

    @Accessor("height")
    int getHeight();

    @Accessor("mipmapLevel")
    int getMipmapLevel();
}
