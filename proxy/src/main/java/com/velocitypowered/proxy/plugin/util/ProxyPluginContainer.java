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

package com.velocitypowered.proxy.plugin.util;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.proxy.VelocityServer;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ProxyPluginContainer implements PluginContainer {

  public static final PluginContainer VELOCITY = new ProxyPluginContainer();

  private final PluginDescription description = new PluginDescription() {
    @Override
    public String id() {
      return "velocity";
    }

    @Override
    public String name() {
      final Package pkg = VelocityServer.class.getPackage();
      return MoreObjects.firstNonNull(pkg.getImplementationTitle(), "Velocity");
    }

    @Override
    public String version() {
      final Package pkg = VelocityServer.class.getPackage();
      return MoreObjects.firstNonNull(pkg.getImplementationVersion(), "<unknown>");
    }

    @Override
    public List<String> authors() {
      final Package pkg = VelocityServer.class.getPackage();
      final String vendor = MoreObjects.firstNonNull(pkg.getImplementationVendor(),
          "Velocity Contributors");
      return List.of(vendor);
    }
  };

  @Override
  public PluginDescription description() {
    return this.description;
  }

  @Override
  public @Nullable Object instance() {
    return null;
  }
}
