package io.smartcat.ranger.core;

/**
 * Value that always returns specified value.
 *
 * @param <T> Type this value would evaluate to.
 */
public class PrimitiveValue<T> implements Value<T> {

    private final T value;

    /**
     * Constructs primitive value which will always return specified <code>value</code>.
     *
     * @param value Value to be returned.
     */
    public PrimitiveValue(T value) {
        this.value = value;
    }

    @Override
    public T eval() {
        return value;
    }

    /**
     * Helper method to construct {@link PrimitiveValue}.
     *
     * @param value Value to be returned by created primitive value.
     * @param <T> Type this value would evaluate to.
     *
     * @return An instance of {@link PrimitiveValue}.
     */
    public static <T> PrimitiveValue<T> of(T value) {
        return new PrimitiveValue<T>(value);
    }
}
