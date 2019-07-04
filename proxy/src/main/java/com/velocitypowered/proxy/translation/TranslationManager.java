package com.velocitypowered.proxy.translation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.Player;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.HoverEvent;
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

    final String plain;

    Translation(String plain) {
      this.plain = plain;
    }
  }

  /**
   * Represents a fixed translation.
   */
  class FixedTranslation extends Translation {

    final Component component;
    final boolean plainComponent;

    FixedTranslation(String plain, Component component) {
      super(plain);
      this.component = component;
      this.plainComponent = component instanceof TextComponent
          && component.children().isEmpty() && !component.hasStyling();
    }
  }

  /**
   * A translation that is backed by text json.
   */
  class ComponentTranslation extends Translation {

    final String json;

    ComponentTranslation(String json, String plain) {
      super(plain);
      this.json = json;
    }
  }

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
      String fileName = file.getFileName().toString();
      int index = fileName.lastIndexOf('.');
      if (index == -1) {
        return;
      }
      String extension = fileName.substring(index + 1);
      Locale locale = Locale.forLanguageTag(fileName.substring(0, index).replace('_', '-'));
      logger.info("Loading file: " + fileName.substring(0, index) + " -> " + locale);
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

      Translation translation;
      if (hasNoArguments(value)) {
        translation = new FixedTranslation(value, TextComponent.of(value));
      } else {
        translation = new Translation(value);
      }
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
      JsonElement json = entry.getValue();
      if (json.isJsonNull()) {
        continue;
      }
      Translation translation;
      String value = json.isJsonPrimitive() ? json.getAsString() : gson.toJson(json);
      if (hasNoArguments(value)) {
        if (json.isJsonPrimitive()) {
          translation = new FixedTranslation(value, TextComponent.of(value));
        } else {
          translation = new FixedTranslation(value, GsonComponentSerializer.INSTANCE
              .deserialize(value));
        }
      } else {
        if (json.isJsonPrimitive()) {
          translation = new Translation(value);
        } else {
          String plain = PlainComponentSerializer.INSTANCE
              .serialize(GsonComponentSerializer.INSTANCE.deserialize(value));
          translation = new ComponentTranslation(value, plain);
        }
      }
      registry.put(key, translation);
    }
    if (fallback == null) {
      fallback = registry;
    }
  }

  private boolean hasNoArguments(String format) {
    try {
      //noinspection RedundantStringFormatCall,unused
      String ignored = String.format(format);
      return true;
    } catch (IllegalFormatException ex) {
      return false;
    }
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

    String translated = translateIfFound(locale, key, arguments);
    return translated != null ? translated : key;
  }

  /**
   * Translates the translation for the given key and arguments.
   *
   * @param locale The locale
   * @param key The key of the translation
   * @param arguments The arguments
   * @return The translated string, if the key is supported, otherwise null
   */
  private @Nullable String translateIfFound(Locale locale, String key, Object... arguments) {
    Translation translation = getTranslation(locale, key);
    return translation == null ? null : String.format(translation.plain, arguments);
  }

  private @Nullable Translation getTranslation(Locale locale, String key) {
    return getRegistry(locale).get(key);
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
    return translateComponent(player.getPlayerSettings().getLocale(), component);
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
    return translateComponent(getRegistry(locale), component);
  }

  private Component translateComponent(
      TranslationRegistry registry, Component component) {
    Component translated = translateComponentIfNeeded(registry, component);
    return translated != null ? translated : component;
  }

  private @Nullable Component translateComponentIfNeeded(
      TranslationRegistry registry, Component component) {
    List<Component> children = component.children();
    List<Component> translatedChildren = translateComponentsIfNeeded(registry, children);

    HoverEvent hoverEvent = component.style().hoverEvent();
    HoverEvent translatedHoverEvent = null;
    if (hoverEvent != null) {
      Component translated = translateComponentIfNeeded(registry, hoverEvent.value());
      if (translated != null) {
        translatedHoverEvent = HoverEvent.of(hoverEvent.action(), translated);
      }
    }

    if (component instanceof TranslatableComponent) {
      TranslatableComponent translatable = (TranslatableComponent) component;

      String key = translatable.key();
      Translation translation = registry.get(key);
      if (translation != null) {
        List<Component> components = translatable.args();
        Object[] arguments = new Object[components.size()];
        for (int i = 0; i < components.size(); i++) {
          Component translated = translateComponent(registry, components.get(i));
          arguments[i] = PlainComponentSerializer.INSTANCE.serialize(translated);
        }
        TextComponent.Builder builder;
        if (translation instanceof FixedTranslation) {
          FixedTranslation fixedTranslation = (FixedTranslation) translation;
          Component fixed = fixedTranslation.component;
          if (children.isEmpty() && !component.hasStyling()) {
            return fixed;
          }
          if (fixedTranslation.plainComponent) {
            builder = TextComponent.builder(((TextComponent) fixed).content());
          } else {
            builder = TextComponent.builder().append(fixed);
          }
        } else if (translation instanceof ComponentTranslation) {
          ComponentTranslation componentTranslation = (ComponentTranslation) translation;
          Component formatted = GsonComponentSerializer.INSTANCE
              .deserialize(String.format(componentTranslation.json, arguments));
          builder = TextComponent.builder().append(formatted);
        } else {
          builder = TextComponent.builder(String.format(translation.plain, arguments));
        }
        builder
            .style(component.style())
            .append(translatedChildren != null ? translatedChildren : children);
        if (translatedHoverEvent != null) {
          builder.hoverEvent(translatedHoverEvent);
        }
        return builder.build();
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

  private @Nullable List<Component> translateComponentsIfNeeded(
      TranslationRegistry registry, List<Component> components) {
    List<Component> modified = null;
    for (int i = 0; i < components.size(); i++) {
      Component component = components.get(i);
      Component translated = translateComponentIfNeeded(registry, component);
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
