package dev.notalpha.dashloader.mixin.option.cache.sprite.content;

import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.sprite.content.SpriteContentModule;
import dev.notalpha.dashloader.mixin.accessor.SpriteContentsAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;

@Mixin(SpriteResourceLoader.class)
public interface SpriteOpenerMixin {
	@Inject(
			method = "method_52851",
			cancellable = true,
			locals = LocalCapture.CAPTURE_FAILEXCEPTION,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;open()Ljava/io/InputStream;", shift = At.Shift.BEFORE)
	)
	private static void dashloaderLoad(Collection<MetadataSectionType<?>> metadatas, Identifier id, Resource resource, CallbackInfoReturnable<SpriteContents> cir, ResourceMetadata resourceMetadata) {
		var dashSpriteData = SpriteContentModule.SOURCE.get(CacheStatus.LOAD);
		if (dashSpriteData != null) {
			SpriteContents spriteContents = dashSpriteData.get(id);
			if (spriteContents != null) {
				((SpriteContentsAccessor) spriteContents).setMetadata(resourceMetadata);
				cir.setReturnValue(spriteContents);
			}
		}
	}

	@Inject(
			method = "method_52851",
			at = @At(value = "RETURN")
	)
	private static void dashloaderSave(Collection<?> collection, Identifier id, Resource resource, CallbackInfoReturnable<SpriteContents> cir) {
		var dashSpriteData = SpriteContentModule.SOURCE.get(CacheStatus.SAVE);
		if (dashSpriteData != null) {
			if (dashSpriteData.containsKey(id)) { // filter out sprites with the same id
				dashSpriteData.put(id, null);
				return;
			}
			dashSpriteData.put(id, cir.getReturnValue());
		}
	}
}
