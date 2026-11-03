package com.skyboxchanger.mixin;

import com.skyboxchanger.config.SkyConfig;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Weather and star overrides. Only applies to ClientWorld, so the server and other players are untouched.
 */
@Mixin(World.class)
public abstract class WorldMixin {

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void skyboxchanger$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if (!isClientWorld()) return;
        SkyConfig c = SkyConfig.INSTANCE;
        if (!c.enabled || "AUTO".equals(c.weather)) return;
        boolean precipitating = "RAIN".equals(c.weather) || "THUNDER".equals(c.weather);
        cir.setReturnValue(precipitating ? 1.0f : 0.0f);
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void skyboxchanger$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (!isClientWorld()) return;
        SkyConfig c = SkyConfig.INSTANCE;
        if (!c.enabled || "AUTO".equals(c.weather)) return;
        cir.setReturnValue("THUNDER".equals(c.weather) ? 1.0f : 0.0f);
    }

    @Inject(method = "getStarBrightness", at = @At("HEAD"), cancellable = true)
    private void skyboxchanger$stars(float delta, CallbackInfoReturnable<Float> cir) {
        if (!isClientWorld()) return;
        SkyConfig c = SkyConfig.INSTANCE;
        if (!c.enabled) return;
        cir.setReturnValue(c.stars ? c.starBrightness : 0.0f);
    }

    private boolean isClientWorld() {
        return ((Object) this) instanceof ClientWorld;
    }
}
