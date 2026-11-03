# SkyBox Changer

A **client-side** Fabric mod for Minecraft **1.21.8** that lets you change the sky however you like. Everything is visual and local, so it never shows on other players' screens and never touches the server.

## Features

- **AI prompt box**: type any sky ("thunderstorm over a red desert at dusk") and the sky is designed to match.
  - With an API key set, an AI model picks the colours, weather, stars and sky text.
  - Without a key, a built-in interpreter maps words to palettes. Unknown words still produce a unique sky.
- **Presets**: Default, Thunderstorm, Cool Red, Chill Purple, Neon Green, Sunset, Midnight, Rainy.
- **Colours**: zenith (top) colour, horizon colour and cloud colour, set as hex codes.
- **Sky text**: any text, any colour, any scale, any Minecraft font, placed at a direction in the sky (azimuth and height). Text stays put as you look around.
- **Weather**: Auto, Clear, Rain or Thunder.
- **Stars**: on or off, with adjustable brightness.
- **Menu**: animated backdrop with a live colour preview, tabbed layout.

## Controls

- Press **K** to open the menu (rebind it in Controls → SkyBox Changer).
- Settings save when you close the menu.

## Install (development)

1. Install JDK 21.
2. Run `./gradlew build` (Windows: `gradlew.bat build`). The jar appears in `build/libs/`.
3. Run `./gradlew runClient` to launch a dev Minecraft instance with the mod loaded.
4. Copy the jar into your `mods` folder with Fabric Loader 0.17+ and Fabric API for 1.21.8.

## Turning on AI designs

Open `config/skyboxchanger.json` and paste your key into `"apiKey"`. The default model is `claude-haiku-5-5`. The key is stored in plain text on your computer. Do not commit it to GitHub. The `.gitignore` in this repo excludes the config folder.

## Limits

- The AI designs the sky from colours, weather, stars and text. It does not paint a picture. A true image-based skybox would need a separate image-generation service and a custom sky renderer.
- The sun and moon stay vanilla. Hiding them needs a deeper render hook than this version uses.
- Snow is not available as a weather option.
- The mixin targets are Yarn names for 1.21.8. If the game fails to start with a mixin error, check the target method names in `mixin/` against your mappings.

## Project layout

```
src/main/java/com/skyboxchanger/
├── SkyBoxChanger.java        entry point, keybind
├── ai/                       prompt interpreter + AI client
├── config/                   settings, presets, colour helpers, sky text model
├── mixin/                    sky colour, cloud colour, weather and star hooks
├── render/                   projected sky text (HUD)
└── screen/                   the menu
```

## Licence

MIT. See `LICENSE`.
