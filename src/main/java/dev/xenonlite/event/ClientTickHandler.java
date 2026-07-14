package dev.xenonlite.event;

import dev.xenonlite.XenonLite;
import dev.xenonlite.util.ClientPlayerUtil;
import dev.xenonlite.util.MouseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import xenon.dev.ui.ClickGuiObj;

public final class ClientTickHandler {
    private boolean guiKeyDown;
    private boolean throwPotKeyDown;
    private boolean patchClickerV2KeyDown;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityPlayerSP player = ClientPlayerUtil.getPlayer();
        if (player == null) {
            return;
        }

        boolean pressed = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (pressed && !this.guiKeyDown) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.currentScreen instanceof ClickGuiObj) {
                minecraft.displayGuiScreen(null);
            } else if (minecraft.currentScreen == null) {
                minecraft.displayGuiScreen(new ClickGuiObj());
            }
        }
        this.guiKeyDown = pressed;

        if (XenonLite.instance != null && XenonLite.instance.getThrowPot() != null) {
            int throwKey = XenonLite.instance.getThrowPot().getKeybinding();
            boolean throwPressed = throwKey != Keyboard.KEY_NONE && Keyboard.isKeyDown(throwKey);
            if (throwPressed && !this.throwPotKeyDown
                    && Minecraft.getMinecraft().currentScreen == null) {
                XenonLite.instance.getThrowPot().toggle();
            }
            this.throwPotKeyDown = throwPressed;
        }

        if (XenonLite.instance != null && XenonLite.instance.getPatchClickerV2() != null) {
            int patchKey = XenonLite.instance.getPatchClickerV2().getKeybinding();
            boolean patchPressed = patchKey != Keyboard.KEY_NONE && Keyboard.isKeyDown(patchKey);
            if (patchPressed && !this.patchClickerV2KeyDown
                    && Minecraft.getMinecraft().currentScreen == null) {
                XenonLite.instance.getPatchClickerV2().toggle();
            }
            this.patchClickerV2KeyDown = patchPressed;
        }

        // Punto de entrada para la lógica que debe ejecutarse cada tick.
        if (MouseUtil.isLeftButtonDown()) {
            // Añade aquí el comportamiento de tu mod.
        }
    }
}
