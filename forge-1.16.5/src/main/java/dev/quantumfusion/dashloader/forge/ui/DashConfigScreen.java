package dev.quantumfusion.dashloader.forge.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

/**
 * Forge 1.16.5 port of modern {@code ConfigScreen}
 * ({@code fabric-1.21.4}).
 *
 * <p>Adaptations:
 * <ul>
 *   <li>No ModMenu on Forge — this screen is registered as the mod's
 *       config GUI via {@code ExtensionPoint.CONFIGGUIFACTORY} in
 *       {@code DashLoaderForge} (the standard Forge 1.16.5 pattern), keeping
 *       the vanilla {@code Screen} base class like modern.</li>
 *   <li>Modern's custom {@code ConfigListWidget} (sliders/text fields) is
 *       replaced with plain toggle {@link Button}s bound to the
 *       {@link DashLoaderConfig} {@code ForgeConfigSpec} values — one row
 *       per option. Numeric options (compression, max caches) and the custom
 *       splash-line text field have no widget yet (documented TODO); the
 *       booleans cover every cache module gate.</li>
 *   <li>Buttons refresh their labels in place via
 *       {@code Widget#setMessage} (1.16.5 API, verified via {@code javap}).</li>
 * </ul>
 * @author Malionaro
 */public final class DashConfigScreen extends Screen {
    private final Screen parent;

    public DashConfigScreen(Screen parent) {
        super(new StringTextComponent("DashLoader config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = 40;
        y = addToggle(y, "Enable cache (master switch)", DashLoaderConfig.ENABLE_CACHE.get(),
                value -> DashLoaderConfig.ENABLE_CACHE.set(value));
        y = addToggle(y, "Cache models", DashLoaderConfig.CACHE_MODELS.get(),
                value -> DashLoaderConfig.CACHE_MODELS.set(value));
        y = addToggle(y, "Cache sprite contents", DashLoaderConfig.CACHE_SPRITES.get(),
                value -> DashLoaderConfig.CACHE_SPRITES.set(value));
        y = addToggle(y, "Cache sprite stitching", DashLoaderConfig.CACHE_STITCHING.get(),
                value -> DashLoaderConfig.CACHE_STITCHING.set(value));
        y = addToggle(y, "Cache splash texts", DashLoaderConfig.CACHE_SPLASHES.get(),
                value -> DashLoaderConfig.CACHE_SPLASHES.set(value));
        y = addToggle(y, "Cache fonts (unsupported, see docs)", DashLoaderConfig.CACHE_FONTS.get(),
                value -> DashLoaderConfig.CACHE_FONTS.set(value));
        y = addToggle(y, "Show caching toast", DashLoaderConfig.SHOW_CACHING_TOAST.get(),
                value -> DashLoaderConfig.SHOW_CACHING_TOAST.set(value));
        y = addToggle(y, "Debug logging", DashLoaderConfig.DEBUG.get(),
                value -> DashLoaderConfig.DEBUG.set(value));

        this.addButton(new Button(this.width / 2 - 100, this.height - 28, 200, 20,
                new StringTextComponent("Done"),
                button -> this.minecraft.displayGuiScreen(this.parent)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredString(matrices, this.font, "DashLoader config",
                this.width / 2, 15, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        DashLoaderConfig.CLIENT_SPEC.save();
    }

    private int addToggle(int y, String name, boolean current, java.util.function.Consumer<Boolean> setter) {
        final boolean[] state = {current};
        this.addButton(new Button(this.width / 2 - 150, y, 300, 20,
                new StringTextComponent(label(name, state[0])), pressed -> {
                    state[0] = !state[0];
                    setter.accept(state[0]);
                    pressed.setMessage(new StringTextComponent(label(name, state[0])));
                }));
        return y + 24;
    }

    private static String label(String name, boolean value) {
        return name + ": " + (value ? "ON" : "OFF");
    }
}
