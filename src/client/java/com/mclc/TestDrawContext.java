package com.mclc;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class TestDrawContext {
    public void test(DrawContext context) {
        Identifier obsidian = Identifier.of("minecraft", "textures/block/obsidian.png");
        com.mclc.utils.RenderUtils.drawTexture(context, obsidian, 0, 0, 0, 0, 16, 16, 16, 16);
    }
}
