package com.velocitypowered.proxy.translation;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.Player;
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
import static com.google.common.base.Preconditions.checkNotNull;

public class TranslationManager {

  private static final Gson GSON = new Gson();

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

    TranslationRegistry registry = getOrCreateRegistry(locale);
    for (String key : resourceBundle.keySet()) {
      String value = resourceBundle.getString(key);

      Translation translation;
      if (hasNoArguments(value)) {
        translation = new FixedTranslation(value, TextComponent.of(value));
      } else {
        translation = new Translation(value);
      }
      registry.put(key, translation);
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
  public void addJson(Locale locale, JsonObject jsonObject) {
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
      String value = json.isJsonPrimitive() ? json.getAsString() : GSON.toJson(json);
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
      //noinspection RedundantStringFormatCall,ResultOfMethodCallIgnored
      String.format(format); // Check if any arguments are required for the translation
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
