package xenon.dev.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;

public abstract class Module {
    protected final Minecraft minecraft = Minecraft.getMinecraft();
    protected final List<Settings> sl = new ArrayList<Settings>();
    protected final char[] characters = new char[36];
    protected boolean enabled;
    protected String name = getClass().getSimpleName();
    private int keybinding;

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public final boolean isEnabled() {
        return this.enabled;
    }

    public String getName() {
        return this.name;
    }

    public List<Settings> getSettings() {
        return Collections.unmodifiableList(this.sl);
    }

    public char[] getCharacters() {
        return this.characters.clone();
    }

    public int getKeybinding() {
        return this.keybinding;
    }

    public void setKeybinding(int keybinding) {
        this.keybinding = Math.max(0, keybinding);
    }

    protected void registerSettings() {
        // Punto de extensión para un futuro gestor de configuración.
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public abstract char getSort();
}
