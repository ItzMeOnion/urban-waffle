package com.skyboxchanger.mixin;

import com.skyboxchanger.config.ColorUtil;
import com.skyboxchanger.config.SkyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the sky colour and cloud colour on the client only. */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void skyboxchanger$skyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        SkyConfig c = SkyConfig.INSTANCE;
        if (!c.enabled) return;
        // Pitch: positive looks down, negative looks up. Looking up shows the top colour.
        float pitch = 0f;
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (camera != null) pitch = camera.getPitch();
        float t = MathHelper.clamp(-pitch / 90f, 0f, 1f);
        cir.setReturnValue(toVec(ColorUtil.lerp(c.horizonColor, c.topColor, t)));
    }

    @Inject(method = "getCloudsColor", at = @At("HEAD"), cancellable = true)
    private void skyboxchanger$cloudColor(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        SkyConfig c = SkyConfig.INSTANCE;
        if (!c.enabled) return;
        cir.setReturnValue(toVec(c.cloudColor));
    }

    private static Vec3d toVec(int argb) {
        return new Vec3d(((argb >> 16) & 0xFF) / 255.0, ((argb >> 8) & 0xFF) / 255.0, (argb & 0xFF) / 255.0);
    }
}
