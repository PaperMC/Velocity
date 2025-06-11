/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.adventure;

import com.google.auto.service.AutoService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBarImplementation;

@AutoService(BossBarImplementation.Provider.class)
@SuppressWarnings("MissingJavadocType")
public class BossBarImplementationProvider implements BossBarImplementation.Provider {
  @Override
  public BossBarImplementation create(final BossBar bar) {
    final VelocityBossBarImplementation impl = new VelocityBossBarImplementation(bar);
    bar.addListener(impl);
    return impl;
  }
}
