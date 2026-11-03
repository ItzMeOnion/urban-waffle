package com.skyboxchanger.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyboxchanger.config.ColorUtil;
import com.skyboxchanger.config.SkyConfig;
import com.skyboxchanger.config.SkyText;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Sends the prompt to an AI model and gets back a sky design as JSON.
 * The AI chooses colours, weather, stars and floating text. It does not paint an image.
 * Requires the user's own API key, stored in config/skyboxchanger.json.
 */
public final class AiSkyboxClient {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static final String SYSTEM_PROMPT = """
            You design Minecraft sky settings. Reply with ONLY one JSON object, no prose, no code fences.
            Keys:
            "topColor": "#RRGGBB" (zenith colour)
            "horizonColor": "#RRGGBB" (colour near the horizon)
            "cloudColor": "#RRGGBB"
            "stars": true or false
            "starBrightness": number from 0 to 3
            "weather": one of "AUTO", "CLEAR", "RAIN", "THUNDER"
            "texts": array of up to 3 objects with "text" (short string), "color" ("#RRGGBB"),
                     "scale" (1 to 6), "azimuth" (0 to 360), "elevation" (-20 to 80), "shadow" (boolean)
            Match the user's description. Make it visually striking. Texts are optional; use [] if none fit.
            """;

    private AiSkyboxClient() {}

    /** Calls the model and completes with the parsed JSON. Apply it on the game thread with {@link #apply}. */
    public static CompletableFuture<JsonObject> generate(String prompt, SkyConfig c) {
        JsonObject body = new JsonObject();
        body.addProperty("model", c.apiModel);
        body.addProperty("max_tokens", 900);
        body.addProperty("system", SYSTEM_PROMPT);
        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt);
        messages.add(msg);
        body.add("messages", messages);

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                .timeout(Duration.ofSeconds(60))
                .header("content-type", "application/json")
                .header("x-api-key", c.apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
            if (resp.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + resp.statusCode() + ": " + shorten(resp.body()));
            }
            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            String text = root.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start < 0 || end <= start) throw new IllegalStateException("AI reply had no JSON");
            return JsonParser.parseString(text.substring(start, end + 1)).getAsJsonObject();
        });
    }

    /** Applies a parsed AI design to the config. Call on the game thread. */
    public static void apply(JsonObject o, String prompt, SkyConfig c) {
        c.lastPrompt = prompt;
        c.preset = "AI: " + (prompt.length() > 20 ? prompt.substring(0, 20) + "..." : prompt);
        if (o.has("topColor")) c.topColor = ColorUtil.parse(o.get("topColor").getAsString(), c.topColor);
        if (o.has("horizonColor")) c.horizonColor = ColorUtil.parse(o.get("horizonColor").getAsString(), c.horizonColor);
        if (o.has("cloudColor")) c.cloudColor = ColorUtil.parse(o.get("cloudColor").getAsString(), c.cloudColor);
        if (o.has("stars")) c.stars = o.get("stars").getAsBoolean();
        if (o.has("starBrightness")) c.starBrightness = (float) o.get("starBrightness").getAsDouble();
        if (o.has("weather")) {
            String w = o.get("weather").getAsString().toUpperCase();
            c.weather = SkyConfig.WEATHERS.contains(w) ? w : "AUTO";
        }
        if (o.has("texts") && o.get("texts").isJsonArray()) {
            c.texts.clear();
            for (var el : o.getAsJsonArray("texts")) {
                JsonObject t = el.getAsJsonObject();
                c.texts.add(new SkyText(
                        t.has("text") ? t.get("text").getAsString() : "",
                        "minecraft:default",
                        t.has("color") ? ColorUtil.parse(t.get("color").getAsString(), 0xFFFFFFFF) : 0xFFFFFFFF,
                        t.has("scale") ? (float) t.get("scale").getAsDouble() : 2f,
                        t.has("azimuth") ? (float) t.get("azimuth").getAsDouble() : 0f,
                        t.has("elevation") ? (float) t.get("elevation").getAsDouble() : 30f,
                        t.has("shadow") ? t.get("shadow").getAsBoolean() : true));
            }
        }
    }

    private static String shorten(String s) {
        return s.length() > 160 ? s.substring(0, 160) + "..." : s;
    }
}
