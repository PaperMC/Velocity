package com.velocitypowered.proxy.text;

import com.google.common.collect.Iterables;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;

public class TextComponents {

  /**
   * Gets a new {@link Iterable} based on the provided
   * {@link Iterable} that transforms the elements into
   * {@link Component}.
   *
   * @param iterable The iterable
   * @return The transformed iterable
   */
  public static Iterable<Component> iterableOf(Iterable<?> iterable) {
    //noinspection StaticPseudoFunctionalStyleMethod
    return Iterables.transform(iterable, TextComponents::wrapIfNeeded);
  }

  /**
   * Converts the objects into a array of {@link Component}s.
   *
   * @param array The array
   * @return The component array
   */
  public static Component[] of(Object... array) {
    Component[] components = new Component[array.length];
    for (int i = 0; i < array.length; i++) {
      components[i] = wrapIfNeeded(array[i]);
    }
    return components;
  }

  private static Component wrapIfNeeded(Object object) {
    return object instanceof Component ? (Component) object : TextComponent.of(object.toString());
  }

  private TextComponents() {
    throw new AssertionError();
  }
}
