package dev.notalpha.dashloader.mixin.option.cache.sprite.content;

import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.sprite.content.SpriteContentModule;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.client.textures.SpriteContentsConstructor;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteResourceLoader.class)
public interface SpriteOpenerMixin {
	@ModifyReturnValue(
			method = "create",
			at = @At("RETURN")
	)
	private static SpriteResourceLoader dashloaderWrapLoader(SpriteResourceLoader original) {
		return (Identifier spriteLocation, Resource resource, SpriteContentsConstructor constructor) -> {
			var loadData = SpriteContentModule.SOURCE.get(CacheStatus.LOAD);
			if (loadData != null) {
				SpriteContents cached = loadData.get(spriteLocation);
				if (cached != null) {
					return cached;
				}
			}

			SpriteContents result = original.loadSprite(spriteLocation, resource, constructor);

			SpriteContentModule.SOURCE.visit(CacheStatus.SAVE, map -> {
				// Same sprite can be opened twice in one boot (double reload): keep the
				// first result instead of poisoning the cache with null (which would
				// force vanilla decoding on every load).
				map.putIfAbsent(spriteLocation, result);
			});
			return result;
		};
	}
}
