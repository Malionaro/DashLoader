package dev.notalpha.dashloader.mixin.option.cache.sprite.content;

import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.sprite.content.SpriteContentModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.client.textures.SpriteContentsConstructor;

/**
 * In 1.21.11 {@code SpriteResourceLoader} is a functional interface; the implementation
 * is the lambda returned by {@code SpriteResourceLoader.create}. Wrapping that instance
 * preserves the LOAD (return cached contents) / SAVE (store loaded contents) behavior.
 * On NeoForge the loader takes an extra {@code SpriteContentsConstructor} argument.
 */
@Mixin(SpriteResourceLoader.class)
public interface SpriteOpenerMixin {
	@Inject(
			method = "create",
			at = @At(value = "RETURN"),
			cancellable = true
	)
	private static void dashloaderWrapOpener(Set<MetadataSectionType<?>> additionalMetadata, CallbackInfoReturnable<SpriteResourceLoader> cir) {
		SpriteResourceLoader original = cir.getReturnValue();
		cir.setReturnValue((Identifier spriteLocation, Resource resource, SpriteContentsConstructor constructor) -> {
			var dashSpriteData = SpriteContentModule.SOURCE.get(CacheStatus.LOAD);
			if (dashSpriteData != null) {
				SpriteContents cached = dashSpriteData.get(spriteLocation);
				if (cached != null) {
					return cached;
				}
			}

			SpriteContents result = original.loadSprite(spriteLocation, resource, constructor);

			var saveData = SpriteContentModule.SOURCE.get(CacheStatus.SAVE);
			if (saveData != null && result != null) {
				// Same sprite can be opened twice in one boot (double reload): keep the
				// first result instead of poisoning the cache with null (which would
				// force vanilla decoding on every load).
				saveData.putIfAbsent(spriteLocation, result);
			}
			return result;
		});
	}
}
