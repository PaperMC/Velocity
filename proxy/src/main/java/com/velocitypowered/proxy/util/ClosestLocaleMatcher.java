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
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Matches a player's locale to the "closest" Velocity locale, for message localization.
 */
public class ClosestLocaleMatcher {

  public static final ClosestLocaleMatcher INSTANCE = new ClosestLocaleMatcher();

  private final Set<Locale> known;
  private final Map<String, Locale> byLanguage;

  @VisibleForTesting
  ClosestLocaleMatcher() {
    this.known = ConcurrentHashMap.newKeySet();
    this.byLanguage = new ConcurrentHashMap<>();
  }

  /**
   * Registers a known locale.
   *
   * @param locale locale to register
   */
  public void registerKnown(final Locale locale) {
    this.known.add(locale);
    if (locale.getLanguage().equals(Locale.CHINESE.getLanguage())) {
      return;
    }

    this.byLanguage.put(locale.getLanguage(), locale);
  }

  /**
   * Finds the closest known locale.
   *
   * @param locale locale to look up
   * @return closest known locale
   */
  public Locale lookupClosest(final Locale locale) {
    if (this.known.contains(locale)) {
      return locale;
    }
    return this.byLanguage.getOrDefault(locale.getLanguage(), locale);
  }
}
