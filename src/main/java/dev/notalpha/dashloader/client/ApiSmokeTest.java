package dev.notalpha.dashloader.client;

import dev.notalpha.dashloader.api.DashLoaderAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiSmokeTest {
	private static final Logger LOG = LoggerFactory.getLogger("DashLoaderSmoke");

	public static void run() {
		DashLoaderAPI.addStatusListener(status -> LOG.info("SMOKE listener got status {}", status));
		LOG.info("SMOKE status at init: {} (loaded={} saving={} idle={})",
				DashLoaderAPI.getStatus(), DashLoaderAPI.isLoaded(), DashLoaderAPI.isSaving(), DashLoaderAPI.isIdle());
		DashLoaderAPI.whenLoaded(() -> LOG.info("SMOKE whenLoaded fired, dir={}", DashLoaderAPI.getCacheDir()));
	}
}
