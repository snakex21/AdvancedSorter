package com.antigravity.advancedsorter.client;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class KeyInputHandler {

    public static final KeyBinding OPEN_CALCULATOR = new KeyBinding("key.advancedsorter.calculator", Keyboard.KEY_C,
            "key.categories.advancedsorter");

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_CALCULATOR);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_CALCULATOR.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null) {
                mc.player.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_CRAFTING_CALCULATOR, mc.world,
                        (int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ);
            }
        }
    }
}
