package com.velocitypowered.proxy.text;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import net.kyori.text.Component;
import net.kyori.text.ComponentBuilder;
import net.kyori.text.TextComponent;

/**
 * A joiner similar to {@link com.google.common.base.Joiner}
 * but for {@link Component}s.
 */
public final class TextJoiner {

  /**
   * Constructs a new {@link TextJoiner} from the given separator.
   *
   * @param separator The separator
   * @return The text joiner
   */
  public static TextJoiner on(String separator) {
    checkNotNull(separator, "separator");
    return new TextJoiner(TextComponent.of(separator));
  }

  /**
   * Constructs a new {@link TextJoiner} from the given separator.
   *
   * @param separator The separator
   * @return The text joiner
   */
  public static TextJoiner on(Component separator) {
    checkNotNull(separator, "separator");
    return new TextJoiner(separator);
  }

  private final Component separator;

  private TextJoiner(Component separator) {
    this.separator = separator;
  }

  /**
   * Joins the array of components.
   *
   * @param components The component array
   * @return The joined component
   */
  public Component join(Component[] components) {
    return join(Arrays.asList(components));
  }

  /**
   * Joins the components.
   *
   * @param first The first component
   * @param second The second component
   * @param more More components
   * @return The joined component
   */
  public Component join(Component first, Component second, Component... more) {
    return join(iterable(first, second, more));
  }

  /**
   * Joins the stream of components.
   *
   * @param components The component array
   * @return The joined component
   */
  public Component join(Stream<? extends Component> components) {
    return join(components.iterator());
  }

  /**
   * Joins the components.
   *
   * @param components The components
   * @return The joined component
   */
  public Component join(Iterable<? extends Component> components) {
    return join(components.iterator());
  }

  /**
   * Joins the component iterator.
   *
   * @param components The components
   * @return The joined component
   */
  public Component join(Iterator<? extends Component> components) {
    if (components.hasNext()) {
      Component first = components.next();
      if (components.hasNext()) {
        TextComponent.Builder builder = TextComponent.builder();
        builder.append(first);
        while (components.hasNext()) {
          builder.append(this.separator);
          builder.append(components.next());
        }
        return builder.build();
      } else {
        return first;
      }
    }
    return TextComponent.empty();
  }

  public <A extends ComponentBuilder<?, A>> A appendTo(
      A appendable, Component[] components) {
    return appendTo(appendable, Arrays.asList(components));
  }

  public <A extends ComponentBuilder<?, A>> A appendTo(
      A appendable, Component first, Component second, Component... rest) {
    return appendTo(appendable, iterable(first, second, rest));
  }

  public <A extends ComponentBuilder<?, A>> A appendTo(
      A appendable, Stream<? extends Component> components) {
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
      A appendable, Iterable<? extends Component> components) {
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
      A appendable, Iterator<? extends Component> components) {
    if (components.hasNext()) {
      appendable.append(components.next());
      while (components.hasNext()) {
        appendable.append(this.separator);
        appendable.append(components.next());
      }
    }
    return appendable;
  }

  private static Iterable<Component> iterable(Component first, Component second, Component[] rest) {
    return new AbstractList<Component>() {

      @Override
      public Component get(int index) {
        return index == 0 ? first : index == 2 ? second : rest[index - 1];
      }

      @Override
      public int size() {
        return rest.length + 2;
      }
    };
  }
}
