package xenon.dev.utils;

public final class XObject<T> {
    private T value;

    public XObject(T value) {
        this.value = value;
    }

    public T get() {
        return this.value;
    }

    public void set(T value) {
        this.value = value;
    }
}
