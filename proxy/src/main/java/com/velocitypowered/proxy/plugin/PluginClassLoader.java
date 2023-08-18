/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.plugin;

import com.velocitypowered.proxy.Velocity;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The per-plugin class loader.
 */
public class PluginClassLoader extends URLClassLoader {

  private static final Set<PluginClassLoader> loaders = new CopyOnWriteArraySet<>();

  static {
    ClassLoader.registerAsParallelCapable();
  }

  public PluginClassLoader(URL[] urls) {
    super(urls, Velocity.class.getClassLoader());
  }

  public void addToClassloaders() {
    loaders.add(this);
  }

  void addPath(Path path) {
    try {
      addURL(path.toUri().toURL());
    } catch (MalformedURLException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void close() throws IOException {
    loaders.remove(this);
    super.close();
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    return loadClass0(name, resolve, true);
  }

  private Class<?> loadClass0(String name, boolean resolve, boolean checkOther)
      throws ClassNotFoundException {
    try {
      return super.loadClass(name, resolve);
    } catch (ClassNotFoundException ignored) {
      // Ignored: we'll try others
    }

    if (checkOther) {
      for (PluginClassLoader loader : loaders) {
        if (loader != this) {
          try {
            return loader.loadClass0(name, resolve, false);
          } catch (ClassNotFoundException ignored) {
            // We're trying others, safe to ignore
          }
        }
      }
    }

    throw new ClassNotFoundException(name);
  }
}
