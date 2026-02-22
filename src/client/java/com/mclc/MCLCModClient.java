package com.mclc;

import com.mclc.config.HUDConfig;
import com.mclc.gui.HUDConfigScreen;
import com.mclc.gui.LunarSettingsScreen;
import org.lwjgl.stb.STBImage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MCLCModClient implements ClientModInitializer {

    private static KeyBinding OPEN_GUI_KEY;
    // public static boolean isFpsEnabled = true; // This line is removed as per the
    // new logic

    @Override
    public void onInitializeClient() {
        OPEN_GUI_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mclc.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.mclc.general"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI_KEY.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HUDConfigScreen());
                }
            }
        });

        // Set custom window icon using GLFW + STBImage directly — cross-platform and
        // crash-proof
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try (InputStream streamIn = getClass().getResourceAsStream("/assets/mclc/textures/gui/icon.png")) {
                if (streamIn == null) {
                    System.err.println("MCLC: icon.png not found at /assets/mclc/textures/gui/icon.png");
                    return;
                }
                // Read all bytes into a direct ByteBuffer (STBImage requires direct memory)
                byte[] bytes = streamIn.readAllBytes();
                ByteBuffer rawBuf = MemoryUtil.memAlloc(bytes.length);
                rawBuf.put(bytes).flip();

                int[] w = { 0 }, h = { 0 }, channels = { 0 };
                ByteBuffer pixels = STBImage.stbi_load_from_memory(rawBuf, w, h, channels, 4);
                MemoryUtil.memFree(rawBuf);

                if (pixels == null) {
                    System.err.println("MCLC: Failed to decode icon PNG: " + STBImage.stbi_failure_reason());
                    return;
                }
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    GLFWImage.Buffer icons = GLFWImage.malloc(1, stack);
                    icons.position(0).width(w[0]).height(h[0]).pixels(pixels);
                    icons.position(0);
                    long handle = client.getWindow().getHandle();
                    GLFW.glfwSetWindowIcon(handle, icons);
                    // Set window title here too — avoids MinecraftClient mixin refmap issues
                    String version = net.minecraft.SharedConstants.getGameVersion().getName();
                    GLFW.glfwSetWindowTitle(handle, "MCLC Client " + version);
                    System.out.println("MCLC: Custom window icon set (" + w[0] + "x" + h[0] + ")");
                } finally {
                    STBImage.stbi_image_free(pixels);
                }
            } catch (Exception e) {
                System.err.println("MCLC: Failed to set window icon: " + e.getMessage());
            }
        });

        // Register HUD Render Event using HUDConfig values
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options.hudHidden || client.player == null)
                return;

            HUDConfig config = HUDConfig.getInstance();

            // FPS Counter
            HUDConfig.ModuleData fpsMod = config.getModule("FPS Counter");
            if (fpsMod != null && fpsMod.enabled) {
                String fpsText = "FPS: " + client.getCurrentFps();
                drawContext.drawTextWithShadow(client.textRenderer, fpsText, fpsMod.x, fpsMod.y, 0xFFFFFFFF);
            }

            // Armor Status
            HUDConfig.ModuleData armorMod = config.getModule("Armor Status");
            if (armorMod != null && armorMod.enabled) {
                int currentY = armorMod.y;
                Iterable<ItemStack> armorItems = client.player.getArmorItems();

                // MC returns armor iterating from Boots to Helmet. We usually want Helmet to
                // Boots visually,
                // so we collect them and reverse, or just draw them bottom-up. Let's draw
                // top-down.
                ItemStack[] armorArray = new ItemStack[4];
                int index = 0;
                for (ItemStack stack : armorItems) {
                    armorArray[index++] = stack;
                }

                for (int i = 3; i >= 0; i--) {
                    ItemStack stack = armorArray[i];
                    if (stack != null && !stack.isEmpty()) {
                        // Draw Item Icon
                        drawContext.drawItem(stack, armorMod.x, currentY);

                        // Default color white if unbreakable
                        int color = 0xFFFFFFFF;
                        String text = "";

                        if (stack.isDamageable()) {
                            int maxDamage = stack.getMaxDamage();
                            int damage = stack.getDamage();
                            int remaining = maxDamage - damage;

                            float percentage = (float) remaining / maxDamage;
                            if (percentage > 0.7f) {
                                color = 0xFF55FF55; // Green
                            } else if (percentage > 0.3f) {
                                color = 0xFFFFFF55; // Yellow
                            } else {
                                color = 0xFFFF5555; // Red
                            }

                            if (armorMod.showPercentage) {
                                text = (int) (percentage * 100) + "%";
                            } else {
                                text = remaining + "/" + maxDamage;
                            }
                        } else {
                            // If it's count-based (like placing blocks in head slot)
                            if (stack.getCount() > 1) {
                                text = String.valueOf(stack.getCount());
                            }
                        }

                        if (!text.isEmpty()) {
                            // Draw text centered vertically with the 16x16 icon
                            drawContext.drawTextWithShadow(client.textRenderer, text, armorMod.x + 20, currentY + 4,
                                    color);
                        }

                        currentY += 18; // 16px icon + 2px padding
                    }
                }
            }
        });
    }
}
