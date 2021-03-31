/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util.title;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.Optional;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a "full" title, including all components. This class is immutable.
 *
 * @deprecated Replaced with {@link net.kyori.adventure.title.Title}
 */
@Deprecated
public final class TextTitle implements Title {

  private final @Nullable Component title;
  private final @Nullable Component subtitle;
  private final int stay;
  private final int fadeIn;
  private final int fadeOut;
  private final boolean resetBeforeSend;

  private TextTitle(Builder builder) {
    this.title = builder.title;
    this.subtitle = builder.subtitle;
    this.stay = builder.stay;
    this.fadeIn = builder.fadeIn;
    this.fadeOut = builder.fadeOut;
    this.resetBeforeSend = builder.resetBeforeSend;
  }

  /**
   * Returns the main title this title has, if any.
   *
   * @return the main title of this title
   */
  public Optional<Component> getTitle() {
    return Optional.ofNullable(title);
  }

  /**
   * Returns the subtitle this title has, if any.
   *
   * @return the subtitle
   */
  public Optional<Component> getSubtitle() {
    return Optional.ofNullable(subtitle);
  }

  /**
   * Returns the number of ticks this title will stay up.
   *
   * @return how long the title will stay, in ticks
   */
  public int getStay() {
    return stay;
  }

  /**
   * Returns the number of ticks over which this title will fade in.
   *
   * @return how long the title will fade in, in ticks
   */
  public int getFadeIn() {
    return fadeIn;
  }

  /**
   * Returns the number of ticks over which this title will fade out.
   *
   * @return how long the title will fade out, in ticks
   */
  public int getFadeOut() {
    return fadeOut;
  }

  /**
   * Returns whether or not a reset packet will be sent before this title is sent. By default,
   * unless explicitly disabled, this is enabled by default.
   *
   * @return whether or not a reset packet will be sent before this title is sent
   */
  public boolean isResetBeforeSend() {
    return resetBeforeSend;
  }

  /**
   * Determines whether or not this title has times set on it. If none are set, it will update the
   * previous title set on the client.
   *
   * @return whether or not this title has times set on it
   */
  public boolean areTimesSet() {
    return stay != 0 || fadeIn != 0 || fadeOut != 0;
  }

  /**
   * Creates a new builder from the contents of this title so that it may be changed.
   *
   * @return a builder instance with the contents of this title
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TextTitle textTitle = (TextTitle) o;
    return stay == textTitle.stay
        && fadeIn == textTitle.fadeIn
        && fadeOut == textTitle.fadeOut
        && resetBeforeSend == textTitle.resetBeforeSend
        && Objects.equals(title, textTitle.title)
        && Objects.equals(subtitle, textTitle.subtitle);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("title", title)
        .add("subtitle", subtitle)
        .add("stay", stay)
        .add("fadeIn", fadeIn)
        .add("fadeOut", fadeOut)
        .add("resetBeforeSend", resetBeforeSend)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, subtitle, stay, fadeIn, fadeOut, resetBeforeSend);
  }

  /**
   * Creates a new builder for constructing titles.
   *
   * @return a builder for constructing titles
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private @Nullable Component title;
    private @Nullable Component subtitle;
    private int stay;
    private int fadeIn;
    private int fadeOut;
    private boolean resetBeforeSend = true;

    private Builder() {
    }

    private Builder(TextTitle copy) {
      this.title = copy.title;
      this.subtitle = copy.subtitle;
      this.stay = copy.stay;
      this.fadeIn = copy.fadeIn;
      this.fadeOut = copy.fadeOut;
      this.resetBeforeSend = copy.resetBeforeSend;
    }

    public Builder title(Component title) {
      this.title = Preconditions.checkNotNull(title, "title");
      return this;
    }

    public Builder clearTitle() {
      this.title = null;
      return this;
    }

    public Builder subtitle(Component subtitle) {
      this.subtitle = Preconditions.checkNotNull(subtitle, "subtitle");
      return this;
    }

    public Builder clearSubtitle() {
      this.subtitle = null;
      return this;
    }

    private int checkTicks(int ticks) {
      Preconditions.checkArgument(ticks >= 0, "ticks value %s is negative", ticks);
      return ticks;
    }

    public Builder stay(int ticks) {
      this.stay = checkTicks(ticks);
      return this;
    }

    public Builder fadeIn(int ticks) {
      this.fadeIn = checkTicks(ticks);
      return this;
    }

    public Builder fadeOut(int ticks) {
      this.fadeOut = checkTicks(ticks);
      return this;
    }

    public Builder resetBeforeSend(boolean b) {
      this.resetBeforeSend = b;
      return this;
    }

    public Optional<Component> getTitle() {
      return Optional.ofNullable(title);
    }

    public Optional<Component> getSubtitle() {
      return Optional.ofNullable(subtitle);
    }

    public int getStay() {
      return stay;
    }

    public int getFadeIn() {
      return fadeIn;
    }

    public int getFadeOut() {
      return fadeOut;
    }

    public boolean isResetBeforeSend() {
      return resetBeforeSend;
    }

    public TextTitle build() {
      return new TextTitle(this);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("title", title)
          .add("subtitle", subtitle)
          .add("stay", stay)
          .add("fadeIn", fadeIn)
          .add("fadeOut", fadeOut)
          .add("resetBeforeSend", resetBeforeSend)
          .toString();
    }
  }
}
