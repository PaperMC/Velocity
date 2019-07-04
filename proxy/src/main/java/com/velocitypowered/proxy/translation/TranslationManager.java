package com.velocitypowered.proxy.translation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TranslationManager {

  private static final Gson GSON = new Gson();

  /**
   * The fallback bundle, if no bundle or translation
   * was found for the a locale.
   */
  private @Nullable Map<String, Translation> fallback;

  private final Map<Locale, Map<String, Translation>> translations = new HashMap<>();

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

  /**
   * Adds a resource bundle for the given locale.
   *
   * <p>The first call to this method will also set the fallback locale.</p>
   *
   * @param locale The locale to register the resource bundle for
   * @param resourceBundle The resource bundle to add
   */
  public void addBundle(Locale locale, ResourceBundle resourceBundle) {
    Map<String, Translation> translations = this.translations
            .computeIfAbsent(locale, locale1 -> new HashMap<>());
    for (String key : resourceBundle.keySet()) {
      String value = resourceBundle.getString(key);

      Translation translation;
      if (!hasArguments(value)) {
        translation = new FixedTranslation(value, TextComponent.of(value));
      } else {
        translation = new Translation(value);
      }
      translations.put(key, translation);
    }
    if (fallback == null) {
      fallback = translations;
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
  public void addJson(Locale locale, JsonObject jsonObject) {
    Map<String, Translation> translations = this.translations
        .computeIfAbsent(locale, locale1 -> new HashMap<>());
    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
      String key = entry.getKey();
      JsonElement json = entry.getValue();
      if (json.isJsonNull()) {
        continue;
      }
      Translation translation;
      String value = json.isJsonPrimitive() ? json.getAsString() : GSON.toJson(json);
      if (!hasArguments(value)) {
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
      translations.put(key, translation);
    }
    if (fallback == null) {
      fallback = translations;
    }
  }

  private boolean hasArguments(String format) {
    try {
      //noinspection RedundantStringFormatCall,ResultOfMethodCallIgnored
      String.format(format); // Check if any arguments are required for the translation
      return false;
    } catch (IllegalFormatException ex) {
      return true;
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
    return translation == null ? null : String.format(locale, translation.plain, arguments);
  }

  private @Nullable Translation getTranslation(Locale locale, String key) {
    Map<String, Translation> translations = this.translations.get(locale);
    Translation translation = null;
    if (translations != null) {
      translation = translations.get(key);
    }
    if (translation == null) {
      if (this.fallback == null) {
        return null;
      }
      translation = this.fallback.get(key);
    }
    return translation;
  }

  /**
   * Translates the {@link Component}.
   *
   * @param locale The locale
   * @param component The component to translate
   * @return The translated component
   */
  public Component translateComponent(Locale locale, Component component) {
    Component translated = translateComponentIfNeeded(locale, component);
    return translated != null ? translated : component;
  }

  private @Nullable Component translateComponentIfNeeded(Locale locale, Component component) {
    List<Component> children = component.children();
    List<Component> translatedChildren = translateComponentsIfNeeded(locale, children);

    if (component instanceof TranslatableComponent) {
      TranslatableComponent translatable = (TranslatableComponent) component;

      String key = translatable.key();
      Translation translation = getTranslation(locale, key);
      if (translation != null) {
        List<Component> components = translatable.args();
        Object[] arguments = new Object[components.size()];
        for (int i = 0; i < components.size(); i++) {
          Component translated = translateComponent(locale, components.get(i));
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
              .deserialize(String.format(locale, componentTranslation.json, arguments));
          builder = TextComponent.builder().append(formatted);
        } else {
          builder = TextComponent.builder(String.format(locale, translation.plain, arguments));
        }
        return builder
                .style(component.style())
                .append(translatedChildren != null ? translatedChildren : children)
                .build();
      }
    }

    if (translatedChildren != null) {
      return component.children(translatedChildren);
    }

    return null;
  }

  private @Nullable List<Component> translateComponentsIfNeeded(Locale locale, List<Component> components) {
    List<Component> modified = null;
    for (int i = 0; i < components.size(); i++) {
      Component component = components.get(i);
      Component translated = translateComponentIfNeeded(locale, component);
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
