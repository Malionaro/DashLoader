package dev.quantumfusion.dashloader.forge.sprite.stitch;

import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Forge 1.16.5 port of modern {@code DashTextureStitcher}
 * ({@code fabric-1.21.4}).
 *
 * <p>Yarn -&gt; MCP mapping:
 * <ul>
 *   <li>Modern {@code TextureStitcher<T extends Stitchable>} -&gt; 1.16.5
 *       {@code Stitcher} (concrete, stitch items are always
 *       {@link TextureAtlasSprite.Info}). The generic parameter disappears;
 *       slots are keyed by {@link ResourceLocation}
 *       ({@code info.getSpriteLocation()}).</li>
 *   <li>{@code add(info)} -&gt; {@code addSprite(info)},
 *       {@code stitch()} -&gt; {@code doStitch()},
 *       {@code getWidth()/getHeight()} -&gt;
 *       {@code getCurrentWidth()/getCurrentHeight()},
 *       {@code getStitchedSprites(consumer)} -&gt;
 *       {@code getStitchSlots(ISpriteLoader)} whose loader is
 *       {@code load(info, x, y, width, height)}.</li>
 * </ul>
 *
 * <p>Same cached-packing logic as modern: when {@link ExportedData} is
 * present, re-added sprites are matched against cached slots (dimension
 * changes or unknown sprites trigger {@link #doFallback()} to vanilla
 * stitching); {@link #doStitch()} falls back if not every cached slot was
 * re-requested. With no cached data every method delegates to
 * {@code super}.
 * @author Malionaro
 */public class DashTextureStitcher extends Stitcher {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-stitch");

    private ExportedData data;
    private int remainingSlots;

    public DashTextureStitcher(int maxWidth, int maxHeight, int mipLevel, ExportedData data) {
        super(maxWidth, maxHeight, mipLevel);
        this.data = data;
        this.remainingSlots = data == null ? 0 : data.slots.size();
    }

    @Override
    public int getCurrentWidth() {
        if (data == null) {
            return super.getCurrentWidth();
        }
        return data.width;
    }

    @Override
    public int getCurrentHeight() {
        if (data == null) {
            return super.getCurrentHeight();
        }
        return data.height;
    }

    @Override
    public void addSprite(TextureAtlasSprite.Info info) {
        if (data == null) {
            super.addSprite(info);
            return;
        }
        ResourceLocation id = info.getSpriteLocation();
        Slot slot = data.slots.get(id);
        if (slot == null) {
            LOGGER.warn("Sprite {} was not cached last time.", id);
            doFallback();
            this.addSprite(info);
            return;
        }
        if (slot.contents != null) {
            LOGGER.warn("Sprite {} was added twice?", id);
        }
        remainingSlots -= 1;
        slot.contents = info;
        if (slot.width != info.getSpriteWidth() || slot.height != info.getSpriteHeight()) {
            LOGGER.warn("Sprite {} changed dimensions since last launch, falling back.", id);
            doFallback();
        }
    }

    public void doFallback() {
        if (data != null) {
            LOGGER.error("Using fallback on texture stitcher.");
            Map<ResourceLocation, Slot> slots = data.slots;
            data = null;
            for (Map.Entry<ResourceLocation, Slot> entry : slots.entrySet()) {
                if (entry.getValue().contents != null) {
                    this.addSprite(entry.getValue().contents);
                }
            }
        } else {
            LOGGER.error("Tried to fallback stitcher twice.");
        }
    }

    @Override
    public void doStitch() {
        if (data != null && remainingSlots != 0) {
            LOGGER.warn("Remaining slots did not match the cached amount, falling back.");
            for (Map.Entry<ResourceLocation, Slot> entry : data.slots.entrySet()) {
                if (entry.getValue().contents == null) {
                    LOGGER.error("Sprite {} was not requested", entry.getKey());
                }
            }
            doFallback();
        }
        if (data == null) {
            super.doStitch();
        }
    }

    @Override
    public void getStitchSlots(Stitcher.ISpriteLoader loader) {
        if (data == null) {
            super.getStitchSlots(loader);
        } else {
            for (Slot slot : data.slots.values()) {
                loader.load(slot.contents, slot.x, slot.y, slot.width, slot.height);
            }
        }
    }

    /** Mutable load-time view of a cached slot (contents filled in during {@code addSprite}). */
    public static final class Slot {
        public TextureAtlasSprite.Info contents;
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public Slot(TextureAtlasSprite.Info contents, int x, int y, int width, int height) {
            this.contents = contents;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /** Serializable snapshot of one stitch run. */
    public static final class Data {
        public final Map<String, DashTextureSlot> slots;
        public final int width;
        public final int height;
        public final int maxWidth;
        public final int maxHeight;
        public final int mipLevel;

        public Data(Map<String, DashTextureSlot> slots, int width, int height,
                int maxWidth, int maxHeight, int mipLevel) {
            this.slots = slots;
            this.width = width;
            this.height = height;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.mipLevel = mipLevel;
        }

        /** Capture slot positions from a finished stitcher. */
        public static Data capture(Stitcher stitcher, int maxWidth, int maxHeight, int mipLevel) {
            final Map<String, DashTextureSlot> slots = new LinkedHashMap<>();
            final int width = stitcher.getCurrentWidth();
            final int height = stitcher.getCurrentHeight();
            stitcher.getStitchSlots((info, x, y, w, h) ->
                    slots.put(info.getSpriteLocation().toString(), new DashTextureSlot(x, y, w, h)));
            return new Data(slots, width, height, maxWidth, maxHeight, mipLevel);
        }

        public ExportedData export() {
            Map<ResourceLocation, Slot> out = new LinkedHashMap<>(slots.size());
            for (Map.Entry<String, DashTextureSlot> entry : slots.entrySet()) {
                DashTextureSlot slot = entry.getValue();
                out.put(new ResourceLocation(entry.getKey()),
                        new Slot(null, slot.x, slot.y, slot.width, slot.height));
            }
            return new ExportedData(out, width, height, maxWidth, maxHeight, mipLevel);
        }
    }

    /** Load-time data: cached packing reused when stitch parameters match. */
    public static final class ExportedData {
        public final Map<ResourceLocation, Slot> slots;
        public final int width;
        public final int height;
        public final int maxWidth;
        public final int maxHeight;
        public final int mipLevel;

        public ExportedData(Map<ResourceLocation, Slot> slots, int width, int height,
                int maxWidth, int maxHeight, int mipLevel) {
            this.slots = slots;
            this.width = width;
            this.height = height;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.mipLevel = mipLevel;
        }

        /** Cached packing is reusable only with identical stitch parameters. */
        public boolean matches(int maxWidth, int maxHeight, int mipLevel) {
            return this.maxWidth == maxWidth
                    && this.maxHeight == maxHeight
                    && this.mipLevel == mipLevel;
        }
    }
}
