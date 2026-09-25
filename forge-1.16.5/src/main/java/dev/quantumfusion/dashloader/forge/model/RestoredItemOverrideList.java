package dev.quantumfusion.dashloader.forge.model;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverride;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link ItemOverrideList} rebuilt from cached override data.
 *
 * <p>Why a subclass instead of the vanilla constructor: the vanilla
 * 5-arg constructor needs a {@code ModelBakery} to bake override targets from
 * unbaked models. On LOAD the targets are already-restored baked models, so
 * this list holds them directly. Matching replicates vanilla
 * {@code ItemOverrideList#getOverrideModel} + package-private
 * {@code ItemOverride#matchesOverride} (threshold semantics: every predicate
 * passes when {@code actual >= required}; missing property getter fails the
 * entry), using only public APIs ({@link ItemOverride} constructor,
 * {@link ItemModelsProperties#func_239417_a_}).
 */
public final class RestoredItemOverrideList extends ItemOverrideList {
    private final List<ItemOverride> overrides;
    private final List<IBakedModel> models;

    public RestoredItemOverrideList(List<ItemOverride> overrides, List<IBakedModel> models) {
        this.overrides = new ArrayList<>(overrides);
        this.models = new ArrayList<>(models);
    }

    @Override
    public IBakedModel getOverrideModel(IBakedModel model, ItemStack stack, ClientWorld world, LivingEntity entity) {
        if (overrides.isEmpty() || stack == null) {
            return model;
        }
        for (int i = 0; i < overrides.size(); i++) {
            if (matches(overrides.get(i), stack, world, entity)) {
                IBakedModel target = i < models.size() ? models.get(i) : null;
                return target == null ? model : target;
            }
        }
        return model;
    }

    @Override
    public com.google.common.collect.ImmutableList<ItemOverride> getOverrides() {
        return com.google.common.collect.ImmutableList.copyOf(overrides);
    }

    private static boolean matches(ItemOverride override, ItemStack stack, ClientWorld world, LivingEntity entity) {
        Map<ResourceLocation, Float> predicates;
        try {
            predicates = ((dev.quantumfusion.dashloader.forge.mixin.accessor.ItemOverrideAccessor) override)
                    .getPredicateMap();
        } catch (Throwable t) {
            return false;
        }
        if (predicates == null || predicates.isEmpty()) {
            return true;
        }
        for (Map.Entry<ResourceLocation, Float> entry : predicates.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                return false;
            }
            net.minecraft.item.IItemPropertyGetter getter;
            try {
                getter = ItemModelsProperties.func_239417_a_(stack.getItem(), entry.getKey());
            } catch (Throwable t) {
                return false;
            }
            if (getter == null) {
                return false;
            }
            float actual;
            try {
                actual = getter.call(stack, world, entity);
            } catch (Throwable t) {
                return false;
            }
            if (actual < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
}
