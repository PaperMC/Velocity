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

class ClosestLocaleMatcherTest {

  private static final Locale EN_US = Locale.US;
  private static final Locale EN_PT = Locale.forLanguageTag("en-PT");
  private static final Locale EN_GB = Locale.UK;
  private static final Locale EN_AU = Locale.forLanguageTag("en-AU");

  @Test
  void exactLocalesMatchThemselvesWhenUsRegisteredFirst() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(EN_US);
    matcher.registerKnown(EN_PT);

    assertEquals(EN_US, matcher.lookupClosest(EN_US));
    assertEquals(EN_PT, matcher.lookupClosest(EN_PT));
  }

  @Test
  void exactLocalesMatchThemselvesWhenPirateRegisteredFirst() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(EN_PT);
    matcher.registerKnown(EN_US);

    assertEquals(EN_US, matcher.lookupClosest(EN_US));
    assertEquals(EN_PT, matcher.lookupClosest(EN_PT));
  }

  @Test
  void unknownVariantFallsBackToSameLanguage() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(EN_US);
    matcher.registerKnown(Locale.GERMANY);

    assertEquals(EN_US, matcher.lookupClosest(EN_GB));
    assertEquals(EN_US, matcher.lookupClosest(EN_AU));
    assertEquals(Locale.GERMANY, matcher.lookupClosest(Locale.forLanguageTag("de-AT")));
  }

  @Test
  void unknownLanguageReturnsRequestedLocale() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(EN_US);

    assertEquals(Locale.FRANCE, matcher.lookupClosest(Locale.FRANCE));
  }

  @Test
  void chineseVariantsDoNotFallBackToEachOther() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    matcher.registerKnown(Locale.SIMPLIFIED_CHINESE);
    matcher.registerKnown(Locale.TRADITIONAL_CHINESE);

    assertEquals(Locale.SIMPLIFIED_CHINESE, matcher.lookupClosest(Locale.SIMPLIFIED_CHINESE));
    assertEquals(Locale.TRADITIONAL_CHINESE, matcher.lookupClosest(Locale.TRADITIONAL_CHINESE));
    Locale hongKong = Locale.forLanguageTag("zh-HK");
    assertEquals(hongKong, matcher.lookupClosest(hongKong));
  }

  @Test
  void lateRegistrationUpdatesPreviousLookups() {
    ClosestLocaleMatcher matcher = new ClosestLocaleMatcher();
    assertEquals(EN_GB, matcher.lookupClosest(EN_GB));

    matcher.registerKnown(EN_US);
    assertEquals(EN_US, matcher.lookupClosest(EN_GB));
    assertEquals(EN_US, matcher.lookupClosest(EN_PT));

    matcher.registerKnown(EN_PT);
    assertEquals(EN_PT, matcher.lookupClosest(EN_PT));
    assertEquals(EN_US, matcher.lookupClosest(EN_US));
  }
}
