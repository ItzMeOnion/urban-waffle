package com.skyboxchanger.config;

public final class ColorUtil {
    private ColorUtil() {}

    /** Linear blend of two ARGB colours. */
    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int na = Math.round(aa + (ba - aa) * t);
        int nr = Math.round(ar + (br - ar) * t);
        int ng = Math.round(ag + (bg - ag) * t);
        int nb = Math.round(ab + (bb - ab) * t);
        return (na << 24) | (nr << 16) | (ng << 8) | nb;
    }

    /** Parses "#RRGGBB", "RRGGBB" or "AARRGGBB". Returns fallback on bad input. */
    public static int parse(String s, int fallback) {
        if (s == null) return fallback;
        try {
            String h = s.trim().replace("#", "");
            if (h.length() == 6) h = "FF" + h;
            return (int) Long.parseLong(h, 16);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static String toHex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }
}
