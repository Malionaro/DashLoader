package dev.notalpha.dashloader;

import dev.notalpha.dashloader.api.DashModule;
import dev.notalpha.dashloader.api.cache.Cache;
import dev.notalpha.dashloader.api.cache.CacheStatusBus;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.config.ConfigHandler;
import dev.notalpha.dashloader.io.MappingSerializer;
import dev.notalpha.dashloader.io.RegistrySerializer;
import dev.notalpha.dashloader.io.data.CacheInfo;
import dev.notalpha.dashloader.misc.ProfilerUtil;
import dev.notalpha.dashloader.registry.MissingHandler;
import dev.notalpha.dashloader.registry.RegistryReaderImpl;
import dev.notalpha.dashloader.registry.RegistryWriterImpl;
import dev.notalpha.dashloader.registry.data.StageData;
import dev.notalpha.taski.builtin.StepTask;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class CacheImpl implements Cache {
	private static final String METADATA_FILE_NAME = "metadata.bin";
	private static final String TMP_SUFFIX = ".tmp";
	private final Path cacheDir;
	// DashLoader metadata
	private final List<DashModule<?>> cacheHandlers;
	private final List<DashObjectClass<?, ?>> dashObjects;
	private final List<MissingHandler<?>> missingHandlers;
	// Serializers
	private final RegistrySerializer registrySerializer;
	private final MappingSerializer mappingsSerializer;
	private CacheStatus status;
	private volatile String hash;

	CacheImpl(Path cacheDir, List<DashModule<?>> cacheHandlers, List<DashObjectClass<?, ?>> dashObjects, List<MissingHandler<?>> missingHandlers) {
		this.cacheDir = cacheDir;
		this.cacheHandlers = cacheHandlers;
		this.dashObjects = dashObjects;
		this.missingHandlers = missingHandlers;
		this.registrySerializer = new RegistrySerializer(dashObjects);
		this.mappingsSerializer = new MappingSerializer(cacheHandlers);
	}

	public void load(String name) {
		this.hash = name;

		if (this.exists()) {
			this.setStatus(CacheStatus.LOAD);
			this.loadCache();
		} else {
			this.setStatus(CacheStatus.SAVE);
		}
	}

	public boolean save(@Nullable Consumer<StepTask> taskConsumer) {
		if (status != CacheStatus.SAVE) {
			throw new RuntimeException("Status is not SAVE");
		}
		DashLoader.LOG.info("Starting DashLoader Caching");
		// Everything is written into a sibling temp directory and moved into place
		// once it is complete. The save runs on a daemon thread, so it can be killed
		// at any moment while the game shuts down. Without the move, that would leave
		// a half written cache that exists() reports as valid and the next launch
		// would waste a full load attempt on it.
		Path tmpDir = getTmpDir();
		try {

			Path ourDir = getDir();

			// A leftover from a save that was killed before it could commit.
			FileUtils.deleteQuietly(tmpDir.toFile());

			// Max caches
			int maxCaches = ConfigHandler.INSTANCE.config.maxCaches;
			if (maxCaches != -1) {
				DashLoader.LOG.info("Checking for cache count.");
				try {
					FileTime oldestTime = null;
					Path oldestPath = null;
					int cacheCount = 1;
					try (Stream<Path> stream = Files.list(cacheDir)) {
						for (Path path : stream.toList()) {
							if (!Files.isDirectory(path)) {
								continue;
							}

							if (path.equals(ourDir) || isTmpDir(path)) {
								continue;
							}

							cacheCount += 1;

							try {
								BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
								FileTime lastAccessTime = attrs.lastAccessTime();
								if (oldestTime == null || lastAccessTime.compareTo(oldestTime) < 0) {
									oldestTime = lastAccessTime;
									oldestPath = path;
								}
							} catch (IOException e) {
								DashLoader.LOG.warn("Could not find access time for cache.", e);
							}
						}
					}

					if (oldestPath != null && cacheCount > maxCaches) {
						DashLoader.LOG.info("Removing {} as we are currently above the maximum caches.", oldestPath);
						if (!FileUtils.deleteQuietly(oldestPath.toFile())) {
							DashLoader.LOG.error("Could not remove cache {}", oldestPath);
						}
					}
				} catch (NoSuchFileException ignored) {
				} catch (IOException io) {
					DashLoader.LOG.error("Could not enforce maximum cache ", io);
				}
			}

			long start = System.currentTimeMillis();

			StepTask main = new StepTask("save", 2);
			if (taskConsumer != null) {
				taskConsumer.accept(main);
			}

			RegistryWriterImpl factory = RegistryWriterImpl.create(missingHandlers, dashObjects);

			// Mappings
			mappingsSerializer.save(tmpDir, factory, cacheHandlers, main);
			main.next();

			// serialization
			main.run(new StepTask("serialize", 2), (task) -> {
				try {
					CacheInfo info = this.registrySerializer.serialize(tmpDir, factory, task::setSubTask);
					task.next();
					DashLoader.METADATA_SERIALIZER.save(tmpDir.resolve(METADATA_FILE_NAME), new StepTask("hi"), info);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				task.next();
			});

			// Commit. The rename is atomic, so getDir() either does not exist at all
			// or is a complete cache.
			if (Files.exists(ourDir)) {
				this.remove();
			}
			Files.move(tmpDir, ourDir, StandardCopyOption.ATOMIC_MOVE);

			DashLoader.LOG.info("Saved cache in {}", ProfilerUtil.getTimeStringFromStart(start));
			return true;
		} catch (Throwable thr) {
			DashLoader.LOG.error("Failed caching", thr);
			this.setStatus(CacheStatus.SAVE);
			FileUtils.deleteQuietly(tmpDir.toFile());
			this.remove();
			return false;
		}
	}

	private static boolean isTmpDir(Path path) {
		return path.getFileName().toString().endsWith(TMP_SUFFIX);
	}

	private void loadCache() {
		if (status != CacheStatus.LOAD) {
			throw new RuntimeException("Status is not LOAD");
		}

		long start = System.currentTimeMillis();
		try {
			StepTask task = new StepTask("Loading DashCache", 3);
			Path cacheDir = getDir();

			// Get metadata
			Path metadataPath = cacheDir.resolve(METADATA_FILE_NAME);
			CacheInfo info = DashLoader.METADATA_SERIALIZER.load(metadataPath);

			// File reading
			StageData[] stageData = registrySerializer.deserialize(cacheDir, info, dashObjects);
			RegistryReaderImpl reader = new RegistryReaderImpl(info, stageData);

			// Exporting assets
			task.run(() -> reader.export(task::setSubTask));

			// Loading mappings
			if (!mappingsSerializer.load(cacheDir, reader, cacheHandlers)) {
				this.setStatus(CacheStatus.SAVE);
				this.remove();
				return;
			}

			DashLoader.LOG.info("Loaded cache in {}", ProfilerUtil.getTimeStringFromStart(start));
		} catch (Exception e) {
			DashLoader.LOG.error("Summoned CrashLoader in {}", ProfilerUtil.getTimeStringFromStart(start), e);
			this.setStatus(CacheStatus.SAVE);
			this.remove();
		}
	}

	public boolean exists() {
		return Files.exists(this.getDir());
	}

	public void remove() {
		try {
			FileUtils.deleteDirectory(this.getDir().toFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset() {
		this.setStatus(CacheStatus.IDLE);
	}

	public Path getDir() {
		if (hash == null) {
			throw new RuntimeException("Cache hash has not been set.");
		}
		return cacheDir.resolve(hash + "/");
	}

	/** Temp directory a save writes to before it is moved onto {@link #getDir()}. */
	private Path getTmpDir() {
		Path dir = getDir();
		return dir.resolveSibling(dir.getFileName() + TMP_SUFFIX);
	}

	public CacheStatus getStatus() {
		return status;
	}

	private void setStatus(CacheStatus status) {
		if (this.status != status) {
			this.status = status;
			DashLoader.LOG.info("\u001B[46m\u001B[30m DashLoader Status change {}\n\u001B[0m", status);
			this.cacheHandlers.forEach(handler -> handler.reset(this));
			// Let api consumers (other mods) react to the change as well.
			CacheStatusBus.fireStatus(status);
		}
	}
}

