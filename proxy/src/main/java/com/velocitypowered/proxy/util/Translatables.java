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

import com.velocitypowered.api.command.CommandSource;

import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public final class Translatables {
  private Translatables(){}

  /**
   * Send a translated info message to the console.
   * @param logger the logger
   * @param translatableComponent the translatable component
   * @param locale the locale desired
   * @param args the arguments, can be an empty array
   */
  public static void info(
      @NotNull Object logger,
      @NotNull TranslatableComponent translatableComponent,
      @NotNull Locale locale,
      @NotNull Component@NotNull... args) {

    if (logger instanceof org.apache.logging.log4j.Logger) {
      ((org.apache.logging.log4j.Logger)logger).info(parse(translatableComponent, locale, args));
    } else {
      ((Logger)logger).info(parse(translatableComponent, locale, args));
    }
  }

  /**
   * Send a translated warn message to console.
   * @param logger the logger
   * @param translatableComponent the translatable component
   * @param locale the locale desired
   * @param args the arguments, can be an empty array
   */
  public static void warn(
      @NotNull Object logger,
      @NotNull TranslatableComponent translatableComponent,
      @NotNull Locale locale,
      @NotNull Component@NotNull... args) {

    if (logger instanceof org.apache.logging.log4j.Logger) {
      ((org.apache.logging.log4j.Logger)logger).warn(parse(translatableComponent, locale, args));
    } else {
      ((Logger)logger).warn(parse(translatableComponent, locale, args));
    }
  }

  /**
   * Send a translated message to a CommandSource.
   * @param source the source
   * @param translatableComponent the translatable component
   * @param locale the locale desired
   * @param args the arguments, can be an empty array
   */
  public static void message(
      @NotNull CommandSource source,
      @NotNull TranslatableComponent translatableComponent,
      @NotNull Locale locale,
      @NotNull Component@NotNull... args) {

    source.sendMessage(GlobalTranslator.render(
        translatableComponent.args(args),
        ClosestLocaleMatcher.INSTANCE.lookupClosest(locale)
    ));
  }

  static String parse(TranslatableComponent translatableComponent, Locale locale, Component... args) {
    return LegacyComponentSerializer.legacySection().serialize(
        GlobalTranslator.render(
          translatableComponent.args(args),
          ClosestLocaleMatcher.INSTANCE.lookupClosest(locale)
        )
    );
  }

  static String parse(TranslatableComponent translatableComponent, Component... args) {
    return parse(translatableComponent, Locale.getDefault(), args);
  }
}
