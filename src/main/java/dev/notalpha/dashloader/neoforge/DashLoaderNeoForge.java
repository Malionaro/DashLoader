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
		// TEMP-DIAG step 6: catch around ServiceLoader loop to capture throw site
		container.registerExtensionPoint(IConfigScreenFactory.class, new DashLoaderConfigScreenFactory());
		DashLoader.LOG.info("DL-NEO STEP6 start");
		dev.notalpha.dashloader.api.cache.CacheFactory factory =
				dev.notalpha.dashloader.api.cache.CacheFactory.create();
		DashLoader.LOG.info("DL-NEO STEP6 factory ok");
		try {
			for (dev.notalpha.dashloader.api.DashEntrypoint ep
					: java.util.ServiceLoader.load(dev.notalpha.dashloader.api.DashEntrypoint.class)) {
				DashLoader.LOG.info("DL-NEO STEP6 entrypoint {}", ep.getClass().getName());
				ep.onDashLoaderInit(factory);
			}
			DashLoader.LOG.info("DL-NEO STEP6 entrypoints done");
		} catch (Throwable t) {
			DashLoader.LOG.fatal("DL-NEO STEP6 loop failed", t);
			throw new RuntimeException("STEP6 failed: " + t, t);
		}
		factory.build(java.nio.file.Path.of("./dashloader-cache/client/"));
		DashLoader.LOG.info("DL-NEO STEP6 built");
	}
}
