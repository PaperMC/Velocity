/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import com.velocitypowered.api.network.ProtocolVersion;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.builder.AbstractBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the characteristics with which a ping to a Minecraft server is simulated.
 * This class is immutable.
 *
 * @since 3.2.0
 */
public final class PingOptions {
  private final ProtocolVersion protocolVersion;
  private final long timeout;

  private PingOptions(final Builder builder) {
    this.protocolVersion = builder.protocolVersion;
    this.timeout = builder.timeout.toMillis();
  }

  /**
   * The version of Minecraft from which ping will be emulated.
   *
   * @return the emulated Minecraft version
   */
  public ProtocolVersion getProtocolVersion() {
    return this.protocolVersion;
  }

  /**
   * The maximum timeout duration for receiving the {@link ServerPing}.
   *
   * @return the server ping timeout in milliseconds
   */
  public long getTimeout() {
    return this.timeout;
  }

  /**
   * Create a new builder to assign values to a new PingOptions.
   *
   * @return a new {@link PingOptions.Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof PingOptions)) {
      return false;
    }
    final PingOptions other = (PingOptions) o;
    return Objects.equals(this.protocolVersion, other.protocolVersion)
            && Objects.equals(this.timeout, other.timeout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.protocolVersion, this.timeout);
  }

  @Override
  public String toString() {
    return "PingOptions{"
            + "protocolVersion=" + protocolVersion
            + ", timeout=" + timeout
            + '}';
  }

  /**
   * A builder for {@link PingOptions} objects.
   *
   * @since 3.2.0
   */
  public static final class Builder implements AbstractBuilder<PingOptions> {
    private ProtocolVersion protocolVersion;
    private Duration timeout;

    private Builder() {
    }

    /**
     * The protocol version used to ping the server
     * corresponding to this Builder.
     *
     * @return the protocol version corresponding to this Builder
     */
    public @Nullable ProtocolVersion getProtocolVersion() {
      return this.protocolVersion;
    }

    /**
     * The maximum timeout duration to wait for the ping
     * corresponding to this Builder.
     *
     * @return the timeout corresponding to this Builder
     */
    public @Nullable Duration getTimeout() {
      return this.timeout;
    }

    /**
     * Sets the protocol with which the server is to be pinged.
     *
     * @param protocolVersion the specified protocol
     * @return this builder
     */
    public Builder version(final ProtocolVersion protocolVersion) {
      this.protocolVersion = protocolVersion;
      return this;
    }

    /**
     * Sets the maximum time to wait to get the required {@link ServerPing}.
     *
     * @param timeout the timeout duration
     * @return this builder
     */
    public Builder timeout(final @NotNull Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * Sets the maximum time to wait to get the required {@link ServerPing}.
     *
     * @param time the timeout duration
     * @param unit the unit of time to be used to provide the timeout duration
     * @return this builder
     */
    public Builder timeout(final long time, final @NotNull TimeUnit unit) {
      this.timeout = Duration.of(time, unit.toChronoUnit());
      return this;
    }

    /**
     * Create a new {@link PingOptions} with the values of this Builder.
     *
     * @return a new PingOptions object
     * @throws NullPointerException if the timeout is not assigned
     *     until this method is executed
     */
    @Override
    public @NotNull PingOptions build() {
      if (timeout == null) {
        throw new NullPointerException("TimeOut cannot be null");
      }
      if (protocolVersion == null) {
        protocolVersion = ProtocolVersion.UNKNOWN;
      }
      return new PingOptions(this);
    }
  }
}
