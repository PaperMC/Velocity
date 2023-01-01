/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player;

import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.crypto.KeyIdentifiable;
import com.velocitypowered.api.util.GameProfile;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a single entry in a {@link TabList}.
 */
public interface TabListEntry extends KeyIdentifiable {
  /**
   * Returns the {@link ChatSession} associated with this entry.
   *
   * @return the chat session
   */
  @Nullable ChatSession getChatSession();

  @Override
  default IdentifiedKey getIdentifiedKey() {
    ChatSession session = getChatSession();
    if (session == null) {
      return null;
    }
    return getChatSession().getIdentifiedKey();
  }

  /**
   * Returns the parent {@link TabList} of this {@code this} {@link TabListEntry}.
   *
   * @return parent {@link TabList}
   */
  TabList getTabList();

  /**
   * Returns the {@link GameProfile} of the entry, which uniquely identifies the entry with the
   * containing {@link java.util.UUID}, as well as deciding what is shown as the player head in the
   * tab list.
   *
   * @return {@link GameProfile} of the entry
   */
  GameProfile getProfile();

  /**
   * Returns {@link Optional} text {@link net.kyori.adventure.text.Component}, which if present is
   * the text displayed for {@code this} entry in the {@link TabList}, otherwise
   * {@link GameProfile#getName()} is shown.
   *
   * @return {@link Optional} text {@link net.kyori.adventure.text.Component} of name displayed in
   *     the tab list
   */
  Optional<Component> getDisplayNameComponent();

  /**
   * Sets the text {@link Component} to be displayed for {@code this} {@link TabListEntry}. If
   * {@code null}, {@link GameProfile#getName()} will be shown.
   *
   * @param displayName to show in the {@link TabList} for {@code this} entry
   * @return {@code this}, for chaining
   */
  TabListEntry setDisplayName(@Nullable Component displayName);

  /**
   * Returns the latency for {@code this} entry.
   *
   * <p>The icon shown in the tab list is calculated by the latency as follows:</p>
   *
   * <ul>
   *  <li>A negative latency will display the no connection icon</li>
   *  <li>0-150 will display 5 bars</li>
   *  <li>150-300 will display 4 bars</li>
   *  <li>300-600 will display 3 bars</li>
   *  <li>600-1000 will display 2 bars</li>
   *  <li>A latency move than 1 second will display 1 bar</li>
   * </ul>
   *
   * @return latency set for {@code this} entry
   */
  int getLatency();

  /**
   * Sets the latency for {@code this} entry to the specified value.
   *
   * @param latency to changed to
   * @return {@code this}, for chaining
   * @see #getLatency()
   */
  TabListEntry setLatency(int latency);

  /**
   * Gets the game mode {@code this} entry has been set to.
   *
   * <p>The number corresponds to the game mode in the following way:</p>
   * <ol start="0">
   * <li>Survival</li>
   * <li>Creative</li>
   * <li>Adventure</li>
   * <li>Spectator</li>
   * </ol>
   *
   * @return the game mode
   */
  int getGameMode();

  /**
   * Sets the game mode for {@code this} entry to the specified value.
   *
   * @param gameMode to change to
   * @return {@code this}, for chaining
   * @see #getGameMode()
   */
  TabListEntry setGameMode(int gameMode);

  /**
   * Returns whether or not this player will be visible to other players in the tab list.
   *
   * @return Whether this entry is listed; only changeable in 1.19.3 and above
   */
  default boolean isListed() {
    return true;
  }

  /**
   * Sets whether this entry is listed.
   *
   * @param listed whether this entry is listed
   * @return {@code this}, for chaining
   */
  default TabListEntry setListed(boolean listed) {
    return this;
  }

  /**
   * Returns a {@link Builder} to create a {@link TabListEntry}.
   *
   * @return {@link TabListEntry} builder
   */
  static Builder builder() {
    return new Builder();
  }

  /**
   * Represents a builder which creates {@link TabListEntry}s.
   *
   * @see TabListEntry
   */
  class Builder {

    private @Nullable TabList tabList;
    private @Nullable GameProfile profile;
    private @Nullable Component displayName;
    private int latency = 0;
    private int gameMode = 0;
    private boolean listed = true;

    private @Nullable ChatSession chatSession;

    private Builder() {
    }

    /**
     * Sets the parent {@link TabList} for this entry, the entry will only be able to be added to
     * that specific {@link TabList}.
     *
     * @param tabList to set
     * @return {@code this}, for chaining
     */
    public Builder tabList(TabList tabList) {
      this.tabList = tabList;
      return this;
    }

    /**
     * Sets the {@link GameProfile} of the {@link TabListEntry}.
     *
     * @param profile to set
     * @return {@code this}, for chaining
     * @see TabListEntry#getProfile()
     */
    public Builder profile(GameProfile profile) {
      this.profile = profile;
      return this;
    }

    /**
     * Sets the {@link IdentifiedKey} of the {@link TabListEntry}.
     * <p>This only works for players currently <b>not</b> connected to this proxy.</p>
     * <p>For any player currently connected to this proxy this will be filled automatically.</p>
     * <p>Will ignore mismatching key revisions data.</p>
     *
     * @param chatSession session to set
     * @return {@code this}, for chaining
     * @see TabListEntry#getChatSession()
     */
    public Builder chatSession(ChatSession chatSession) {
      this.chatSession = chatSession;
      return this;
    }

    /**
     * Sets the displayed name of the {@link TabListEntry}.
     *
     * @param displayName to set
     * @return {@code this}, for chaining
     * @see TabListEntry#getDisplayNameComponent() ()
     */
    public Builder displayName(@Nullable Component displayName) {
      this.displayName = displayName;
      return this;
    }

    /**
     * Sets the latency of the {@link TabListEntry}.
     *
     * @param latency to set
     * @return {@code this}, for chaining
     * @see TabListEntry#getLatency()
     */
    public Builder latency(int latency) {
      this.latency = latency;
      return this;
    }

    /**
     * Sets the game mode of the {@link TabListEntry}.
     *
     * @param gameMode to set
     * @return {@code this}, for chaining
     * @see TabListEntry#getGameMode()
     */
    public Builder gameMode(int gameMode) {
      this.gameMode = gameMode;
      return this;
    }

    /**
     * Sets wether this entry should be visible.
     *
     * @param listed to set
     * @return ${code this}, for chaining
     * @see TabListEntry#isListed()
     */
    public Builder listed(boolean listed) {
      this.listed = listed;
      return this;
    }

    /**
     * Constructs the {@link TabListEntry} specified by {@code this} {@link Builder}.
     *
     * @return the constructed {@link TabListEntry}
     */
    public TabListEntry build() {
      if (tabList == null) {
        throw new IllegalStateException("The Tablist must be set when building a TabListEntry");
      }
      if (profile == null) {
        throw new IllegalStateException("The GameProfile must be set when building a TabListEntry");
      }
      return tabList.buildEntry(profile, displayName, latency, gameMode, chatSession, listed);
    }
  }
}
