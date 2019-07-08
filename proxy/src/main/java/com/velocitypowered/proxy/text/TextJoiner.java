package com.velocitypowered.proxy.text;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import net.kyori.text.Component;
import net.kyori.text.ComponentBuilder;
import net.kyori.text.TextComponent;

public final class TextJoiner {

  public static TextJoiner on(String separator) {
    checkNotNull(separator, "separator");
    return new TextJoiner(TextComponent.of(separator));
  }

  public static TextJoiner on(Component separator) {
    checkNotNull(separator, "separator");
    return new TextJoiner(separator);
  }

  private final Component separator;

  private TextJoiner(Component separator) {
    this.separator = separator;
  }

  public Component join(Object[] components) {
    return join(Arrays.asList(components));
  }

  public Component join(Object first, Object second, Object... rest) {
    return join(iterable(first, second, rest));
  }

  public Component join(Stream<?> components) {
    return join(components.iterator());
  }

  public Component join(Iterable<?> components) {
    return join(components.iterator());
  }

  /**
   * Joins the components of the {@link Iterable}.
   *
   * @param components The component iterable
   * @return The joined component
   */
  public Component join(Iterator<?> components) {
    if (components.hasNext()) {
      Component first = wrapIfNeeded(components.next());
      if (components.hasNext()) {
        TextComponent.Builder builder = TextComponent.builder();
        builder.append(first);
        while (components.hasNext()) {
          builder.append(this.separator);
          builder.append(wrapIfNeeded(components.next()));
        }
        return builder.build();
      } else {
        return first;
      }
    }
    return TextComponent.empty();
  }

  public <A extends ComponentBuilder<?, A>> A appendTo(
      A appendable, Object[] components) {
    return appendTo(appendable, Arrays.asList(components));
  }

  public <A extends ComponentBuilder<?, A>> A appendTo(
      A appendable, Object first, Object second, Object... rest) {
    return appendTo(appendable, iterable(first, second, rest));
  }

  public <A extends ComponentBuilder<?, A>> A appendTo(
      A appendable, Stream<?> components) {
    return appendTo(appendable, components.iterator());
  }

  /**
   * Joins the components of the {@link Iterable} and
   * appends the result to {@link A}.
   *
   * @param appendable The appendable
   * @param components The component iterable
   * @return The joined component
   */
  public <A extends ComponentBuilder<?, A>> A appendTo(
      A appendable, Iterable<?> components) {
    return appendTo(appendable, components.iterator());
  }

  /**
   * Joins the components of the {@link Iterator} and
   * appends the result to {@link A}.
   *
   * @param appendable The appendable
   * @param components The component iterator
   * @return The joined component
   */
  public <A extends ComponentBuilder<?, A>> A appendTo(
      A appendable, Iterator<?> components) {
    if (components.hasNext()) {
      appendable.append(wrapIfNeeded(components.next()));
      while (components.hasNext()) {
        appendable.append(this.separator);
        appendable.append(wrapIfNeeded(components.next()));
      }
    }
    return appendable;
  }

  private static Component wrapIfNeeded(Object object) {
    return object instanceof Component ? (Component) object : TextComponent.of(object.toString());
  }

  private static Iterable<Object> iterable(Object first, Object second, Object[] rest) {
    return new AbstractList<Object>() {

      @Override
      public Object get(int index) {
        return index == 0 ? first : index == 2 ? second : rest[index - 1];
      }

      @Override
      public int size() {
        return rest.length + 2;
      }
    };
  }
}
