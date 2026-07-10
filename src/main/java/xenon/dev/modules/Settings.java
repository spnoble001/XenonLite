package xenon.dev.modules;

public final class Settings {
    private final String name;
    private final Module owner;
    private final boolean booleanSetting;
    private boolean booleanValue;
    private float numberValue;
    private final float minimum;
    private final float maximum;

    public Settings(char[] name, Module owner, boolean value) {
        this.name = new String(name);
        this.owner = owner;
        this.booleanSetting = true;
        this.booleanValue = value;
        this.minimum = 0.0F;
        this.maximum = 1.0F;
    }

    public Settings(char[] name, Module owner, float value, float minimum, float maximum, boolean ignored) {
        this.name = new String(name);
        this.owner = owner;
        this.booleanSetting = false;
        this.minimum = minimum;
        this.maximum = maximum;
        setValfloat(value);
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
        this.numberValue = Math.max(this.minimum, Math.min(this.maximum, value));
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

    public float getMin() {
        return this.minimum;
    }

    public float getMax() {
        return this.maximum;
    }
}
