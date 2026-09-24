package dev.quantumfusion.dashloader.forge.model;

import dev.quantumfusion.dashloader.forge.mixin.accessor.WeightedBakedModelAccessor;
import dev.quantumfusion.dashloader.forge.mixin.accessor.WeightedModelAccessor;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Forge 1.16.5 port of modern {@code DashWeightedBakedModel}
 * ({@code fabric-1.21.4}).
 *
 * <p>Yarn -&gt; MCP mapping: yarn {@code WeightedBakedModel} -&gt; MCP
 * {@code WeightedBakedModel} (same name). Entries are read through
 * {@link WeightedBakedModelAccessor} + {@link WeightedModelAccessor}
 * (mirroring modern {@code WeightedBakedModelAccessor}); field names
 * ({@code models}, {@code model}, {@code itemWeight}) verified via
 * {@code javap} against the mapped Forge jar.
 *
 * <p>1.16.5 quirk: the entry class
 * ({@code WeightedBakedModel$WeightedModel}) is package-private, so
 * {@link #toVanilla} constructs entries reflectively
 * ({@code setAccessible}) with a clear failure if that ever breaks. Each
 * entry stores the model as a model-id string resolved through
 * caller-supplied lookups (Hyphen int pointers are out of scope — see
 * {@code DashCacheBackend}).
 */
public final class DashWeightedBakedModel {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-model");

    public final List<Entry> entries;

    public DashWeightedBakedModel(List<Entry> entries) {
        this.entries = entries;
    }

    /**
     * Snapshot a baked weighted model.
     *
     * @param modelIds maps each entry model to its model-id string
     */
    public static DashWeightedBakedModel toDash(WeightedBakedModel model,
            Function<IBakedModel, String> modelIds) {
        List<?> raw = ((WeightedBakedModelAccessor) model).getModels();
        List<Entry> out = new ArrayList<>(raw.size());
        for (Object entry : raw) {
            WeightedModelAccessor access = (WeightedModelAccessor) entry;
            out.add(new Entry(modelIds.apply(access.getModel()), access.getItemWeight()));
        }
        return new DashWeightedBakedModel(out);
    }

    /**
     * Rebuild a baked weighted model.
     *
     * @param models resolves model-id strings to (already restored) models
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public WeightedBakedModel toVanilla(Function<String, IBakedModel> models) {
        try {
            Class<?> entryClass = Class.forName(
                    "net.minecraft.client.renderer.model.WeightedBakedModel$WeightedModel");
            Constructor<?> ctor = entryClass.getDeclaredConstructor(IBakedModel.class, int.class);
            ctor.setAccessible(true);
            // Raw list on purpose: the entry class is package-private in
            // net.minecraft.client.renderer.model and cannot be named here.
            List built = new ArrayList(entries.size());
            for (Entry entry : entries) {
                built.add(ctor.newInstance(models.apply(entry.model), entry.weight));
            }
            return new WeightedBakedModel(built);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Could not construct WeightedBakedModel$WeightedModel entries", e);
            throw new IllegalStateException("Weighted model restore failed (see log)", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashWeightedBakedModel that = (DashWeightedBakedModel) o;
        return Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return entries == null ? 0 : entries.hashCode();
    }

    /** Serializable entry: model-id string + weight. */
    public static final class Entry {
        public final String model;
        public final int weight;

        public Entry(String model, int weight) {
            this.model = model;
            this.weight = weight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return weight == entry.weight && Objects.equals(model, entry.model);
        }

        @Override
        public int hashCode() {
            int result = model == null ? 0 : model.hashCode();
            result = 31 * result + weight;
            return result;
        }
    }
}
