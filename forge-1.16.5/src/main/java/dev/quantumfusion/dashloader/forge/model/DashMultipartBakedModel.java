package dev.quantumfusion.dashloader.forge.model;

import dev.quantumfusion.dashloader.forge.mixin.accessor.MultipartBakedModelAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import net.minecraft.client.renderer.model.multipart.Selector;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Forge 1.16.5 port of modern {@code DashMultipartBakedModel}
 * ({@code fabric-1.21.4}).
 *
 * <p>Yarn -&gt; MCP mapping:
 * <ul>
 *   <li>yarn {@code MultipartBakedModel} -&gt; MCP
 *       {@code MultipartBakedModel} (same name). Note the constructor takes
 *       {@code List<Pair<Predicate<BlockState>, IBakedModel>>} directly —
 *       there is no {@code Selector} wrapper record like in modern.</li>
 *   <li>Modern {@code MultipartModelSelector} -&gt; MCP
 *       {@code net.minecraft.client.renderer.model.multipart.Selector}
 *       (unbaked, parsed from the blockstate JSON) with
 *       {@code getPredicate(StateContainer)} rebuilding the predicate —
 *       exactly the round-trip the modern code performs with its
 *       state-manager id + selector pair.</li>
 * </ul>
 *
 * <p>Each component stores:
 * <ul>
 *   <li>{@code model} — model-id string for the cache (Hyphen int pointers
 *       are out of scope — see {@code DashCacheBackend}). Resolution
 *       to/from {@link IBakedModel} goes through caller-supplied lookup
 *       functions.</li>
 *   <li>{@code selector} — the unbaked vanilla {@link Selector}; predicates
 *       ({@code Predicate<BlockState>}) are arbitrary lambdas and cannot be
 *       serialized, so they are rebuilt from the selector on load.</li>
 *   <li>{@code stateOwner} — id of the block owning the state container,
 *       mirroring modern per-component identifier storage.</li>
 * </ul>
 *
 * <p>Saving needs the unbaked {@link Selector} list alongside the baked
 * model (same requirement as modern, which keeps the
 * {@code MULTIPART_PREDICATES} map for this). Wiring that map to the 1.16.5
 * baking path is a documented TODO in {@link ModelModule}.
 */
public final class DashMultipartBakedModel {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-model");

    public final List<Component> components;

    public DashMultipartBakedModel(List<Component> components) {
        this.components = components;
    }

    /**
     * Snapshot a baked multipart model.
     *
     * @param baked baked model (selectors read via
     *        {@link MultipartBakedModelAccessor})
     * @param selectors unbaked selectors in the same order as the baked
     *        selector list
     * @param stateOwner id of the block owning the state container
     * @param modelIds maps each baked part model to its model-id string
     */
    public static DashMultipartBakedModel toDash(MultipartBakedModel baked,
            List<Selector> selectors, ResourceLocation stateOwner,
            Function<IBakedModel, String> modelIds) {
        List<Pair<Predicate<BlockState>, IBakedModel>> bakedSelectors =
                ((MultipartBakedModelAccessor) baked).getSelectors();
        if (bakedSelectors.size() != selectors.size()) {
            LOGGER.warn("Multipart selector count mismatch (baked={} unbaked={}), caching skipped for this model.",
                    bakedSelectors.size(), selectors.size());
            return new DashMultipartBakedModel(new ArrayList<Component>());
        }
        List<Component> out = new ArrayList<>(bakedSelectors.size());
        for (int i = 0; i < bakedSelectors.size(); i++) {
            out.add(new Component(
                    modelIds.apply(bakedSelectors.get(i).getRight()),
                    selectors.get(i),
                    stateOwner));
        }
        return new DashMultipartBakedModel(out);
    }

    /**
     * Rebuild a baked multipart model.
     *
     * @param models resolves model-id strings to (already restored) models
     * @param containers resolves block ids to state containers for
     *        predicate rebuilding
     */
    public MultipartBakedModel toVanilla(Function<String, IBakedModel> models,
            Function<ResourceLocation, StateContainer<?, ?>> containers) {
        List<Pair<Predicate<BlockState>, IBakedModel>> out =
                new ArrayList<>(components.size());
        for (Component component : components) {
            IBakedModel model = models.apply(component.model);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Predicate<BlockState> predicate =
                    component.selector.getPredicate((StateContainer) containers.apply(component.stateOwner));
            out.add(Pair.of(predicate, model));
        }
        return new MultipartBakedModel(out);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashMultipartBakedModel that = (DashMultipartBakedModel) o;
        return Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return components == null ? 0 : components.hashCode();
    }

    /** Serializable component: model-id string + unbaked selector + owner id. */
    public static final class Component {
        public final String model;
        /** Runtime unbaked object — serialization of selectors is a cache-backend TODO. */
        public final Selector selector;
        public final ResourceLocation stateOwner;

        public Component(String model, Selector selector, ResourceLocation stateOwner) {
            this.model = model;
            this.selector = selector;
            this.stateOwner = stateOwner;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Component component = (Component) o;
            return Objects.equals(model, component.model)
                    && Objects.equals(selector, component.selector)
                    && Objects.equals(stateOwner, component.stateOwner);
        }

        @Override
        public int hashCode() {
            int result = model == null ? 0 : model.hashCode();
            result = 31 * result + (selector == null ? 0 : selector.hashCode());
            result = 31 * result + (stateOwner == null ? 0 : stateOwner.hashCode());
            return result;
        }
    }
}
