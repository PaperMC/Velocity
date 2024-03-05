/*
 * Copyright (C) 2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.util.translation;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.translation.TranslationRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Velocity Translation Registry.
 * Based on <a href="https://github.com/KyoriPowered/adventure/pull/972">Adventure PR</a>.
 * MIT Licenced.
 */
public final class VelocityTranslationRegistry implements TranslationRegistry {
  private final TranslationRegistry backedRegistry;

  public VelocityTranslationRegistry(final TranslationRegistry backed) {
    this.backedRegistry = backed;
  }

  @Override
  public boolean contains(@NotNull String key) {
    return backedRegistry.contains(key);
  }

  @Override
  public @NotNull Key name() {
    return backedRegistry.name();
  }

  @Override
  public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
    return null;
  }

  @Override
  public @Nullable Component translate(
          @NotNull TranslatableComponent component,
          @NotNull Locale locale
  ) {
    final MessageFormat translationFormat = backedRegistry.translate(component.key(), locale);

    if (translationFormat == null) {
      return null;
    }

    final String miniMessageString = translationFormat.toPattern();

    final Component resultingComponent;

    if (component.args().isEmpty()) {
      resultingComponent = MiniMessage.miniMessage().deserialize(miniMessageString);
    } else {
      resultingComponent = MiniMessage.miniMessage().deserialize(miniMessageString,
              new ArgumentTag(component.args()));
    }

    if (component.children().isEmpty()) {
      return resultingComponent;
    } else {
      return resultingComponent.children(component.children());
    }
  }

  @Override
  public void defaultLocale(@NotNull Locale locale) {
    backedRegistry.defaultLocale(locale);
  }

  @Override
  public void register(@NotNull String key, @NotNull Locale locale, @NotNull MessageFormat format) {
    backedRegistry.register(key, locale, format);
  }

  @Override
  public void unregister(@NotNull String key) {
    backedRegistry.unregister(key);
  }

  private static final class ArgumentTag implements TagResolver {
    private static final String NAME = "argument";
    private static final String NAME_1 = "arg";

    private final List<? extends ComponentLike> argumentComponents;

    public ArgumentTag(final @NotNull List<? extends ComponentLike> argumentComponents) {
      this.argumentComponents = Objects.requireNonNull(argumentComponents, "argumentComponents");
    }

    @Override
    public Tag resolve(
            final @NotNull String name,
            final @NotNull ArgumentQueue arguments,
            final @NotNull Context ctx
    ) throws ParsingException {
      final int index = arguments.popOr("No argument number provided")
              .asInt().orElseThrow(() -> ctx.newException("Invalid argument number", arguments));

      if (index < 0 || index >= argumentComponents.size()) {
        throw ctx.newException("Invalid argument number", arguments);
      }

      return Tag.inserting(argumentComponents.get(index));
    }

    @Override
    public boolean has(final @NotNull String name) {
      return name.equals(NAME) || name.equals(NAME_1);
    }
  }
}
