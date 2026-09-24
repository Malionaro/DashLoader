package dev.quantumfusion.dashloader.forge.ui.toast;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.quantumfusion.dashloader.forge.ui.Color;
import dev.quantumfusion.dashloader.forge.ui.DrawerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.ToastGui;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Forge 1.16.5 port of modern {@code DashToast} ({@code fabric-26.3}).
 *
 * <p>Yarn -&gt; MCP mapping:
 * <ul>
 *   <li>Modern {@code Toast} -&gt; MCP {@code IToast}; the 1.16.5 render
 *       entry point {@code func_230444_a_(MatrixStack, ToastGui, long)}
 *       returning {@link IToast.Visibility} folds modern's
 *       {@code update()} (progress + visibility) and
 *       {@code extractRenderState()} (drawing) into one call.</li>
 *   <li>Modern {@code GuiGraphicsExtractor} drawing -&gt; 1.16.5
 *       {@link MatrixStack} fills/text via {@link DrawerUtil}.</li>
 *   <li>Modern {@code Mth#clamp} -&gt; {@code MathHelper#clamp} (javap-verified
 *       {@code (FFF)F} on the mapped snapshot jar).</li>
 *   <li>Size is the modern 200x40 (1.16.5 {@code IToast} defaults are
 *       160x32, so width/height are overridden).</li>
 *   <li>Dedup token: default {@code getType()} ({@code NO_TOKEN}) is kept,
 *       so {@code ToastGui#getToast(DashToast.class, NO_TOKEN)} finds it.</li>
 * </ul>
 *
 * <p>Kept verbatim from modern: dark background, 125 animated background
 * lines kept inside the toast bounds (no scissor needed), progress bar +
 * glow, status text with right-aligned progress text, fun-fact line,
 * {@code FAILED_COLOR} on crash, auto-hide timing (DONE 2s, CRASHED 10s).
 * The fun facts mirror modern {@code HahaManager} (config custom lines have
 * no Forge equivalent here, so the built-in list is used directly).
 * @author Malionaro
 */public final class DashToast implements IToast {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-toast");

    private static final int PROGRESS_BAR_HEIGHT = 2;
    private static final int PADDING = 8;
    private static final int LINES = 125;

    private static final String[] FACTS = {
            "Dash was for the cool kids",
            "fun fact: 1 + 1 = 11",
            "glisco goes around and yells",
            ":froge:",
            ":bigfroge:",
            ":smolfroge:",
            "Frog + Doge = Froge",
            "Froges dad is cool",
            "Rogger Rogger!",
            "Yes commander!",
            "I am not the swarm!",
            "Get that golden strawberry!",
            "Kevin is cool.",
            "B-Sides are where I flex.",
            "Starting an accelerated backhop",
            "Gordon Freeman. I like your tie.",
            "The factory must grow.",
            "Not the biters.",
            "Ya got more red belts?",
            "I need more boilers.",
            "Throughput of circuits is gud.",
            "amogus",
            "sus",
            "imposter",
            "it was red!",
            "What does the vent button do?",
            "We need more white wine.",
            "I season my cuttingboard",
            "Do as I say, not as I do",
            "Colton is fired",
            "Was a banger on the cord.",
            "My code thinks different.",
            "Make it for 300$ sell it for 1300$",
            "Steve is almost chad",
            "IKEA is traditional.",
            "1 + 1 = 11",
            "https://ko-fi.com/notequalalpha",
            "USB-C is gud.",
            "Modrinth gud.",
            "Leocth and Alpha were first.",
            "Corn on a jakob is the best.",
            "Cornebb is cool.",
            "Hyphen is cool.",
            "DashLoader kinda banger.",
            "MFOTS was a thing.",
            ":tnypotat:",
            "418 I'm a teapot is a real error",
            "mld hrdr - leocth 2022",
            "Devin beat 7C after 5 1/2 hours",
            "Look at me, I am vibing up here",
            "Doesn't break REI",
            "Come here often?",
            "We back!",
            "No spaghetti code here..."
    };

    public final DashToastState state;
    private final Random random = new Random();
    private final String fact = FACTS[(int) (System.currentTimeMillis() % FACTS.length)];
    private List<Line> lines = new ArrayList<>();
    private long oldTime = System.currentTimeMillis();
    private float progress = 0;
    private Color progressColor = DrawerUtil.getProgressColor(progress);
    private Visibility visibility = Visibility.SHOW;
    private static boolean loggedFirstRender = false;

    public DashToast() {
        this.state = new DashToastState();
        // Create lines
        for (int i = 0; i < LINES; i++) {
            this.lines.add(new Line());
        }
    }

    @Override
    public int func_230445_a_() {
        return 200;
    }

    @Override
    public int func_238540_d_() {
        return 40;
    }

    @Override
    public Visibility func_230444_a_(MatrixStack matrices, ToastGui toastGui, long time) {
        if (!loggedFirstRender) {
            loggedFirstRender = true;
            LOGGER.info("DashToast first render, status={}", state.getStatus());
        }
        Minecraft minecraft = toastGui.getMinecraft();
        FontRenderer font = minecraft.fontRenderer;

        // Modern update(): progress + visibility.
        if (state.getStatus() == DashToastStatus.CRASHED) {
            progress = (float) this.state.getProgress();
            progressColor = DrawerUtil.FAILED_COLOR;
        } else {
            progress = (float) this.state.getProgress();
            progressColor = DrawerUtil.getProgressColor(progress);
        }

        if (state.getStatus() == DashToastStatus.CRASHED
                && System.currentTimeMillis() - state.getTimeDone() > 10000) {
            visibility = Visibility.HIDE;
        } else if (state.getStatus() == DashToastStatus.DONE
                && System.currentTimeMillis() - state.getTimeDone() > 2000) {
            visibility = Visibility.HIDE;
        } else {
            visibility = Visibility.SHOW;
        }

        // Modern extractRenderState(): drawing.
        final int width = this.func_230445_a_();
        final int height = this.func_238540_d_();
        final int barY = height - PROGRESS_BAR_HEIGHT;

        // Tick progress
        List<Line> newList = new ArrayList<>();
        List<Line> newListPrio = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        for (Line line : this.lines) {
            if (line.tick(width, height, progress, (currentTime - this.oldTime) / 17f)) {
                newListPrio.add(line);
            } else {
                newList.add(line);
            }
        }
        this.oldTime = currentTime;
        this.lines = newList;
        this.lines.addAll(newListPrio);

        // Draw the ui
        DrawerUtil.drawRect(matrices, 0, 0, width, height, DrawerUtil.BACKGROUND_COLOR);

        // Draw the background lines.
        for (Line line : lines) {
            line.draw(matrices);
        }

        // Draw progress text
        String progressText = this.state.getProgressText();
        if (progressText == null) {
            progressText = "";
        }
        int progressTextY = this.fact != null ? barY - PADDING : (barY / 2) + (font.FONT_HEIGHT / 2);
        DrawerUtil.drawText(matrices, font, PADDING, progressTextY, this.state.getText(), DrawerUtil.STATUS_COLOR);
        DrawerUtil.drawText(matrices, font, (width - PADDING) - font.getStringWidth(progressText),
                progressTextY, progressText, DrawerUtil.STATUS_COLOR);

        if (this.fact != null) {
            // Draw the fun fact
            DrawerUtil.drawText(matrices, font, PADDING, font.FONT_HEIGHT + PADDING,
                    this.fact, DrawerUtil.FOREGROUND_COLOR);
        }

        // Draw progress bar
        DrawerUtil.drawRect(matrices, 0, barY, width, PROGRESS_BAR_HEIGHT, DrawerUtil.PROGRESS_TRACK);
        DrawerUtil.drawRect(matrices, 0, barY, (int) (width * progress), PROGRESS_BAR_HEIGHT, progressColor);

        // Epic rtx graphics. aka I slapped some glow on the things.
        // Line glow
        for (Line line : lines) {
            line.drawGlow(matrices, width, height);
        }
        // Progress bar glow
        DrawerUtil.drawGlowClipped(matrices, 0, barY, (int) (width * progress), PROGRESS_BAR_HEIGHT, 0.75f,
                progressColor, true, true, true, true, 0, 0, width, height);

        return visibility;
    }

    public enum ColorKind {
        Neutral,
        Progress,
        Crashed,
    }

    private final class Line {
        public final int width;
        public final int height;
        public ColorKind colorKind;
        public float x;
        public float y;
        public float speedBoost;
        private Color color;

        public Line() {
            this.x = -1000;
            this.y = -1000;
            this.width = 30 + DashToast.this.random.nextInt(20);
            this.height = 2 + DashToast.this.random.nextInt(3);
            this.colorKind = ColorKind.Neutral;
            this.color = new Color(0xFF0000FF);
        }

        public boolean tick(int screenWidth, int screenHeight, float progress, float delta) {
            // Move the values
            this.x += (float) (speedBoost * (0.8 + (2.5 * progress))) * delta;

            // Check if not visible. Lines are kept inside the toast so no scissor is needed.
            if (x > screenWidth || x + width < 0 || x + width > screenWidth) {
                // Randomize position
                this.x = -width;
                this.y = screenHeight * DashToast.this.random.nextFloat();

                // Randomise color
                if (state.getStatus() == DashToastStatus.CRASHED) {
                    if (DashToast.this.random.nextFloat() > 0.9 || this.colorKind == ColorKind.Progress) {
                        this.colorKind = ColorKind.Crashed;
                    }
                } else {
                    if (DashToast.this.random.nextFloat() > 0.95) {
                        this.colorKind = ColorKind.Progress;
                    } else {
                        this.colorKind = ColorKind.Neutral;
                    }
                }

                // Randomise speed based on some values.
                // Weight (the size of the line), 0.2 deviation
                float weight = 1f - getWeight();
                float weightSpeed = (float) (0.7 + (weight * 0.6));

                // Kind (The type of line),
                float kindSpeed;
                if (this.colorKind == ColorKind.Neutral) {
                    kindSpeed = (float) (1.0 + (DashToast.this.random.nextFloat() * 0.2f));
                } else {
                    kindSpeed = (float) (1.0 + (DashToast.this.random.nextFloat() * 0.8f));
                }

                this.speedBoost = kindSpeed * weightSpeed;
                return this.colorKind != ColorKind.Neutral;
            }
            this.color = getColor(progress);

            return false;
        }

        public void draw(MatrixStack matrices) {
            DrawerUtil.drawRect(matrices, (int) x, (int) y, width, height, color);
        }

        public void drawGlow(MatrixStack matrices, int clipWidth, int clipHeight) {
            if (this.colorKind != ColorKind.Neutral) {
                DrawerUtil.drawGlowClipped(matrices, x, y, width, height, (getWeight() + 2.0f) / 3.0f,
                        this.color, false, true, false, true, 0, 0, clipWidth, clipHeight);
            }
        }

        public Color getColor(double progress) {
            Color color;
            switch (this.colorKind) {
                case Neutral:
                    color = DrawerUtil.NEUTRAL_LINE;
                    break;
                case Progress:
                    if (state.getStatus() == DashToastStatus.CRASHED) {
                        color = DrawerUtil.FAILED_COLOR;
                    } else {
                        color = DrawerUtil.getProgressColor(progress);
                    }
                    break;
                case Crashed:
                    color = DrawerUtil.FAILED_COLOR;
                    break;
                default:
                    color = DrawerUtil.NEUTRAL_LINE;
                    break;
            }

            return DrawerUtil.withOpacity(color, MathHelper.clamp(((this.x) / (this.width)), 0.0f, 1.0f));
        }

        public float getWeight() {
            return ((this.width * (float) this.height) - 60f) / 190f;
        }
    }
}
