/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import static com.google.common.base.Preconditions.checkNotNull;

import com.velocitypowered.api.network.ProtocolVersion;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.builder.AbstractBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
   */
  public static final PingOptions DEFAULT = PingOptions.builder().build();
  private final ProtocolVersion protocolVersion;
  private final long timeout;
  private final String virtualHost;

  private PingOptions(final Builder builder) {
    this.protocolVersion = builder.protocolVersion;
    this.timeout = builder.timeout;
    this.virtualHost = builder.virtualHost;
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
   * The virtual host to pass to the server for the ping.
   *
   * @return the virtual hostname to pass to the server for the ping
   * @since 3.4.0
   */
  public @Nullable String getVirtualHost() {
    return this.virtualHost;
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
    if (!(o instanceof final PingOptions other)) {
      return false;
    }
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
    private ProtocolVersion protocolVersion = ProtocolVersion.UNKNOWN;
    private long timeout = 0;
    private String virtualHost = null;

    private Builder() {
    }

    /**
     * Sets the protocol with which the server is to be pinged.
     *
     * @param protocolVersion the specified protocol
     * @return this builder
     */
    public Builder version(final @NotNull ProtocolVersion protocolVersion) {
      checkNotNull(protocolVersion, "protocolVersion cannot be null");
      this.protocolVersion = protocolVersion;
      return this;
    }

    /**
     * Sets the maximum time to wait to get the required {@link ServerPing}.
     *
     * @param timeout the timeout duration
     *                A value of 0 means that the read-timeout value
     *                from the Velocity configuration will be used,
     *                while a negative value means that there will
     *                be no timeout.
     * @return this builder
     */
    public Builder timeout(final @NotNull Duration timeout) {
      checkNotNull(timeout, "timeout cannot be null");
      this.timeout = timeout.toMillis();
      return this;
    }

    /**
     * Sets the maximum time to wait to get the required {@link ServerPing}.
     *
     * @param time the timeout duration
     *             A value of 0 means that the read-timeout value
     *             from the Velocity configuration will be used,
     *             while a negative value means that there will
     *             be no timeout.
     * @param timeunit the unit of time to be used to provide the timeout duration
     * @return this builder
     */
    public Builder timeout(final long time, final @NotNull TimeUnit timeunit) {
      checkNotNull(timeunit, "timeunit cannot be null");
      this.timeout = timeunit.toMillis(time);
      return this;
    }

    /**
     * Sets the virtual host to pass to the server for the ping.
     *
     * @param virtualHost the virtual hostname to pass to the server for the ping
     * @return this builder
     * @since 3.4.0
     */
    public Builder virtualHost(final @Nullable String virtualHost) {
      this.virtualHost = virtualHost;
      return this;
    }

    /**
     * Create a new {@link PingOptions} with the values of this Builder.
     *
     * @return a new PingOptions object
     */
    @Override
    public @NotNull PingOptions build() {
      return new PingOptions(this);
    }
  }
}
