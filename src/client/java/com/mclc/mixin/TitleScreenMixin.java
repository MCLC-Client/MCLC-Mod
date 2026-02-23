package com.mclc.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the "Minecraft Realms" button, shifts remaining buttons upward,
 * and fully removes the "Copyright Mojang AB." text from render().
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends net.minecraft.client.gui.screen.Screen {

    // Copyright string exact match as used by vanilla TitleScreen
    private static final String COPYRIGHT = "Copyright Mojang AB. Do not distribute!";

    protected TitleScreenMixin(net.minecraft.text.Text title) {
        super(title);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 1. init() — remove Realms button, shift remaining buttons up
    // ────────────────────────────────────────────────────────────────────────────
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;

        int realmsY = -1;
        int removedHeight = 20;

        for (Element element : screen.children()) {
            if (element instanceof ButtonWidget btn) {
                if (btn.getMessage().getContent() instanceof TranslatableTextContent t
                        && t.getKey().equals("menu.online")) {
                    realmsY = btn.getY();
                    removedHeight = btn.getHeight();
                    btn.visible = false;
                    btn.active = false;
                    btn.setWidth(0);
                    btn.setHeight(0);
                }
            }
        }

        if (realmsY < 0)
            return;

        final int removedY = realmsY;
        final int shift = removedHeight + 4;

        for (Element element : screen.children()) {
            if (element instanceof ButtonWidget btn && btn.visible && btn.getY() > removedY) {
                btn.setY(btn.getY() - shift);
            }
        }

        // Pin non-button ClickableWidgets in lower half (e.g. language/accessibility
        // icons)
        for (Element element : screen.children()) {
            if (element instanceof ClickableWidget cw
                    && !(element instanceof ButtonWidget)
                    && cw.visible
                    && cw.getY() > height / 2) {
                cw.setY(height - 10);
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 2. Hide the copyright text radically using a Redirect
    // ────────────────────────────────────────────────────────────────────────────
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    private int hideCopyrightText(DrawContext instance, net.minecraft.client.font.TextRenderer textRenderer,
            net.minecraft.text.Text text, int x, int y, int color) {
        if (text != null && text.getString().contains("Copyright Mojang AB")) {
            // Do not draw it, return immediately. drawTextWithShadow returns an int
            // (typically x + width).
            return x;
        }
        // Otherwise, allow normal rendering
        return instance.drawTextWithShadow(textRenderer, text, x, y, color);
    }
}
