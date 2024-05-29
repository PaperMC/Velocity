/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.Services;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Information about the current server build.
 */
@SuppressWarnings({"checkstyle", "CheckStyle"}) // Temporarily
@ApiStatus.NonExtendable
public interface ServerBuildInfo {
  /**
  * The brand id for Velocity.
  */
  Key BRAND_VELOCITY_ID = Key.key("papermc", "velocity");

  /**
  * Gets the {@code ServerBuildInfo}.
  *
  * @return the {@code ServerBuildInfo}
  */
  static @NotNull ServerBuildInfo buildInfo() {
    //<editor-fold defaultstate="collapsed" desc="Holder">
    /**
     * This is a holder, it holds the serverbuildinfo :).
     */
    final class Holder {
      static final Optional<ServerBuildInfo> INSTANCE = Services.service(ServerBuildInfo.class);
    }
    //</editor-fold>

    return Holder.INSTANCE.orElseThrow();
  }

  /**
  * Gets the brand id of the server.
  *
  * @return the brand id of the server (e.g. "papermc:velocity")
  */
  @NotNull Key brandId();

  /**
  * Checks if the current server supports the specified brand.
  *
  * @param brandId the brand to check (e.g. "papermc:folia")
  * @return {@code true} if the server supports the specified brand
  */
  @ApiStatus.Experimental
  boolean isBrandCompatible(final @NotNull Key brandId);

  /**
  * Gets the brand name of the server.
  *
  * @return the brand name of the server (e.g. "Velocity")
  */
  @NotNull String brandName();


//  /**
//  * Gets the Velocity version id.
//  *
//  * @return the Velocity version id (e.g. "3.3.0-SNAPSHOT", "3.3.0", "3.0.0")
//  */
//  @NotNull String velocityVersionId();

  /**
  * Gets the Velocity version name.
  *
  * @return the Velocity version name (e.g. "3.3.0 Snapshot", "3.3.0", "3.0.0")
  */
  @NotNull String velocityVersionName();

  /**
  * Gets the build number.
  *
  * @return the build number
  */
  @NotNull OptionalInt buildNumber();

  /**
  * Gets the build time.
  *
  * @return the build time
  */
  @NotNull Instant buildTime();

  /**
  * Gets the git commit branch.
  *
  * @return the git commit branch
  */
  @NotNull Optional<String> gitBranch();

  /**
  * Gets the git commit hash.
  *
  * @return the git commit hash
  */
  @NotNull Optional<String> gitCommit();

  /**
  * Creates a string representation of the server build information.
  *
  * @param representation the type of representation
  * @return a string
  */
  @NotNull String asString(final @NotNull StringRepresentation representation);

  /**
  * String representation types.
  */
  enum StringRepresentation {
      /**
      * A simple version string, in format {@code <velocityVersionName>-<buildNumber>-<gitCommit>}.
      */
      VERSION_SIMPLE,
      /**
      * A simple version string, in format {@code <velocityVersionName>-<buildNumber>-<gitBranch>@<gitCommit> (<buildTime>)}.
      */
      VERSION_FULL,
  }
}
