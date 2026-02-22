package com.mclc.utils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

public class RenderUtils {

    /**
     * Cross-version texture drawing.
     * In 1.21.1, the signature is drawTexture(Identifier, int, int, float/int,
     * float/int, int, int, int, int).
     * In 1.21.5, it was replaced by drawTexture(Function<Identifier, RenderLayer>,
     * Identifier, int, int, float, float, int, int, int, int).
     */
    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width,
            int height, int texW, int texH) {
        for (Method m : context.getClass().getMethods()) {
            Class<?>[] pts = m.getParameterTypes();

            // 1.21.1 signature (9 parameters)
            if (pts.length == 9 && pts[0] == Identifier.class && pts[8] == int.class) {
                try {
                    if (pts[3] == float.class) {
                        m.invoke(context, texture, x, y, u, v, width, height, texW, texH);
                    } else {
                        m.invoke(context, texture, x, y, (int) u, (int) v, width, height, texW, texH);
                    }
                    return;
                } catch (Exception ignored) {
                }
            }

            // 1.21.5 signature (10 parameters with Function at index 0)
            if (pts.length == 10 && pts[1] == Identifier.class && pts[4] == float.class) {
                try {
                    Function<Identifier, Object> renderLayerFunc = id -> {
                        try {
                            for (Method rlm : net.minecraft.client.render.RenderLayer.class.getMethods()) {
                                if (Modifier.isStatic(rlm.getModifiers()) &&
                                        rlm.getParameterCount() == 1 &&
                                        rlm.getParameterTypes()[0] == Identifier.class &&
                                        rlm.getReturnType() == net.minecraft.client.render.RenderLayer.class) {
                                    return rlm.invoke(null, id);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                        return null;
                    };
                    m.invoke(context, renderLayerFunc, texture, x, y, u, v, width, height, texW, texH);
                    return;
                } catch (Exception ignored) {
                }
            }
        }
    }
}
