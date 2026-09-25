package dev.quantumfusion.dashloader.forge.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import dev.quantumfusion.dashloader.forge.mixin.accessor.AndConditionAccessor;
import dev.quantumfusion.dashloader.forge.mixin.accessor.OrConditionAccessor;
import dev.quantumfusion.dashloader.forge.mixin.accessor.PropertyValueConditionAccessor;
import dev.quantumfusion.dashloader.forge.mixin.accessor.SelectorAccessor;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemTransformVec3f;
import net.minecraft.client.renderer.model.VariantList;
import net.minecraft.client.renderer.model.multipart.AndCondition;
import net.minecraft.client.renderer.model.multipart.ICondition;
import net.minecraft.client.renderer.model.multipart.OrCondition;
import net.minecraft.client.renderer.model.multipart.PropertyValueCondition;
import net.minecraft.client.renderer.model.multipart.Selector;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared Gson instance for the Forge 1.16.5 cache backend.
 *
 * <p>Modern ({@code fabric-26.3}) serializes module {@code Data} objects with
 * Hyphen (an int-pointer object registry over a binary format). Hyphen is
 * deliberately out on this toolchain (Java 8 / ForgeGradle 4, unpublished
 * {@code dashloader-core} 3.0-SNAPSHOT — see {@code PORTING_NOTES.md}), so
 * this port serializes the same {@code Data} POJOs ({@code ModelModule.Data},
 * {@code SpriteContentModule.Data}, {@code SpriteStitcherModule.Data},
 * {@code SplashModule.Data}) as JSON with Gson, which ships on the 1.16.5
 * classpath (2.8.0).
 *
 * <p>Custom adapters (all MCP names verified via {@code javap} against the
 * mapped snapshot jar):
 * <ul>
 *   <li>{@link ResourceLocation} — written as its {@code toString()}
 *       ({@code namespace:path}), read back with the single-string
 *       constructor.</li>
 *   <li>{@link ItemCameraTransforms} — the 8 {@link ItemTransformVec3f}
 *       fields ({@code thirdperson_left/right, firstperson_left/right, head,
 *       gui, ground, fixed}, each with {@code rotation/translation/scale}
 *       {@link Vector3f}s) written as float triples and rebuilt with the
 *       8-arg constructor.</li>
 *   <li>{@link Selector} — the unbaked condition tree ({@code AndCondition} /
 *       {@code OrCondition} / {@code PropertyValueCondition} /
 *       {@code TRUE} / {@code FALSE}, read via the condition accessor
 *       mixins) written as tagged JSON objects; predicates are arbitrary
 *       lambdas and are rebuilt via {@code Selector#getPredicate} on LOAD,
 *       so only the tree is stored. The part-model {@code VariantList} is
 *       likewise bake-time only (the baked part is cached separately), so a
 *       deserialized selector carries an empty one. Unknown (modded)
 *       {@code ICondition} implementations fail loudly instead of
 *       corrupting the cache.</li>
 * </ul>
 *
 * <p>{@link net.minecraft.util.Direction} (enum) and all primitive/collection
 * shapes need no adapter.
 */
public final class CacheGson {
    private CacheGson() {
    }

    public static Gson create() {
        return new GsonBuilder()
                .registerTypeAdapter(ResourceLocation.class, new ResourceLocationAdapter())
                .registerTypeAdapter(ItemCameraTransforms.class, new ItemCameraTransformsAdapter())
                .registerTypeAdapter(Selector.class, new SelectorAdapter())
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    private static final class ResourceLocationAdapter
            implements JsonSerializer<ResourceLocation>, JsonDeserializer<ResourceLocation> {
        @Override
        public JsonElement serialize(ResourceLocation src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return null;
            }
            return new JsonPrimitive(src.toString());
        }

        @Override
        public ResourceLocation deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            return new ResourceLocation(json.getAsString());
        }
    }

    private static final class ItemCameraTransformsAdapter
            implements JsonSerializer<ItemCameraTransforms>, JsonDeserializer<ItemCameraTransforms> {
        private static final String[] NAMES = {
                "thirdperson_left", "thirdperson_right",
                "firstperson_left", "firstperson_right",
                "head", "gui", "ground", "fixed"
        };

        @Override
        public JsonElement serialize(ItemCameraTransforms src, Type typeOfSrc,
                JsonSerializationContext context) {
            if (src == null) {
                return null;
            }
            JsonObject out = new JsonObject();
            ItemTransformVec3f[] parts = {
                    src.thirdperson_left, src.thirdperson_right,
                    src.firstperson_left, src.firstperson_right,
                    src.head, src.gui, src.ground, src.fixed
            };
            for (int i = 0; i < NAMES.length; i++) {
                out.add(NAMES[i], writePart(parts[i]));
            }
            return out;
        }

        @Override
        public ItemCameraTransforms deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return ItemCameraTransforms.DEFAULT;
            }
            JsonObject obj = json.getAsJsonObject();
            return new ItemCameraTransforms(
                    readPart(obj, "thirdperson_left"),
                    readPart(obj, "thirdperson_right"),
                    readPart(obj, "firstperson_left"),
                    readPart(obj, "firstperson_right"),
                    readPart(obj, "head"),
                    readPart(obj, "gui"),
                    readPart(obj, "ground"),
                    readPart(obj, "fixed"));
        }

        private static JsonObject writePart(ItemTransformVec3f part) {
            JsonObject obj = new JsonObject();
            ItemTransformVec3f safe = part == null ? ItemTransformVec3f.DEFAULT : part;
            obj.add("rotation", writeVec(safe.rotation));
            obj.add("translation", writeVec(safe.translation));
            obj.add("scale", writeVec(safe.scale));
            return obj;
        }

        private static JsonArray writeVec(Vector3f vec) {
            JsonArray arr = new JsonArray();
            arr.add(vec == null ? 0.0f : vec.getX());
            arr.add(vec == null ? 0.0f : vec.getY());
            arr.add(vec == null ? 0.0f : vec.getZ());
            return arr;
        }

        private static ItemTransformVec3f readPart(JsonObject obj, String name) {
            if (!obj.has(name) || obj.get(name).isJsonNull()) {
                return ItemTransformVec3f.DEFAULT;
            }
            JsonObject part = obj.getAsJsonObject(name);
            return new ItemTransformVec3f(
                    readVec(part, "rotation"),
                    readVec(part, "translation"),
                    readVec(part, "scale"));
        }

        private static Vector3f readVec(JsonObject obj, String name) {
            if (!obj.has(name) || obj.get(name).isJsonNull()) {
                return new Vector3f(0.0f, 0.0f, 0.0f);
            }
            JsonArray arr = obj.getAsJsonArray(name);
            return new Vector3f(
                    arr.get(0).getAsFloat(),
                    arr.get(1).getAsFloat(),
                    arr.get(2).getAsFloat());
        }
    }

    /**
     * Serializes unbaked multipart {@link Selector}s as their condition
     * tree. Only vanilla {@code ICondition} implementations are supported
     * (anything else throws loudly — a corrupt cache entry is worse than a
     * vanilla fallback).
     */
    private static final class SelectorAdapter
            implements JsonSerializer<Selector>, JsonDeserializer<Selector> {
        @Override
        public JsonElement serialize(Selector src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return null;
            }
            JsonObject out = new JsonObject();
            out.add("condition", writeCondition(((SelectorAccessor) src).getCondition()));
            return out;
        }

        @Override
        public Selector deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            ICondition condition = readCondition(json.getAsJsonObject().get("condition"));
            // The VariantList is bake-time only (the baked part model is
            // cached separately), so restored selectors carry an empty one;
            // only the condition tree is used (via getPredicate) on LOAD.
            return new Selector(condition, new VariantList(Collections.emptyList()));
        }

        private static JsonObject writeCondition(ICondition condition) {
            JsonObject out = new JsonObject();
            if (condition == null || condition == ICondition.TRUE) {
                out.addProperty("type", "true");
            } else if (condition == ICondition.FALSE) {
                out.addProperty("type", "false");
            } else if (condition instanceof PropertyValueCondition) {
                PropertyValueConditionAccessor access = (PropertyValueConditionAccessor) condition;
                out.addProperty("type", "property");
                out.addProperty("key", access.getKey());
                out.addProperty("value", access.getValue());
            } else if (condition instanceof AndCondition) {
                out.addProperty("type", "and");
                JsonArray values = new JsonArray();
                for (ICondition sub : ((AndConditionAccessor) condition).getConditions()) {
                    values.add(writeCondition(sub));
                }
                out.add("values", values);
            } else if (condition instanceof OrCondition) {
                out.addProperty("type", "or");
                JsonArray values = new JsonArray();
                for (ICondition sub : ((OrConditionAccessor) condition).getConditions()) {
                    values.add(writeCondition(sub));
                }
                out.add("values", values);
            } else {
                throw new UnsupportedOperationException(
                        "Unsupported multipart condition: " + condition.getClass().getName());
            }
            return out;
        }

        private static ICondition readCondition(JsonElement json) {
            if (json == null || json.isJsonNull()) {
                return ICondition.TRUE;
            }
            JsonObject obj = json.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "true";
            if ("true".equals(type)) {
                return ICondition.TRUE;
            } else if ("false".equals(type)) {
                return ICondition.FALSE;
            } else if ("property".equals(type)) {
                return new PropertyValueCondition(
                        obj.get("key").getAsString(), obj.get("value").getAsString());
            } else if ("and".equals(type)) {
                return new AndCondition(readConditionList(obj.getAsJsonArray("values")));
            } else if ("or".equals(type)) {
                return new OrCondition(readConditionList(obj.getAsJsonArray("values")));
            }
            throw new JsonParseException("Unknown multipart condition type: " + type);
        }

        private static List<ICondition> readConditionList(JsonArray values) {
            List<ICondition> out = new ArrayList<>(values.size());
            for (JsonElement element : values) {
                out.add(readCondition(element));
            }
            return out;
        }
    }
}
