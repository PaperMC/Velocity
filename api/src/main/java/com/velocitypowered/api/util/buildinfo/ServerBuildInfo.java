/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util.buildinfo;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.Services;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Information about the current server build.
 *
 * @apiNote to be separated later
 */
@SuppressWarnings({"checkstyle", "CheckStyle"}) // Temporarily
@ApiStatus.NonExtendable
public interface ServerBuildInfo {

  /**
  * Gets the {@code ServerBuildInfo}.
  *
  * @return the {@code ServerBuildInfo}
  */
  static <T extends ServerBuildInfo> @NotNull T buildInfo() {
    //<editor-fold defaultstate="collapsed" desc="Holder">
    /**
     * This is a holder, it holds the serverbuildinfo :).
     */
    final class Holder {
      static final Optional<ServerBuildInfo> INSTANCE = Services.service(ServerBuildInfo.class);
    }
    //</editor-fold>

    return (T) Holder.INSTANCE.orElseThrow();
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
      * A simple version string, in format {@code <versionName>-<buildNumber>-<gitCommit>}.
      */
      VERSION_SIMPLE,
      /**
      * A simple version string, in format {@code <versionName>-<buildNumber>-<gitBranch>@<gitCommit> (<buildTime>)}.
      */
      VERSION_FULL,
  }
}
