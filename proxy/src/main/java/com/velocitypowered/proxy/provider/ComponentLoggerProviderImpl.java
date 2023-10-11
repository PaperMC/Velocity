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

package com.velocitypowered.proxy.provider;

import com.velocitypowered.proxy.util.TranslatableMapper;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.logger.slf4j.ComponentLoggerProvider;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

/**
 * Velocity ComponentLogger Provider.
 */
@SuppressWarnings("UnstableApiUsage")
public final class ComponentLoggerProviderImpl implements ComponentLoggerProvider {
  private static final ANSIComponentSerializer SERIALIZER = ANSIComponentSerializer.builder()
          .flattener(TranslatableMapper.FLATTENER)
          .build();

  @Override
  public @NotNull ComponentLogger logger(
          final @NotNull LoggerHelper helper,
          final @NotNull String name
  ) {
    return helper.delegating(LoggerFactory.getLogger(name), SERIALIZER::serialize);
  }
}
