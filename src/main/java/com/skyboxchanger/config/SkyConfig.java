package com.skyboxchanger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** All SkyBox Changer settings. Saved to config/skyboxchanger.json. Client-side only. */
public class SkyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skyboxchanger.json");

    public static final List<String> PRESETS = List.of(
            "Default", "Thunderstorm", "Cool Red", "Chill Purple", "Neon Green",
            "Sunset", "Midnight", "Rainy");
    public static final List<String> WEATHERS = List.of("AUTO", "CLEAR", "RAIN", "THUNDER");
    public static final List<String> FONTS = List.of(
            "minecraft:default", "minecraft:alt", "minecraft:uniform", "minecraft:illageralt");

    public static SkyConfig INSTANCE = load();

    public boolean enabled = true;
    public String preset = "Default";
    public int topColor = 0xFF3A7BD5;
    public int horizonColor = 0xFF9FD8FF;
    public int cloudColor = 0xFFFFFFFF;
    public boolean stars = true;
    public float starBrightness = 1.0f;
    public String weather = "AUTO";
    public String lastPrompt = "";
    public String apiKey = "";
    public String apiModel = "claude-haiku-5-5";
    public List<SkyText> texts = new ArrayList<>();

    public static SkyConfig load() {
        try {
            if (Files.exists(FILE)) {
                SkyConfig c = GSON.fromJson(Files.readString(FILE), SkyConfig.class);
                if (c != null) {
                    if (c.texts == null) c.texts = new ArrayList<>();
                    return c;
                }
            }
        } catch (Exception e) {
            System.err.println("[SkyBoxChanger] Could not read config, using defaults: " + e.getMessage());
        }
        return new SkyConfig();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[SkyBoxChanger] Could not save config: " + e.getMessage());
        }
    }

    public void applyPreset(String name) {
        preset = name;
        stars = true;
        starBrightness = 1.0f;
        weather = "AUTO";
        switch (name) {
            case "Thunderstorm" -> { topColor = 0xFF161B26; horizonColor = 0xFF46505E; cloudColor = 0xFF2E3442; stars = false; weather = "THUNDER"; }
            case "Cool Red"     -> { topColor = 0xFF3B0008; horizonColor = 0xFFE0283A; cloudColor = 0xFFFF6B7A; starBrightness = 0.6f; }
            case "Chill Purple" -> { topColor = 0xFF2B1055; horizonColor = 0xFF9B6BCF; cloudColor = 0xFFC9A7F0; }
            case "Neon Green"   -> { topColor = 0xFF001A0B; horizonColor = 0xFF39FF14; cloudColor = 0xFF9DFF7A; starBrightness = 1.5f; }
            case "Sunset"       -> { topColor = 0xFF2E1A47; horizonColor = 0xFFFF7E47; cloudColor = 0xFFFFB38A; }
            case "Midnight"     -> { topColor = 0xFF02040A; horizonColor = 0xFF101A33; cloudColor = 0xFF1A2540; starBrightness = 1.8f; }
            case "Rainy"        -> { topColor = 0xFF2F3A46; horizonColor = 0xFF7F8C99; cloudColor = 0xFF59636E; stars = false; weather = "RAIN"; }
            default -> { preset = "Default"; topColor = 0xFF3A7BD5; horizonColor = 0xFF9FD8FF; cloudColor = 0xFFFFFFFF; }
        }
    }
}
