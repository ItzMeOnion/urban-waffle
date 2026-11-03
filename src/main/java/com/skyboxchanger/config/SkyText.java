package com.skyboxchanger.config;

/** One piece of text floating in the sky. Position is a direction: azimuth (0 = north, clockwise) and elevation (up/down). */
public class SkyText {
    public String text = "Hello";
    public String font = "minecraft:default";
    public int color = 0xFFFFFFFF;
    public float scale = 2.0f;
    public float azimuth = 0f;
    public float elevation = 30f;
    public boolean shadow = true;

    public SkyText() {}

    public SkyText(String text, String font, int color, float scale, float azimuth, float elevation, boolean shadow) {
        this.text = text;
        this.font = font;
        this.color = color;
        this.scale = scale;
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.shadow = shadow;
    }
}
