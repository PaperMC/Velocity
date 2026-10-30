/*
 * Copyright (C) 2021-2023 Velocity Contributors
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Matches a player's locale to the "closest" Velocity locale, for message localization.
 */
public class ClosestLocaleMatcher {

  public static final ClosestLocaleMatcher INSTANCE = new ClosestLocaleMatcher();

  private final Map<String, Locale> byLanguage;
  private final Set<Locale> knownLocales;

  ClosestLocaleMatcher() {
    this.byLanguage = new ConcurrentHashMap<>();
    this.knownLocales = ConcurrentHashMap.newKeySet();
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

    this.knownLocales.add(locale);
    this.byLanguage.put(locale.getLanguage(), locale);
  }

  /**
   * Returns an exact known locale before trying a locale with the same language.
   *
   * @param locale the requested locale
   * @return the matched locale, or the requested locale if no match is known
   */
  public Locale lookupClosest(final Locale locale) {
    if (knownLocales.contains(locale)) {
      return locale;
    }
    return byLanguage.getOrDefault(locale.getLanguage(), locale);
  }
}
