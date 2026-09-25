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
@Mixin(value = AtlasTexture.SheetData.class, remap = false)
public interface AtlasSheetDataAccessor {
    @Accessor("field_217808_d")
    List<TextureAtlasSprite> getSprites();

    @Accessor("field_217806_b")
    int getWidth();

    @Accessor("field_217807_c")
    int getHeight();

    @Accessor("field_229224_d_")
    int getMipmapLevel();
}
