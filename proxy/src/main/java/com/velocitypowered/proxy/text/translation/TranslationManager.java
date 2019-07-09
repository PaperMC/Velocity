package com.velocitypowered.proxy.text.translation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.text.TextComponents;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.Style;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TranslationManager {

  private static final Logger logger = LogManager.getLogger(TranslationManager.class);
  private static final Gson gson = new Gson();

  /**
   * The fallback registry, if no bundle or translation
   * was found for a specific locale.
   */
  private @Nullable TranslationRegistry fallback;

  private final Map<Locale, TranslationRegistry> registries = new HashMap<>();

  class TranslationRegistry {

    /**
     * A language specific registry as fallback. The fallback won't care
     * about the country or variations.
     */
    private final @Nullable TranslationRegistry fallback;
    private final Map<String, Translation> translations = new HashMap<>();

    TranslationRegistry(@Nullable TranslationRegistry fallback) {
      this.fallback = fallback;
    }

    void put(String key, Translation translation) {
      this.translations.put(key, translation);
      if (this.fallback != null) {
        this.fallback.putIfAbsent(key, translation);
      }
    }

    private void putIfAbsent(String key, Translation translation) {
      this.translations.putIfAbsent(key, translation);
    }

    @Nullable Translation get(String key) {
      Translation translation = getDirect(key);
      if (translation != null) {
        return translation;
      }
      TranslationRegistry fallback = TranslationManager.this.fallback;
      return fallback != null && fallback != this && fallback != this.fallback
          ? fallback.getDirect(key) : null;
    }

    @Nullable Translation getDirect(String key) {
      Translation translation = this.translations.get(key);
      if (translation != null) {
        return translation;
      }
      return this.fallback != null ? this.fallback.getDirect(key) : null;
    }
  }

  /**
   * Represents a translation.
   */
  class Translation {

    final Component component;
    final String plain;

    Translation(Component component) {
      this.component = component;
      this.plain = PlainComponentSerializer.INSTANCE.serialize(component);
    }
  }

  /**
   * Represents a fixed translation.
   */
  class FixedTranslation extends Translation {

    FixedTranslation(Component component) {
      super(component);
    }
  }

  /**
   * Sets the fallback {@link Locale}.
   *
   * @param locale The fallback locale
   */
  public void setFallback(Locale locale) {
    this.fallback = getOrCreateRegistry(locale);
  }

  private TranslationRegistry getOrCreateRegistry(Locale locale) {
    TranslationRegistry registry = this.registries.get(locale);
    if (registry != null) {
      return registry;
    }
    TranslationRegistry fallback = null;
    if (!locale.getCountry().isEmpty() || !locale.getVariant().isEmpty()) {
      fallback = this.registries.computeIfAbsent(new Locale(locale.getLanguage()),
          langLocale -> new TranslationRegistry(null));
    }
    registry = new TranslationRegistry(fallback);
    this.registries.put(locale, registry);
    return registry;
  }

  /**
   * Loads a directory with translation files.
   *
   * @param path The path of the directory
   * @throws IOException If something goes wrong
   */
  public void loadDirectory(Path path) throws IOException {
    Files.walk(path).forEach(file -> {
      if (!Files.isRegularFile(file)) {
        return;
      }
      String fullName = file.getFileName().toString();
      String extension = com.google.common.io.Files.getFileExtension(fullName);
      if (extension.isEmpty()) {
        return;
      }
      String name = com.google.common.io.Files.getNameWithoutExtension(fullName);
      Locale locale = Locale.forLanguageTag(name.replace('_', '-'));
      if (extension.equals("properties")) {
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(file)) {
          properties.load(is);
          addBundle(locale, properties);
        } catch (IOException e) {
          logger.error("Failed to load translations properties file", e);
        }
      } else if (extension.equals("json")) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
          addBundle(locale, gson.fromJson(reader, JsonObject.class));
        } catch (IOException e) {
          logger.error("Failed to load translations json file", e);
        }
      }
    });
  }

  /**
   * Adds the properties for the given locale.
   *
   * <p>The first call to this method will also set the fallback locale.</p>
   *
   * @param locale The locale to register the resource bundle for
   * @param properties The properties to add
   */
  public void addBundle(Locale locale, Properties properties) {
    checkNotNull(locale, "locale");
    checkNotNull(properties, "properties");

    Map<String, String> entries = new HashMap<>();
    for (Object key : properties.keySet()) {
      entries.put(key.toString(), properties.getProperty(key.toString()));
    }
    addBundle(locale, entries);
  }

  /**
   * Adds a resource bundle for the given locale.
   *
   * <p>The first call to this method will also set the fallback locale.</p>
   *
   * @param locale The locale to register the resource bundle for
   * @param resourceBundle The resource bundle to add
   */
  public void addBundle(Locale locale, ResourceBundle resourceBundle) {
    checkNotNull(locale, "locale");
    checkNotNull(resourceBundle, "resourceBundle");

    Map<String, String> entries = new HashMap<>();
    for (String key : resourceBundle.keySet()) {
      entries.put(key, resourceBundle.getString(key));
    }
    addBundle(locale, entries);
  }

  /**
   * Adds a json object for the given locale.
   *
   * <p>The first call to this method will also set the fallback locale.</p>
   *
   * @param locale The locale to register the resource bundle for
   * @param entries The entries to add
   */
  public void addBundle(Locale locale, Map<String, String> entries) {
    checkNotNull(locale, "locale");
    checkNotNull(entries, "entries");

    TranslationRegistry registry = getOrCreateRegistry(locale);
    for (Map.Entry<String, String> entry : entries.entrySet()) {
      String value = entry.getValue();
      Translation translation = createTranslation(value,
          TextComponent::of);
      registry.put(entry.getKey(), translation);
    }
    if (fallback == null) {
      fallback = registry;
    }
  }

  /**
   * Adds a json object for the given locale.
   *
   * <p>The first call to this method will also set the fallback locale.</p>
   *
   * @param locale The locale to register the resource bundle for
   * @param jsonObject The json object to add
   */
  public void addBundle(Locale locale, JsonObject jsonObject) {
    checkNotNull(locale, "locale");
    checkNotNull(jsonObject, "jsonObject");

    TranslationRegistry registry = getOrCreateRegistry(locale);
    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
      String key = entry.getKey();
      JsonElement element = entry.getValue();
      if (element.isJsonNull()) {
        continue;
      }
      String value = gson.toJson(element);
      Translation translation = createTranslation(value,
          GsonComponentSerializer.INSTANCE::deserialize);
      registry.put(key, translation);
    }
    if (fallback == null) {
      fallback = registry;
    }
  }

  private Translation createTranslation(String value, Function<String, Component> function) {
    Component component = function.apply(value);
    if (this.formatPattern.matcher(value).find()) {
      return new Translation(component);
    }
    return new FixedTranslation(component);
  }

  /**
   * Translates the translation for the given key and arguments.
   *
   * @param locale The locale
   * @param key The key of the translation
   * @param arguments The arguments
   * @return The translated string, if the key is supported, otherwise the key
   */
  public String translate(Locale locale, String key, Object... arguments) {
    checkNotNull(locale, "locale");
    checkNotNull(key, "key");
    checkNotNull(arguments, "arguments");

    Component[] components = TextComponents.of(arguments);
    Component translated = translateComponent(
        locale, TranslatableComponent.of(key, components));
    return PlainComponentSerializer.INSTANCE.serialize(translated);
  }

  private TranslationRegistry getRegistry(Locale locale) {
    TranslationRegistry registry = this.registries.get(locale);
    if (registry != null) {
      return registry;
    }
    if (!locale.getCountry().isEmpty() || !locale.getVariant().isEmpty()) {
      registry = this.registries.get(new Locale(locale.getLanguage()));
    }
    return registry != null ? registry : this.fallback;
  }

  /**
   * Translates the {@link Component} for the given {@link Player}.
   *
   * @param player The player
   * @param component The component to translate
   * @return The translated component
   */
  public Component translateComponent(Player player, Component component) {
    checkNotNull(player, "player");
    return translateComponent(player.getLocale(), component);
  }

  /**
   * Translates the {@link Component}.
   *
   * @param locale The locale
   * @param component The component to translate
   * @return The translated component
   */
  public Component translateComponent(Locale locale, Component component) {
    checkNotNull(locale, "locale");
    checkNotNull(component, "component");
    return translateComponent(getRegistry(locale), component, null);
  }

  /**
   * Translates the {@link Component}, returns the provided {@link Component}
   * if it's not needed to translate the component.
   *
   * @param registry The translation registry
   * @param component The component to translate
   * @param formattingContext The current formatting context
   * @return The translated component
   */
  private Component translateComponent(
      TranslationRegistry registry, Component component,
      @Nullable FormattingContext formattingContext) {
    Component translated = translateComponentIfNeeded(registry, component, formattingContext);
    return translated != null ? translated : component;
  }

  private final Pattern formatPattern = Pattern
      .compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

  /**
   * Represents a formatting context. This context is used
   * when formatting a translation with arguments.
   */
  private static class FormattingContext {

    private final List<Component> arguments;
    private int nextArgument;

    private FormattingContext(List<Component> arguments) {
      this.arguments = arguments;
    }

    Component argument(@Nullable String index) {
      int i = index == null ? this.nextArgument++ : Integer.parseInt(index);
      if (i >= 0 && i < this.arguments.size()) {
        return this.arguments.get(i);
      }
      return TextComponent.of("<unknown arg " + i + ">");
    }
  }

  /**
   * Gets whether the given {@link Component} is a plain text.
   *
   * @param component The component
   * @return Whether it is plain text
   */
  private static boolean isPlainComponent(Component component) {
    return component instanceof TextComponent
        && !component.hasStyling() && component.children().isEmpty();
  }

  /**
   * A improved text builder to reduce the
   * complexity and reduce component creation.
   */
  private static class TextAppender {

    private TextComponent.@Nullable Builder builder = null;
    private StringBuilder text = null;

    TextComponent.Builder builder() {
      if (this.builder == null) {
        this.builder = TextComponent.builder();
      }
      return this.builder;
    }

    void append(String text) {
      if (this.text == null) {
        this.text = new StringBuilder(text);
      } else {
        this.text.append(text);
      }
    }

    void append(String text, int start, int end) {
      if (this.text == null) {
        this.text = new StringBuilder(end - start + 16);
      }
      this.text.append(text, start, end);
    }

    void append(Component component) {
      if (isPlainComponent(component)) {
        append(((TextComponent) component).content());
      } else {
        appendRemaining();
        builder().append(component);
      }
    }

    void appendRemaining() {
      if (this.text != null && this.text.length() > 0) {
        String value = this.text.toString();
        this.text.setLength(0);
        if (this.builder == null) {
          this.builder = TextComponent.builder(value);
        } else {
          this.builder.append(value);
        }
      }
    }
  }

  private @Nullable HoverEvent translateHoverEventIfNeeded(
      TranslationRegistry registry, HoverEvent hoverEvent,
      @Nullable FormattingContext formattingContext) {
    Component translated = translateComponentIfNeeded(
        registry, hoverEvent.value(), formattingContext);
    if (translated != null) {
      return HoverEvent.of(hoverEvent.action(), translated);
    }
    return null;
  }

  private @Nullable Component translateComponentIfNeeded(
      TranslationRegistry registry, Component component,
      @Nullable FormattingContext formattingContext) {
    List<Component> children = component.children();
    HoverEvent hoverEvent = component.style().hoverEvent();

    if (formattingContext != null && component instanceof TextComponent) {
      TextComponent textComponent = (TextComponent) component;
      TextAppender appender = null;

      String content = textComponent.content();
      Matcher matcher = this.formatPattern.matcher(content);

      int end = 0;
      while (matcher.find()) {
        if (appender == null) {
          appender = new TextAppender();
        }
        int start = matcher.start();
        if (start != end) {
          appender.append(content, end, start);
        }
        end = matcher.end();

        char code = matcher.group(2).charAt(0);
        if (code == 's') {
          Component argument = formattingContext.argument(matcher.group(1));
          appender.append(translateComponent(registry, argument, null));
        } else if (code == '%') {
          appender.append("%");
        } else {
          throw new UnsupportedFormatException("Invalid format: " + content.substring(start, end));
        }
      }

      if (appender != null) {
        if (end != content.length()) {
          appender.append(content, end, content.length());
        }

        appender.appendRemaining();
        TextComponent.Builder builder = appender.builder();
        builder.style(component.style());

        List<Component> translatedChildren = translateComponentsIfNeeded(
            registry, children, formattingContext);
        builder.append(translatedChildren != null ? translatedChildren : children);

        if (hoverEvent != null) {
          HoverEvent translatedHoverEvent = translateHoverEventIfNeeded(
              registry, hoverEvent, formattingContext);
          builder.hoverEvent(translatedHoverEvent != null ? translatedHoverEvent : hoverEvent);
        }

        return builder.build();
      }
    }

    List<Component> translatedChildren = translateComponentsIfNeeded(
        registry, children, formattingContext);

    HoverEvent translatedHoverEvent = hoverEvent != null ? translateHoverEventIfNeeded(
        registry, hoverEvent, formattingContext) : null;

    if (component instanceof TranslatableComponent) {
      TranslatableComponent translatable = (TranslatableComponent) component;

      String key = translatable.key();
      Translation translation = registry.get(key);
      if (translation != null) {
        Component formatted;
        if (translation instanceof FixedTranslation) {
          FixedTranslation fixedTranslation = (FixedTranslation) translation;
          formatted = fixedTranslation.component;
        } else {
          List<Component> arguments = translatable.args();
          FormattingContext context = new FormattingContext(arguments);
          formatted = translateComponent(registry, translation.component, context);
        }
        if (children.isEmpty()) {
          if (!component.hasStyling()) {
            return formatted;
          }
        } else {
          children = translatedChildren != null ? translatedChildren :
              new ArrayList<>(children);
          children.addAll(0, formatted.children());
          formatted = formatted.children(children);
        }
        if (translatedHoverEvent != null) {
          formatted = formatted.hoverEvent(translatedHoverEvent);
        }
        return formatted.style(mergeStyle(component.style(), formatted.style()));
      }
    }

    if (translatedChildren != null || translatedHoverEvent != null) {
      if (translatedChildren != null) {
        component = component.children(translatedChildren);
      }
      if (translatedHoverEvent != null) {
        component = component.hoverEvent(translatedHoverEvent);
      }
      return component;
    }

    return null;
  }

  // TODO: Replace with text library method once added
  private static Style mergeStyle(Style first, Style second) {
    first = first.mergeDecorations(second);

    TextColor color = second.color();
    if (color != null) {
      first = first.color(color);
    }

    HoverEvent hoverEvent = second.hoverEvent();
    if (hoverEvent != null) {
      first = first.hoverEvent(hoverEvent);
    }

    ClickEvent clickEvent = second.clickEvent();
    if (clickEvent != null) {
      first = first.clickEvent(clickEvent);
    }

    String insertion = second.insertion();
    return insertion != null ? first.insertion(insertion) : first;
  }

  private @Nullable List<Component> translateComponentsIfNeeded(
      TranslationRegistry registry, List<Component> components,
      @Nullable FormattingContext formattingContext) {
    List<Component> modified = null;
    for (int i = 0; i < components.size(); i++) {
      Component component = components.get(i);
      Component translated = translateComponentIfNeeded(registry, component, formattingContext);
      if (translated != null) {
        if (modified == null) {
          modified = new ArrayList<>(components);
        }
        modified.set(i, translated);
      }
    }
    return modified;
  }
}
