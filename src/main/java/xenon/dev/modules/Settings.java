package xenon.dev.modules;

public final class Settings {
    private final String name;
    private final Module owner;
    private final boolean booleanSetting;
    private final boolean modeSetting;
    private boolean booleanValue;
    private float numberValue;
    private final float minimum;
    private final float maximum;
    private final float step;
    private final String[] modes;
    private int modeIndex;

    public Settings(char[] name, Module owner, boolean value) {
        this.name = new String(name);
        this.owner = owner;
        this.booleanSetting = true;
        this.modeSetting = false;
        this.booleanValue = value;
        this.minimum = 0.0F;
        this.maximum = 1.0F;
        this.step = 0.0F;
        this.modes = new String[0];
    }

    public Settings(char[] name, Module owner, float value, float minimum, float maximum, boolean ignored) {
        this.name = new String(name);
        this.owner = owner;
        this.booleanSetting = false;
        this.modeSetting = false;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = 0.0F;
        this.modes = new String[0];
        setValfloat(value);
    }

    public Settings(char[] name, Module owner, float value, float minimum, float maximum, float step) {
        this.name = new String(name);
        this.owner = owner;
        this.booleanSetting = false;
        this.modeSetting = false;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = Math.max(0.0F, step);
        this.modes = new String[0];
        setValfloat(value);
    }

    public Settings(char[] name, Module owner, String value, String... modes) {
        this.name = new String(name);
        this.owner = owner;
        this.booleanSetting = false;
        this.modeSetting = true;
        this.minimum = 0.0F;
        this.maximum = 0.0F;
        this.step = 0.0F;
        this.modes = modes == null ? new String[0] : modes.clone();
        setValString(value);
    }

    public boolean getValBoolean() {
        return this.booleanValue;
    }

    public void setValBoolean(boolean value) {
        this.booleanValue = value;
    }

    public float getValfloat() {
        return this.numberValue;
    }

    public void setValfloat(float value) {
        float clamped = Math.max(this.minimum, Math.min(this.maximum, value));
        if (this.step > 0.0F) {
            clamped = this.minimum + Math.round((clamped - this.minimum) / this.step) * this.step;
        }
        this.numberValue = Math.max(this.minimum, Math.min(this.maximum, clamped));
    }

    public String getValString() {
        return this.modes.length == 0 ? "" : this.modes[this.modeIndex];
    }

    public void setValString(String value) {
        this.modeIndex = 0;
        for (int i = 0; i < this.modes.length; i++) {
            if (this.modes[i].equalsIgnoreCase(value)) {
                this.modeIndex = i;
                return;
            }
        }
    }

    public void nextMode() {
        if (this.modes.length > 0) {
            this.modeIndex = (this.modeIndex + 1) % this.modes.length;
        }
    }

    public String getName() {
        return this.name;
    }

    public Module getOwner() {
        return this.owner;
    }

    public boolean isBooleanSetting() {
        return this.booleanSetting;
    }

    public boolean isModeSetting() {
        return this.modeSetting;
    }

    public boolean isNumberSetting() {
        return !this.booleanSetting && !this.modeSetting;
    }

    public float getMin() {
        return this.minimum;
    }

    public float getMax() {
        return this.maximum;
    }
}
