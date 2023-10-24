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

package com.velocitypowered.proxy.util;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.Nullable;


/**
 * Velocity Translation Mapper.
 */
public enum TranslatableMapper implements BiConsumer<TranslatableComponent, Consumer<Component>> {
  INSTANCE;

  public static final ComponentFlattener FLATTENER = ComponentFlattener.basic().toBuilder()
          .complexMapper(TranslatableComponent.class, TranslatableMapper.INSTANCE)
          .build();

  @Override
  public void accept(
          final TranslatableComponent translatableComponent,
          final Consumer<Component> componentConsumer
  ) {
    for (final Translator source : GlobalTranslator.translator().sources()) {
      if (source instanceof TranslationRegistry
              && ((TranslationRegistry) source).contains(translatableComponent.key())) {
        componentConsumer.accept(GlobalTranslator.render(translatableComponent,
                ClosestLocaleMatcher.INSTANCE.lookupClosest(Locale.getDefault())));
        return;
      }
    }
    final @Nullable String fallback = translatableComponent.fallback();
    if (fallback == null) {
      return;
    }
    for (final Translator source : GlobalTranslator.translator().sources()) {
      if (source instanceof TranslationRegistry
              && ((TranslationRegistry) source).contains(fallback)) {
        componentConsumer.accept(
                GlobalTranslator.render(Component.translatable(fallback),
                        ClosestLocaleMatcher.INSTANCE.lookupClosest(Locale.getDefault())));
        return;
      }
    }
  }
}
