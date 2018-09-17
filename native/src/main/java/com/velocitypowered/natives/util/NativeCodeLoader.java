package com.velocitypowered.natives.util;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class NativeCodeLoader<T> implements Supplier<T> {
    private final List<Variant<T>> variants;
    private volatile Variant<T> selected;

    public NativeCodeLoader(List<Variant<T>> variants) {
        this.variants = ImmutableList.copyOf(variants);
    }

    @Override
    public T get() {
        if (selected == null) {
            selected = tryLoad();
        }
        return selected.object;
    }

    private Variant<T> tryLoad() {
        synchronized (this) {
            if (selected != null) {
                return selected;
            }

            for (Variant<T> variant : variants) {
                T got = variant.get();
                if (got == null) {
                    continue;
                }
                selected = variant;
                return selected;
            }
            throw new IllegalArgumentException("Can't find any suitable variants");
        }
    }

    public String getLoadedVariant() {
        if (selected == null) {
            selected = tryLoad();
        }
        return selected.name;
    }

    static class Variant<T> {
        private boolean available;
        private final Runnable setup;
        private final String name;
        private final T object;
        private boolean hasBeenSetup = false;

        Variant(BooleanSupplier available, Runnable setup, String name, T object) {
            this.available = available.getAsBoolean();
            this.setup = setup;
            this.name = name;
            this.object = object;
        }

        private void setup() {
            if (available && !hasBeenSetup) {
                try {
                    setup.run();
                    hasBeenSetup = true;
                } catch (Exception e) {
                    available = false;
                }
            }
        }

        public T get() {
            if (!hasBeenSetup) {
                setup();
            }

            if (available) {
                return object;
            }

            return null;
        }
    }

    static final BooleanSupplier MACOS = () -> System.getProperty("os.name").equalsIgnoreCase("Mac OS X") &&
            System.getProperty("os.arch").equals("x86_64");
    static final BooleanSupplier LINUX = () -> System.getProperties().getProperty("os.name").equalsIgnoreCase("Linux") &&
            System.getProperty("os.arch").equals("amd64");
    static final BooleanSupplier ALWAYS = () -> true;
}
