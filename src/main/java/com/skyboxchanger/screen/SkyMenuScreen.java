package com.skyboxchanger.screen;

import com.google.gson.JsonObject;
import com.skyboxchanger.ai.AiSkyboxClient;
import com.skyboxchanger.ai.PromptInterpreter;
import com.skyboxchanger.config.ColorUtil;
import com.skyboxchanger.config.SkyConfig;
import com.skyboxchanger.config.SkyText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Locale;

/** The SkyBox Changer menu. Changes apply live; settings save when you close it. */
public class SkyMenuScreen extends Screen {

    private enum Tab {
        PROMPT("AI Prompt"), PRESETS("Presets"), COLOURS("Colours"), TEXT("Sky Text"), WEATHER("Weather");
        final String label;
        Tab(String label) { this.label = label; }
    }

    private static final int TAB_W = 92;
    private static final int ROW_H = 24;

    private final Screen parent;
    private Tab tab = Tab.PROMPT;
    private String status = "Describe any sky and press Generate.";
    private int statusColor = 0xFFBFD7FF;
    private int animTick = 0;

    // Widgets reused across tabs
    private TextFieldWidget promptField, topField, horizonField, cloudField;
    private TextFieldWidget textField, colourField, scaleField, azField, elField;
    private int fontIndex = 0;
    private boolean newShadow = true;

    public SkyMenuScreen(Screen parent) {
        super(Text.literal("SkyBox Changer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - (Tab.values().length * TAB_W) / 2;
        int x = left;
        for (Tab t : Tab.values()) {
            final Tab target = t;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(t == tab ? "> " + t.label + " <" : t.label),
                    b -> { tab = target; clearAndInit(); })
                    .dimensions(x, 30, TAB_W - 4, 20).build());
            x += TAB_W;
        }
        int top = 62;
        switch (tab) {
            case PROMPT -> buildPrompt(top);
            case PRESETS -> buildPresets(top);
            case COLOURS -> buildColours(top);
            case TEXT -> buildText(top);
            case WEATHER -> buildWeather(top);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(width / 2 - 60, height - 28, 120, 20).build());
    }

    // ---------- Tabs ----------

    private void buildPrompt(int y) {
        SkyConfig c = SkyConfig.INSTANCE;
        promptField = addField(width / 2 - 170, y, 340, c.lastPrompt, 200);
        promptField.setPlaceholder(Text.literal("e.g. thunderstorm over a red desert at dusk"));
        addDrawableChild(ButtonWidget.builder(Text.literal("Generate Sky"), b -> generate())
                .dimensions(width / 2 - 60, y + 30, 120, 20).build());
    }

    private void buildPresets(int y) {
        SkyConfig c = SkyConfig.INSTANCE;
        int col = 0;
        for (String name : SkyConfig.PRESETS) {
            int bx = width / 2 - 170 + (col % 2) * 175;
            int by = y + (col / 2) * 26;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(name.equals(c.preset) ? "* " + name : name),
                    b -> { c.applyPreset(name); status = "Applied " + name + "."; statusColor = 0xFFBFD7FF; clearAndInit(); })
                    .dimensions(bx, by, 165, 20).build());
            col++;
        }
    }

    private void buildColours(int y) {
        SkyConfig c = SkyConfig.INSTANCE;
        topField = addLabelledField(width / 2 - 170, y, "Zenith (top)", ColorUtil.toHex(c.topColor));
        horizonField = addLabelledField(width / 2 - 170, y + 26, "Horizon", ColorUtil.toHex(c.horizonColor));
        cloudField = addLabelledField(width / 2 - 170, y + 52, "Clouds", ColorUtil.toHex(c.cloudColor));
        addDrawableChild(ButtonWidget.builder(Text.literal("Apply Colours"), b -> {
            c.topColor = ColorUtil.parse(topField.getText(), c.topColor);
            c.horizonColor = ColorUtil.parse(horizonField.getText(), c.horizonColor);
            c.cloudColor = ColorUtil.parse(cloudField.getText(), c.cloudColor);
            c.preset = "Custom";
            status = "Colours applied.";
            statusColor = 0xFFBFD7FF;
        }).dimensions(width / 2 - 60, y + 84, 120, 20).build());
    }

    private void buildText(int y) {
        SkyConfig c = SkyConfig.INSTANCE;
        textField = addLabelledField(width / 2 - 170, y, "Text", "Hello sky");
        colourField = addLabelledField(width / 2 - 170, y + 26, "Colour", "#FFFFFF");
        scaleField = addLabelledField(width / 2 - 170, y + 52, "Scale (1-6)", "2");
        azField = addLabelledField(width / 2 - 170, y + 78, "Direction (0-360)", "0");
        elField = addLabelledField(width / 2 - 170, y + 104, "Height (-20 to 80)", "30");

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Font: " + SkyConfig.FONTS.get(fontIndex).replace("minecraft:", "")),
                b -> { fontIndex = (fontIndex + 1) % SkyConfig.FONTS.size(); clearAndInit(); })
                .dimensions(width / 2 + 10, y + 130, 160, 20).build());
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Shadow: " + (newShadow ? "on" : "off")),
                b -> { newShadow = !newShadow; clearAndInit(); })
                .dimensions(width / 2 - 170, y + 130, 160, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Add to Sky"), b -> addText())
                .dimensions(width / 2 - 170, y + 156, 165, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), b -> {
            c.texts.clear();
            status = "All sky text removed.";
            statusColor = 0xFFBFD7FF;
            clearAndInit();
        }).dimensions(width / 2 + 5, y + 156, 165, 20).build());

        // Existing entries, each with a remove button
        int listY = y + 186;
        for (int i = 0; i < Math.min(c.texts.size(), 6); i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> {
                c.texts.remove(idx);
                clearAndInit();
            }).dimensions(width / 2 + 80, listY + i * 20, 90, 18).build());
        }
    }

    private void buildWeather(int y) {
        SkyConfig c = SkyConfig.INSTANCE;
        addDrawableChild(ButtonWidget.builder(Text.literal("Weather: " + c.weather), b -> {
            int i = SkyConfig.WEATHERS.indexOf(c.weather);
            c.weather = SkyConfig.WEATHERS.get((i + 1) % SkyConfig.WEATHERS.size());
            c.preset = "Custom";
            clearAndInit();
        }).dimensions(width / 2 - 85, y, 170, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Stars: " + (c.stars ? "on" : "off")), b -> {
            c.stars = !c.stars;
            clearAndInit();
        }).dimensions(width / 2 - 85, y + 26, 170, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Brighter stars"), b -> {
            c.starBrightness = Math.min(3f, c.starBrightness + 0.25f);
            status = "Star brightness " + String.format(Locale.ROOT, "%.2f", c.starBrightness);
            statusColor = 0xFFBFD7FF;
        }).dimensions(width / 2 - 85, y + 52, 83, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Dimmer stars"), b -> {
            c.starBrightness = Math.max(0f, c.starBrightness - 0.25f);
            status = "Star brightness " + String.format(Locale.ROOT, "%.2f", c.starBrightness);
            statusColor = 0xFFBFD7FF;
        }).dimensions(width / 2 + 2, y + 52, 83, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sky mod: " + (c.enabled ? "ON" : "OFF")), b -> {
            c.enabled = !c.enabled;
            clearAndInit();
        }).dimensions(width / 2 - 85, y + 84, 170, 20).build());
    }

    // ---------- Actions ----------

    private void generate() {
        SkyConfig c = SkyConfig.INSTANCE;
        String prompt = promptField.getText().trim();
        if (prompt.isEmpty()) {
            setStatus("Type a prompt first.", 0xFFFF8080);
            return;
        }
        if (c.apiKey == null || c.apiKey.isBlank()) {
            PromptInterpreter.apply(prompt, c);
            setStatus("Built locally from your prompt. Add an API key in config for AI designs.", 0xFFBFD7FF);
            clearAndInit();
            return;
        }
        setStatus("AI is designing your sky...", 0xFFFFE08A);
        AiSkyboxClient.generate(prompt, c).whenComplete((json, err) ->
                MinecraftClient.getInstance().execute(() -> {
                    if (err != null) {
                        setStatus("AI failed: " + err.getMessage(), 0xFFFF8080);
                    } else {
                        AiSkyboxClient.apply(json, prompt, c);
                        setStatus("Sky updated by AI.", 0xFF8CFFA0);
                        clearAndInit();
                    }
                }));
    }

    private void addText() {
        SkyConfig c = SkyConfig.INSTANCE;
        String txt = textField.getText().trim();
        if (txt.isEmpty()) {
            setStatus("Type some text first.", 0xFFFF8080);
            return;
        }
        float scale = clamp(parseFloat(scaleField.getText(), 2f), 1f, 6f);
        float az = ((parseFloat(azField.getText(), 0f) % 360f) + 360f) % 360f;
        float el = clamp(parseFloat(elField.getText(), 30f), -20f, 80f);
        int colour = ColorUtil.parse(colourField.getText(), 0xFFFFFFFF);
        c.texts.add(new SkyText(txt, SkyConfig.FONTS.get(fontIndex), colour, scale, az, el, newShadow));
        setStatus("Added \"" + txt + "\" to the sky.", 0xFF8CFFA0);
        clearAndInit();
    }

    private void setStatus(String msg, int colour) {
        status = msg;
        statusColor = colour;
    }

    // ---------- Helpers ----------

    private TextFieldWidget addField(int x, int y, int w, String value, int max) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(""));
        f.setMaxLength(max);
        f.setText(value == null ? "" : value);
        return addDrawableChild(f);
    }

    private TextFieldWidget addLabelledField(int x, int y, String label, String value) {
        return addField(x + 110, y, 220, value, 64);
    }

    private static float parseFloat(String s, float fallback) {
        try { return Float.parseFloat(s.trim()); } catch (Exception e) { return fallback; }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---------- Rendering ----------

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        animTick++;
        // Animated night-sky backdrop so the menu reads as a sky in itself
        int top = ColorUtil.lerp(0xFF0A0F24, 0xFF2B1055, (float) (0.5 + 0.5 * Math.sin(animTick / 80.0)));
        int bottom = ColorUtil.lerp(0xFF1B2A4A, 0xFF5A2A6E, (float) (0.5 + 0.5 * Math.cos(animTick / 110.0)));
        ctx.fillGradient(0, 0, width, height, top, bottom);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        SkyConfig c = SkyConfig.INSTANCE;

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("SKYBOX CHANGER"), width / 2, 10, 0xFFFFFFFF);

        // Live preview strip
        int pw = 160;
        ctx.fillGradient(width / 2 - pw - 10, 52, width / 2 - 10, 58, c.topColor, c.horizonColor);
        ctx.fillGradient(width / 2 + 10, 52, width / 2 + pw + 10, 58, c.horizonColor, c.cloudColor);

        int y = 62;
        switch (tab) {
            case PROMPT -> {
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Describe any sky, e.g. \"neon green city at night\""),
                        width / 2, y - 12, 0xFFB8C7E0);
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, y + 60, statusColor);
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal("Current: " + c.preset + " | Weather: " + c.weather),
                        width / 2, y + 80, 0xFF9FB3D1);
            }
            case PRESETS -> ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, y + 120, statusColor);
            case COLOURS -> ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Hex colours, e.g. #FF3366"), width / 2, y + 112, 0xFF9FB3D1);
            case TEXT -> {
                int listY = y + 186;
                ctx.drawTextWithShadow(textRenderer, Text.literal("Sky text (" + c.texts.size() + "/6 shown):"),
                        width / 2 - 170, listY - 10, 0xFFFFFFFF);
                for (int i = 0; i < Math.min(c.texts.size(), 6); i++) {
                    SkyText t = c.texts.get(i);
                    ctx.drawTextWithShadow(textRenderer,
                            Text.literal((i + 1) + ". " + t.text + "  (" + ColorUtil.toHex(t.color) + ", x" + t.scale + ")"),
                            width / 2 - 170, listY + i * 20 + 4, t.color);
                }
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, height - 46, statusColor);
            }
            case WEATHER -> ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Visual only. Nobody else sees your weather."), width / 2, y + 112, 0xFF9FB3D1);
        }
    }

    @Override
    public void close() {
        SkyConfig.INSTANCE.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
