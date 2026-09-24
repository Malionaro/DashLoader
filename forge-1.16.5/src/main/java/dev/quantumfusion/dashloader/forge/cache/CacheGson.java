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
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemTransformVec3f;
import net.minecraft.client.renderer.model.multipart.Selector;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import java.lang.reflect.Type;

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
 *   <li>{@link Selector} — explicitly unsupported: predicates are arbitrary
 *       lambdas and the unbaked condition tree ({@code ICondition}
 *       implementations) has no stable serialized form. The adapter throws
 *       loudly if ever invoked; in practice it never is because
 *       {@code ModelModule} defers multipart models to the missing list
 *       (vanilla fallback), so multipart maps are always empty. See
 *       {@code PORTING_NOTES.md}.</li>
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
                .registerTypeAdapter(Selector.class, new UnsupportedSelectorAdapter())
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
     * PERMANENT partial skip (see {@code PORTING_NOTES.md}): unbaked
     * multipart {@code Selector}s cannot be serialized on this toolchain.
     * Never invoked for empty multipart maps; fails loudly otherwise.
     */
    private static final class UnsupportedSelectorAdapter
            implements JsonSerializer<Selector>, JsonDeserializer<Selector> {
        @Override
        public JsonElement serialize(Selector src, Type typeOfSrc, JsonSerializationContext context) {
            throw new UnsupportedOperationException(
                    "Multipart Selector serialization is unsupported on 1.16.5 (see PORTING_NOTES.md).");
        }

        @Override
        public Selector deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException {
            throw new JsonParseException(
                    "Multipart Selector deserialization is unsupported on 1.16.5 (see PORTING_NOTES.md).");
        }
    }
}
