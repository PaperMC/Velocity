/*
 * Copyright (C) 2026 Velocity Contributors
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClosestLocaleMatcherTest {

  private static final Locale PIRATE = Locale.forLanguageTag("en-PT");

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void preservesExactLocalesInEitherRegistrationOrder(boolean usFirst) {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(usFirst ? Locale.US : PIRATE);
    matcher.registerKnown(usFirst ? PIRATE : Locale.US);

    assertEquals(Locale.US, matcher.lookupClosest(Locale.US));
    assertEquals(PIRATE, matcher.lookupClosest(PIRATE));
  }

  @Test
  void fallsBackToKnownLocaleForSameLanguage() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(Locale.US);

    assertEquals(Locale.US, matcher.lookupClosest(Locale.UK));
  }

  @Test
  void keepsUnknownLanguage() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(Locale.US);

    assertEquals(Locale.GERMANY, matcher.lookupClosest(Locale.GERMANY));
  }

  @Test
  void usesLocaleRegisteredAfterAnEarlierLookup() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    assertEquals(Locale.UK, matcher.lookupClosest(Locale.UK));

    matcher.registerKnown(Locale.US);

    assertEquals(Locale.US, matcher.lookupClosest(Locale.UK));
  }

  @Test
  void prefersExactLocaleRegisteredAfterFallbackLookup() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(Locale.US);
    assertEquals(Locale.US, matcher.lookupClosest(PIRATE));

    matcher.registerKnown(PIRATE);

    assertEquals(PIRATE, matcher.lookupClosest(PIRATE));
    assertEquals(Locale.US, matcher.lookupClosest(Locale.US));
  }

  @Test
  void keepsChineseRegionalVariantsSeparate() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(Locale.SIMPLIFIED_CHINESE);

    assertEquals(Locale.TRADITIONAL_CHINESE,
        matcher.lookupClosest(Locale.TRADITIONAL_CHINESE));
    assertEquals(Locale.SIMPLIFIED_CHINESE,
        matcher.lookupClosest(Locale.SIMPLIFIED_CHINESE));
  }
}
