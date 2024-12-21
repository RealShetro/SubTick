package subtick.client;

import net.minecraft.client.Minecraft;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;

public class KeyCallbacks {
    public static void init(Minecraft mc) {
        IHotkeyCallback callbackHotkeys = new KeyCallbackHotkeys(mc);

        Configs.OPEN_GUI_CONFIGS.getKeybind().setCallback(callbackHotkeys);
    }

    private static class KeyCallbackHotkeys implements IHotkeyCallback {
        private final Minecraft mc;

        public KeyCallbackHotkeys(Minecraft mc) {
            this.mc = mc;
        }

        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            if (this.mc.player == null || this.mc.level == null) {
                return false;
            }

            if (key == Configs.OPEN_GUI_CONFIGS.getKeybind()) {
                GuiBase.openGui(new GuiConfigs());
                return true;
            }

            return false;
        }
    }
}