/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package top.notcoral.velocity.config;

/**
 * Selects how the F3 server brand (the text in the upper-right of the debug screen) is rendered.
 *
 * <p>{@link #DEFAULT} keeps upstream behavior — the backend brand is appended with the proxy name,
 * e.g. {@code "paper (Velocity)"}. {@link #CUSTOM} replaces it entirely with the configured
 * {@code custom-brand} string, parsed through MiniMessage so colors and decorations can be applied.
 */
public enum BrandMode {
  /** Upstream behavior: append the proxy name to the backend's brand. */
  DEFAULT,
  /** Replace the brand with the configured {@code custom-brand} MiniMessage string. */
  CUSTOM
}
