package com.skyboxchanger;

import com.skyboxchanger.config.SkyConfig;
import com.skyboxchanger.render.SkyTextRenderer;
import com.skyboxchanger.screen.SkyMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class SkyBoxChanger implements ClientModInitializer {
    public static final String MOD_ID = "skyboxchanger";

    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        SkyConfig.INSTANCE.getClass(); // loads the config now
        SkyTextRenderer.init();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyboxchanger.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenu.wasPressed()) {
                if (mc.currentScreen == null) {
                    mc.setScreen(new SkyMenuScreen(null));
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> SkyConfig.INSTANCE.save());
    }
}
