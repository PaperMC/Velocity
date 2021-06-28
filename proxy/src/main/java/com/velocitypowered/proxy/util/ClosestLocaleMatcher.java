package com.velocitypowered.proxy.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClosestLocaleMatcher {
  public static final ClosestLocaleMatcher INSTANCE = new ClosestLocaleMatcher();

  private final Map<String, Locale> byLanguage;
  private final LoadingCache<Locale, Locale> closest;

  private ClosestLocaleMatcher() {
    this.byLanguage = new ConcurrentHashMap<>();
    this.closest = Caffeine.newBuilder()
        .build(sublocale -> {
          final String tag = sublocale.getLanguage();
          return byLanguage.getOrDefault(tag, sublocale);
        });
  }

  public void registerKnown(final Locale locale) {
    if (locale.getLanguage().equals(new Locale("zh").getLanguage())) {
      return;
    }

    this.byLanguage.put(locale.getLanguage(), locale);
  }

  public Locale lookupClosest(final Locale locale) {
    return closest.get(locale);
  }
}
