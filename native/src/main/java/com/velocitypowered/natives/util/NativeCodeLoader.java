package com.velocitypowered.natives.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class NativeCodeLoader<T> implements Supplier<T> {
    private final Variant<T> selected;

    NativeCodeLoader(List<Variant<T>> variants) {
        this.selected = getVariant(variants);
    }

    @Override
    public T get() {
        return selected.object;
    }

    private static <T> Variant<T> getVariant(List<Variant<T>> variants) {
        for (Variant<T> variant : variants) {
            T got = variant.get();
            if (got == null) {
                continue;
            }
            return variant;
        }
        throw new IllegalArgumentException("Can't find any suitable variants");
    }

    public String getLoadedVariant() {
        return selected.name;
    }

    static class Variant<T> {
        private volatile boolean available;
        private final Runnable setup;
        private final String name;
        private final T object;
        private volatile boolean hasBeenSetup = false;

        Variant(BooleanSupplier available, Runnable setup, String name, T object) {
            this.available = available.getAsBoolean();
            this.setup = setup;
            this.name = name;
            this.object = object;
        }

        public @Nullable T get() {
            if (!available) {
                return null;
            }

            // Make sure setup happens only once
            if (!hasBeenSetup) {
                synchronized (this) {
                    // We change availability if need be below, may as well check it again here for sanity.
                    if (!available) {
                        return null;
                    }

                    // Okay, now try the setup if we haven't done so yet.
                    if (!hasBeenSetup) {
                        try {
                            setup.run();
                            hasBeenSetup = true;
                            return object;
                        } catch (Exception e) {
                            available = false;
                            return null;
                        }
                    }
                }
            }

            return object;
        }
    }

    static final BooleanSupplier MACOS = () -> System.getProperty("os.name", "").equalsIgnoreCase("Mac OS X") &&
            System.getProperty("os.arch").equals("x86_64");
    static final BooleanSupplier LINUX = () -> System.getProperties().getProperty("os.name", "").equalsIgnoreCase("Linux") &&
            System.getProperty("os.arch").equals("amd64");
    static final BooleanSupplier ALWAYS = () -> true;
}
