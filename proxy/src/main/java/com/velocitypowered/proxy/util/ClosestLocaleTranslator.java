/*
 * Copyright (C) 2018 Velocity Contributors
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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClosestLocaleTranslator implements Translator {
  private final Translator delegate;
  private final Map<String, Locale> byLanguage;
  private final LoadingCache<Locale, Locale> closest;

  public ClosestLocaleTranslator(final Translator delegate) {
    this.delegate = delegate;
    this.byLanguage = new ConcurrentHashMap<>();
    this.closest = Caffeine.newBuilder()
        .build(sublocale -> {
          final String tag = sublocale.getLanguage();
          return byLanguage.getOrDefault(tag, sublocale);
        });
  }

  /**
   * Registers a known locale.
   *
   * @param locale locale to register
   */
  public void registerKnown(final Locale locale) {
    if (locale.getLanguage().equals(new Locale("zh").getLanguage())) {
      return;
    }

    this.byLanguage.put(locale.getLanguage(), locale);
  }

  public Locale lookupClosest(final Locale locale) {
    return closest.get(locale);
  }

  @Override
  public @NotNull Key name() {
    return delegate.name();
  }

  @Override
  public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
    return delegate.translate(key, lookupClosest(locale));
  }
}
