package com.velocitypowered.natives.util;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

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

    private Status status;
    private final Runnable setup;
    private final String name;
    private final T object;

    Variant(BooleanSupplier possiblyAvailable, Runnable setup, String name, T object) {
      this.status =
          possiblyAvailable.getAsBoolean() ? Status.POSSIBLY_AVAILABLE : Status.NOT_AVAILABLE;
      this.setup = setup;
      this.name = name;
      this.object = object;
    }

    public @Nullable T get() {
      if (status == Status.NOT_AVAILABLE || status == Status.SETUP_FAILURE) {
        return null;
      }

      // Make sure setup happens only once
      if (status == Status.POSSIBLY_AVAILABLE) {
        try {
          setup.run();
          status = Status.SETUP;
        } catch (Exception e) {
          status = Status.SETUP_FAILURE;
          return null;
        }
      }

      return object;
    }
  }

  private enum Status {
    NOT_AVAILABLE,
    POSSIBLY_AVAILABLE,
    SETUP,
    SETUP_FAILURE
  }

  static final BooleanSupplier MACOS = () ->
      System.getProperty("os.name", "").equalsIgnoreCase("Mac OS X") &&
          System.getProperty("os.arch").equals("x86_64");
  static final BooleanSupplier LINUX = () ->
      System.getProperties().getProperty("os.name", "").equalsIgnoreCase("Linux") &&
          System.getProperty("os.arch").equals("amd64");
  static final BooleanSupplier ALWAYS = () -> true;
}
