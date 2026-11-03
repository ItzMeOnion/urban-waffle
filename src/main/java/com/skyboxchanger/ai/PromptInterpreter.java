package com.skyboxchanger.ai;

import com.skyboxchanger.config.SkyConfig;

import java.awt.Color;
import java.util.Locale;

/**
 * Offline fallback for the prompt box. Works with no API key.
 * Known words map to palettes; anything else gets a palette derived from the prompt text,
 * so every prompt produces a distinct sky.
 */
public final class PromptInterpreter {
    private PromptInterpreter() {}

    private record Palette(String[] keys, int top, int horizon, int cloud) {}

    private static final Palette[] PALETTES = {
            new Palette(new String[]{"thunder", "storm"}, 0xFF161B26, 0xFF46505E, 0xFF2E3442),
            new Palette(new String[]{"red", "crimson", "blood", "fire"}, 0xFF3B0008, 0xFFE0283A, 0xFFFF6B7A),
            new Palette(new String[]{"purple", "violet", "lavender", "chill"}, 0xFF2B1055, 0xFF9B6BCF, 0xFFC9A7F0),
            new Palette(new String[]{"green", "neon", "toxic", "lime"}, 0xFF001A0B, 0xFF39FF14, 0xFF9DFF7A),
            new Palette(new String[]{"sunset", "orange", "dusk"}, 0xFF2E1A47, 0xFFFF7E47, 0xFFFFB38A),
            new Palette(new String[]{"midnight", "night", "space", "galaxy"}, 0xFF02040A, 0xFF101A33, 0xFF1A2540),
            new Palette(new String[]{"ocean", "blue", "sea"}, 0xFF0B2A5B, 0xFF3A9BDC, 0xFFBFE6FF),
            new Palette(new String[]{"ice", "frozen", "winter", "snow"}, 0xFFB8D8EE, 0xFFEFF8FF, 0xFFFFFFFF),
            new Palette(new String[]{"gold", "golden", "desert"}, 0xFF7A4A10, 0xFFFFD27A, 0xFFFFF0C2),
            new Palette(new String[]{"pink", "candy", "cotton"}, 0xFF7A2A5C, 0xFFFF9ACD, 0xFFFFD6EC),
    };

    /** Applies the prompt to the config. Keeps the text fields and enabled flag as they are. */
    public static void apply(String prompt, SkyConfig c) {
        String p = prompt.toLowerCase(Locale.ROOT);
        c.lastPrompt = prompt;
        c.preset = "Custom";

        int[] pal = paletteFor(p);
        c.topColor = pal[0];
        c.horizonColor = pal[1];
        c.cloudColor = pal[2];

        if (p.contains("thunder") || p.contains("storm")) c.weather = "THUNDER";
        else if (p.contains("rain") || p.contains("drizzle")) c.weather = "RAIN";
        else if (p.contains("clear") || p.contains("sunny")) c.weather = "CLEAR";
        else c.weather = "AUTO";

        c.stars = p.contains("night") || p.contains("star") || p.contains("midnight") || p.contains("space") || p.contains("galaxy");
        c.starBrightness = c.stars ? 1.4f : 1.0f;
    }

    private static int[] paletteFor(String p) {
        for (Palette pal : PALETTES) {
            for (String k : pal.keys()) {
                if (p.contains(k)) return new int[]{pal.top(), pal.horizon(), pal.cloud()};
            }
        }
        // Unknown words: derive a stable hue from the prompt text
        float hue = (Math.abs(p.hashCode()) % 360) / 360f;
        int top = 0xFF000000 | Color.HSBtoRGB(hue, 0.75f, 0.35f);
        int horizon = 0xFF000000 | Color.HSBtoRGB(hue, 0.55f, 0.95f);
        int cloud = 0xFF000000 | Color.HSBtoRGB(hue, 0.20f, 1.0f);
        return new int[]{top, horizon, cloud};
    }
}
