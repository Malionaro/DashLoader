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
		// TEMP-DIAG step 2: minimal constructor to bisect pre-body failure
		System.out.println("DL-NEO BOOT");
	}
}
