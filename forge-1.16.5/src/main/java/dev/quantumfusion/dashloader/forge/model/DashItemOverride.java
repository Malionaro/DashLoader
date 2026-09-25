package dev.quantumfusion.dashloader.forge.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Forge 1.16.5 port of modern override caching (minimal).
 *
 * <p>One item-override entry: predicate values keyed by predicate id string
 * ({@code ResourceLocation#toString}, e.g. {@code minecraft:damaged}) plus the
 * target model id string (resolved through the staged top models, same
 * id-string scheme as weighted/multipart references). Targets that are not
 * staged top models are skipped with a warning at SAVE (vanilla fallback for
 * that override); unresolvable ids are skipped with a warning at LOAD.
 */
public final class DashItemOverride {
    /** Predicate id string ({@code ResourceLocation#toString}) to threshold value. */
    public final Map<String, Float> predicates;
    /** Target model id string (must resolve to an already-restored model on LOAD). */
    public final String model;

    public DashItemOverride(Map<String, Float> predicates, String model) {
        // LinkedHashMap for deterministic JSON.
        this.predicates = predicates == null
                ? new LinkedHashMap<String, Float>()
                : new LinkedHashMap<String, Float>(predicates);
        this.model = model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashItemOverride that = (DashItemOverride) o;
        return Objects.equals(predicates, that.predicates)
                && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        int result = predicates == null ? 0 : predicates.hashCode();
        result = 31 * result + (model == null ? 0 : model.hashCode());
        return result;
    }
}
