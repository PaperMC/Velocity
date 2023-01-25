/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import static com.google.common.base.Preconditions.checkArgument;

import com.velocitypowered.api.network.ProtocolVersion;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.builder.AbstractBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Contains the parameters used to ping a {@link RegisteredServer}.
 * This class is immutable.
 *
 * @since 3.2.0
 * @see RegisteredServer#ping(PingOptions)
 */
public final class PingOptions {
  /**
   * Default PingOptions.
   * Uses a {@link ProtocolVersion#UNKNOWN} version and a timeout of 3000.
   */
  public static final PingOptions DEFAULT = PingOptions.builder().build();
  private final ProtocolVersion protocolVersion;
  private final long timeout;

  private PingOptions(final Builder builder) {
    this.protocolVersion = builder.protocolVersion;
    this.timeout = builder.timeout;
  }

  /**
   * The protocol version used to ping the server.
   *
   * @return the emulated Minecraft version
   */
  public ProtocolVersion getProtocolVersion() {
    return this.protocolVersion;
  }

  /**
   * The maximum period of time to wait for a response from the remote server.
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
    private long timeout = 3000;

    private Builder() {
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
      final long time = timeout.toMillis();
      checkArgument(time >= 0, "timeout cannot be negative");
      this.timeout = time;
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
      checkArgument(time >= 0, "timeout cannot be negative");
      this.timeout = unit.toMillis(time);
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
      if (protocolVersion == null) {
        protocolVersion = ProtocolVersion.UNKNOWN;
      }
      return new PingOptions(this);
    }
  }
}
