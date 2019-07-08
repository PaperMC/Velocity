package com.velocitypowered.proxy.text.translation;

import static com.google.common.base.Preconditions.checkNotNull;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Translatable {

  public static Translatable of(String key) {
    checkNotNull(key, "key");
    return new Translatable(key);
  }

  private final String key;
  private @Nullable TranslatableComponent withoutArgs;

  private Translatable(String key) {
    this.key = key;
  }

  /**
   * Gets the {@link TranslatableComponent} without any arguments.
   *
   * @return The translatable component
   */
  public TranslatableComponent get() {
    if (this.withoutArgs == null) {
      this.withoutArgs = TranslatableComponent.of(this.key);
    }
    return this.withoutArgs;
  }

  public TranslatableComponent.Builder builder() {
    return TranslatableComponent.builder(this.key);
  }

  public TranslatableComponent with(TextComponent... args) {
    return TranslatableComponent.of(this.key, args);
  }

  public TranslatableComponent with(Object... args) {
    return TranslatableComponent.of(this.key, toComponentArray(args));
  }

  public TranslatableComponent.Builder builderWith(TextComponent... args) {
    return TranslatableComponent.builder(this.key).args(args);
  }

  public TranslatableComponent.Builder builderWith(Object... args) {
    return TranslatableComponent.builder(this.key).args(toComponentArray(args));
  }

  private static Component[] toComponentArray(Object[] args) {
    Component[] args1 = new Component[args.length];
    for (int i = 0; i < args1.length; i++) {
      Object object = args[i];
      if (object instanceof Component) {
        args1[i] = (Component) object;
      } else {
        args1[i] = TextComponent.of(object.toString());
      }
    }
    return args1;
  }
}
