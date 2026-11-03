package com.skyboxchanger.render;

import com.skyboxchanger.config.SkyConfig;
import com.skyboxchanger.config.SkyText;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Draws the custom sky text. Each line is placed by its direction (azimuth and elevation),
 * projected onto the screen from the camera's current view, so it stays put as you look around.
 */
public final class SkyTextRenderer {
    private SkyTextRenderer() {}

    public static void init() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SkyConfig c = SkyConfig.INSTANCE;
        if (!c.enabled || c.texts.isEmpty() || mc.world == null || mc.player == null || mc.options.hudHidden) return;
        if (!mc.world.getDimension().hasSkyLight()) return;

        Camera cam = mc.gameRenderer.getCamera();
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());

        // Camera basis. Forward uses Minecraft's convention: yaw 0 looks toward +Z.
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -fz, ry = 0, rz = fx;
        double rLen = Math.sqrt(rx * rx + rz * rz);
        if (rLen < 1e-6) return; // looking straight up or down
        rx /= rLen; rz /= rLen;
        double ux = ry * fz - rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;

        double tanHalfFov = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        double aspect = (double) w / h;

        for (SkyText t : c.texts) {
            double az = Math.toRadians(t.azimuth);
            double el = Math.toRadians(t.elevation);
            // Azimuth 0 = north (-Z), 90 = east (+X), going clockwise
            double dx = Math.sin(az) * Math.cos(el);
            double dy = Math.sin(el);
            double dz = -Math.cos(az) * Math.cos(el);

            double zc = dx * fx + dy * fy + dz * fz;
            if (zc <= 0.05) continue; // behind the camera
            double xc = dx * rx + dy * ry + dz * rz;
            double yc = dx * ux + dy * uy + dz * uz;
            double sx = (xc / zc) / (tanHalfFov * aspect);
            double sy = (yc / zc) / tanHalfFov;
            if (Math.abs(sx) > 1.2 || Math.abs(sy) > 1.2) continue; // off screen

            double px = w / 2.0 + sx * (w / 2.0);
            double py = h / 2.0 - sy * (h / 2.0);
            draw(ctx, mc.textRenderer, t, px, py);
        }
    }

    private static void draw(DrawContext ctx, TextRenderer tr, SkyText t, double px, double py) {
        Identifier fontId = Identifier.tryParse(t.font);
        Text text = Text.literal(t.text);
        if (fontId != null) text = text.copy().setStyle(Style.EMPTY.withFont(fontId));
        int width = tr.getWidth(text);

        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) px, (float) py);
        m.scale(t.scale, t.scale);
        ctx.drawText(tr, text, -width / 2, -tr.fontHeight / 2, t.color, t.shadow);
        m.popMatrix();
    }
}
