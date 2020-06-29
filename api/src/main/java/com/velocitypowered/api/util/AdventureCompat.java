package com.velocitypowered.api.util;

/**
 * Utilities to convert from adventure {@link net.kyori.adventure.text.Component}s to text
 * {@link net.kyori.text.Component}s and vice versa.
 *
 * @deprecated Provided only as a transitional aid, will be removed in Velocity 2.0.0
 */
@Deprecated
public class AdventureCompat {
  private AdventureCompat() {
    throw new AssertionError("Do not create instances of this class.");
  }

  public static net.kyori.adventure.text.Component asAdventureComponent(
      net.kyori.text.Component component) {
    String json = net.kyori.text.serializer.gson.GsonComponentSerializer.INSTANCE
        .serialize(component);
    return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
        .deserialize(json);
  }

  public static net.kyori.text.Component asOriginalTextComponent(
      net.kyori.adventure.text.Component component) {
    String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
        .colorDownsamplingGson().serialize(component);
    return net.kyori.text.serializer.gson.GsonComponentSerializer.INSTANCE.deserialize(json);
  }
}
