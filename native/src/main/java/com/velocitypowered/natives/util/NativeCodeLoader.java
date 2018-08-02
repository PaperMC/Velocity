package com.velocitypowered.natives.util;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class NativeCodeLoader<T> {
    private final List<Variant<T>> variants;
    private Variant<T> selected;

    public NativeCodeLoader(List<Variant<T>> variants) {
        this.variants = ImmutableList.copyOf(variants);
    }

    public Supplier<T> supply() {
        if (selected == null) {
            selected = select();
        }
        return selected.supplier;
    }

    private Variant<T> select() {
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
        for (Variant<T> variant : variants) {
            T got = variant.get();
            if (got == null) {
                continue;
            }
            return variant.name;
        }
        throw new IllegalArgumentException("Can't find any suitable variants");
    }

    static class Variant<T> {
        private boolean available;
        private final Runnable setup;
        private final String name;
        private final Supplier<T> supplier;
        private boolean hasBeenSetup = false;

        Variant(BooleanSupplier available, Runnable setup, String name, Supplier<T> supplier) {
            this.available = available.getAsBoolean();
            this.setup = setup;
            this.name = name;
            this.supplier = supplier;
        }

        public boolean setup() {
            if (available && !hasBeenSetup) {
                try {
                    setup.run();
                    hasBeenSetup = true;
                } catch (Exception e) {
                    //logger.error("Unable to set up {}", name, e);
                    available = false;
                }
            }
            return hasBeenSetup;
        }

        public T get() {
            if (!hasBeenSetup) {
                setup();
            }

            if (available) {
                return supplier.get();
            }

            return null;
        }
    }

    public static final BooleanSupplier MACOS = () -> System.getProperty("os.name").equalsIgnoreCase("Mac OS X") &&
            System.getProperty("os.arch").equals("x86_64");
    public static final BooleanSupplier LINUX = () -> System.getProperties().getProperty("os.name").equalsIgnoreCase("Linux") &&
            System.getProperty("os.arch").equals("amd64");
    public static final BooleanSupplier MAC_AND_LINUX = () -> MACOS.getAsBoolean() || LINUX.getAsBoolean();
    public static final BooleanSupplier ALWAYS = () -> true;
}
