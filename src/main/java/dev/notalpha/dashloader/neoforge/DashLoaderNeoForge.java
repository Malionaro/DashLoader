package dev.notalpha.dashloader.neoforge;

import dev.notalpha.dashloader.DashLoader;
import dev.notalpha.dashloader.client.DashLoaderClient;
import dev.notalpha.dashloader.client.DashLoaderConfigScreenFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge mod entrypoint (client-only).
 *
 * <p>Referencing {@link DashLoaderClient#CACHE} forces the {@code DashLoaderClient}
 * static initializer to run, which builds the cache via {@link java.util.ServiceLoader}
 * entrypoints. Mixins also touch {@code DashLoaderClient.CACHE}, so this is a
 * belt-and-braces trigger executed at mod construction time.
 */
@Mod(value = "dashloader", dist = Dist.CLIENT)
public class DashLoaderNeoForge {
	public DashLoaderNeoForge(ModContainer container) {
		// TEMP-DIAG step 7: replicate onDashLoaderInit module by module
		container.registerExtensionPoint(IConfigScreenFactory.class, new DashLoaderConfigScreenFactory());
		DashLoader.LOG.info("DL-NEO STEP7 start");
		dev.notalpha.dashloader.api.cache.CacheFactory factory =
				dev.notalpha.dashloader.api.cache.CacheFactory.create();
		try {
			DashLoader.LOG.info("DL-NEO STEP7 FontModule");
			factory.addModule(new dev.notalpha.dashloader.client.font.FontModule());
			DashLoader.LOG.info("DL-NEO STEP7 ModelModule");
			factory.addModule(new dev.notalpha.dashloader.client.model.ModelModule());
			DashLoader.LOG.info("DL-NEO STEP7 SplashModule");
			factory.addModule(new dev.notalpha.dashloader.client.splash.SplashModule());
			DashLoader.LOG.info("DL-NEO STEP7 StitchModule");
			factory.addModule(new dev.notalpha.dashloader.client.sprite.stitch.SpriteStitcherModule());
			DashLoader.LOG.info("DL-NEO STEP7 ContentModule");
			factory.addModule(new dev.notalpha.dashloader.client.sprite.content.SpriteContentModule());
			DashLoader.LOG.info("DL-NEO STEP7 modules done");
			factory.build(java.nio.file.Path.of("./dashloader-cache/client/"));
			DashLoader.LOG.info("DL-NEO STEP7 built");
		} catch (Throwable t) {
			DashLoader.LOG.fatal("DL-NEO STEP7 failed", t);
			throw new RuntimeException("STEP7 failed: " + t, t);
		}
	}
}
