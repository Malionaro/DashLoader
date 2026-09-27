package dev.notalpha.dashloader.client.shader;

import dev.notalpha.dashloader.DashLoader;

import dev.notalpha.dashloader.api.CachingData;
import dev.notalpha.dashloader.api.DashModule;
import dev.notalpha.dashloader.api.cache.Cache;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.api.collection.IntIntList;
import dev.notalpha.dashloader.api.collection.IntObjectList;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import dev.notalpha.dashloader.api.registry.RegistryWriter;
import dev.notalpha.dashloader.config.ConfigHandler;
import dev.notalpha.dashloader.config.Option;
import dev.notalpha.taski.builtin.StepTask;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.ShaderProgramDefinition;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;

public class ShaderModule implements DashModule<ShaderModule.Data> {
	public static final CachingData<HashMap<ShaderLoader.ShaderSourceKey, String>> SHADER_SOURCES = new CachingData<>();
	public static final CachingData<HashMap<Identifier, ShaderProgramDefinition>> SHADER_DEFINITIONS = new CachingData<>();
//	public static final CachingData<HashMap<Identifier, PostEffectPipeline>> POST_EFFECTS = new CachingData<>(); // TODO


	@Override
	public void reset(Cache cache) {
		SHADER_SOURCES.reset(cache, new HashMap<>());
		SHADER_DEFINITIONS.reset(cache, new HashMap<>());
//		POST_EFFECTS.reset(cache, new HashMap<>());
	}

	@Override
	public Data save(RegistryWriter factory, StepTask task) {
		var data1 = SHADER_SOURCES.get(CacheStatus.SAVE);
		var data2 = SHADER_DEFINITIONS.get(CacheStatus.SAVE);
//		var data3 = POST_EFFECTS.get(CacheStatus.SAVE);

		if (data1 == null || data2 == null) {
			return null;
		}

		var out = new IntObjectList<String>(new ArrayList<>(data1.size()));
		var out2 = new IntIntList(new ArrayList<>(data2.size()));
//		var out3 = new IntIntList(new ArrayList<>(data3.size()));

		data1.forEach((identifier, entry) -> {
			try {
				out.put(factory.add(identifier), entry);
			} catch (RuntimeException e) {
				DashLoader.LOG.warn("Skipping uncacheable shader {}: {}", identifier, e.getMessage());
			}
		});

		data2.forEach((identifier, entry) -> {
			// Register the value first: if it cannot be serialised, nothing is added
			// at all and the shader is compiled the normal way on the next boot.
			try {
				int entryPointer = factory.add(entry);
				out2.put(factory.add(identifier), entryPointer);
			} catch (RuntimeException e) {
				DashLoader.LOG.warn("Skipping uncacheable shader program {} ({}): {}", identifier, entry.getClass().getName(), e.getMessage());
			}
		});

//		data3.forEach((identifier, entry) -> {
//			out3.put(factory.add(identifier), factory.add(entry));
//		});

		return new Data(out, out2);
	}

	@Override
	public void load(Data data, RegistryReader reader, StepTask task) {
		var out1 = new HashMap<ShaderLoader.ShaderSourceKey, String>(data.data1.list().size());
		var out2 = new HashMap<Identifier, ShaderProgramDefinition>(data.data2.list().size());
//		var out3 = new HashMap<Identifier, PostEffectPipeline>(data.data3.list().size());
		data.data1.forEach((key, value) -> out1.put(reader.get(key), value));
		data.data2.forEach((key, value) -> out2.put(reader.get(key), reader.get(value)));
//		data.data3.forEach((key, value) -> out3.put(reader.get(key), reader.get(value)));

		SHADER_SOURCES.set(CacheStatus.LOAD, out1);
		SHADER_DEFINITIONS.set(CacheStatus.LOAD, out2);
//		POST_EFFECTS.set(CacheStatus.LOAD, out3);
	}

	@Override
	public Class<Data> getDataClass() {
		return Data.class;
	}

	@Override
	public boolean isActive() {
		return ConfigHandler.optionActive(Option.CACHE_SHADER);
	}

	public static final class Data {
		public final IntObjectList<String> data1;
		public final IntIntList data2;
//		public final IntIntList data3;

		public Data(IntObjectList<String> data1, IntIntList data2) {
			this.data1 = data1;
			this.data2 = data2;
//			this.data3 = data3;
		}
	}
}
