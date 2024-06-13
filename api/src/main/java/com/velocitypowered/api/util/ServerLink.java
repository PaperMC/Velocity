/*
 * Copyright (C) 2021-2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a custom URL servers can show in player pause menus.
 * Links can be of a built-in type or use a custom component text label.
 */
public final class ServerLink {

  private @Nullable Type type;
  private @Nullable Component label;
  private final String link;

  /**
   * Construct a server link with a custom component label.
   *
   * @param label the label to display
   * @param link  the URL to open when clicked
   */
  public ServerLink(Component label, String link) throws IllegalArgumentException {
    this.label = Preconditions.checkNotNull(label, "label");
    this.link = URI.create(link).toString();
  }

  /**
   * Construct a server link with a built-in type.
   *
   * @param type the {@link Type type} of link
   * @param link the URL to open when clicked
   */
  public ServerLink(Type type, String link) throws IllegalArgumentException {
    this.type = Preconditions.checkNotNull(type, "type");
    this.link = URI.create(link).toString();
  }

  /**
   * Get the type of the server link.
   *
   * @return the type of the server link
   */
  public Optional<Type> getBuiltInType() {
    return Optional.ofNullable(type);
  }

  /**
   * Get the link URL.
   *
   * @return the link URL
   */
  public String getLink() {
    return link;
  }

  /**
   * Get the custom component label of the server link.
   *
   * @return the custom component label of the server link
   */
  public Optional<Component> getCustomLabel() {
    return Optional.ofNullable(label);
  }

  /**
   * Built-in types of server links.
   *
   * @apiNote {@link Type#BUG_REPORT} links are shown on the connection error screen
   */
  public enum Type {
    BUG_REPORT,
    COMMUNITY_GUIDELINES,
    SUPPORT,
    STATUS,
    FEEDBACK,
    COMMUNITY,
    WEBSITE,
    FORUMS,
    NEWS,
    ANNOUNCEMENTS
  }

}
