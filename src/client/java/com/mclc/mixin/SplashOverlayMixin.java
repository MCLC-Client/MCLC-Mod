package com.mclc.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private ResourceReload reload;

    private static final Identifier LOGO = Identifier.of("mclc", "textures/gui/icon.png");
    private float smoothedProgress = 0.0f;
    private int fadeFrames = 0;
    private boolean isFadingOut = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initAdjustments(MinecraftClient client, ResourceReload reload,
            Consumer<Optional<Throwable>> exceptionHandler, boolean reloading, CallbackInfo ci) {
        // Prevent red background flashing, use very dark violet
        client.getFramebuffer().setClearColor(0.039f, 0.016f, 0.071f, 1.0f); // #0a0412
        client.getFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);

        // Register texture manually to ensure it's loaded before the first frame
        client.getTextureManager().registerTexture(LOGO, new net.minecraft.client.texture.ResourceTexture(LOGO));
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderCustomOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Cancel the original Mojang render method
        ci.cancel();

        // Safe close & crash fix
        if (this.client == null || this.reload == null) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // Safe Logic & Loop Fix: Update smoothed progress
        float rawProgress = this.reload.getProgress();
        if (rawProgress >= 0.95f) {
            rawProgress = 1.0f; // Visual snap internally
        }

        if (!this.isFadingOut) {
            this.smoothedProgress = MathHelper.lerp(0.15f, this.smoothedProgress, rawProgress);
            if (this.smoothedProgress >= 0.99f) {
                this.smoothedProgress = 1.0f;
            }
        }

        // Trigger Auto-Fade
        if (this.smoothedProgress >= 1.0f && this.reload.isComplete()) {
            this.isFadingOut = true;
        }

        if (this.isFadingOut) {
            this.fadeFrames++;
        }

        // Auto-fade over 20 frames, then cleanly close
        if (this.fadeFrames >= 20) {
            this.client.setOverlay(null);
            return;
        }

        // Render main menu behind us if we are fading out
        if (this.isFadingOut && this.client.currentScreen != null) {
            this.client.currentScreen.render(context, mouseX, mouseY, delta);
        }

        float alpha = 1.0f - (this.fadeFrames / 20.0f);

        if (alpha > 0.0f) {
            RenderSystem.enableBlend();

            // 1. New Dark Violet to Black Gradient (#0a0412 to #000000)
            int bgAlpha = (int) (alpha * 255);
            if (bgAlpha > 0) {
                int bgTop = (bgAlpha << 24) | 0x0A0412;
                int bgBot = (bgAlpha << 24) | 0x000000;
                context.fillGradient(0, 0, width, height, bgTop, bgBot);
            }

            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

            // 2. Draw Logo safely using RenderSystem binding
            RenderSystem.setShaderTexture(0, LOGO);
            int logoWidth = 120;
            int logoHeight = 120;

            context.getMatrices().push();
            context.getMatrices().translate(width / 2.0, height / 2.0 - 20, 0);
            context.drawTexture(LOGO, -logoWidth / 2, -logoHeight / 2, 0, 0, logoWidth, logoHeight, logoWidth,
                    logoHeight);
            context.getMatrices().pop();

            // 3. New Thin Progress Bar logic
            int barWidth = 240;
            int barHeight = 4;
            int barX = (width - barWidth) / 2;
            int barY = height / 2 + 70;

            String progressText = "Lade Ressourcen... " + (int) (this.smoothedProgress * 100) + "%";
            int textWidth = client.textRenderer.getWidth(progressText);

            // Text above the bar: bright violet-white formatting
            context.drawTextWithShadow(client.textRenderer, progressText, (width - textWidth) / 2, barY - 12,
                    0xE8D4FF | ((int) (alpha * 255) << 24));

            // Alpha applied colors for the new violet theme (#9d50bb)
            int brightAlpha = (int) (alpha * 255);
            int bgColor = 0xFF140A21; // Dark track
            int progressColor = 0xFF9D50BB; // Requested light violet
            int glowColor = 0xFF9D50BB; // Glow same color as requested

            int finalBgColor = (bgColor & 0x00FFFFFF) | (brightAlpha << 24);
            int finalProgressColor = (progressColor & 0x00FFFFFF) | (brightAlpha << 24);
            int finalGlowColor = (glowColor & 0x00FFFFFF) | ((int) (alpha * 80) << 24); // Softer glow transparency

            // Draw Background track (Empty Bar)
            drawRoundedRect(context, barX, barY, barWidth, barHeight, finalBgColor);

            int currentFill = (int) (barWidth * this.smoothedProgress);
            if (currentFill > 0) {
                // Glowing Drop Shadow backing (softer padding)
                drawRoundedRect(context, barX - 2, barY - 2, currentFill + 4, barHeight + 4, finalGlowColor);
                // Filled Bar Progress Core
                drawRoundedRect(context, barX, barY, currentFill, barHeight, finalProgressColor);
            }

            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0)
            return;
        int radius = height / 2;
        if (width < radius * 2) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x + 1, y, x + radius, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - radius, y, x + width - 1, y + height, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
