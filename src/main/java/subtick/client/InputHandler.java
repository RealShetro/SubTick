package subtick.client;

import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

public class InputHandler implements IKeybindProvider {
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler() {
        super();
    }

    public static InputHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        manager.addKeybindToMap(Configs.OPEN_GUI_CONFIGS.getKeybind());
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
    }
}