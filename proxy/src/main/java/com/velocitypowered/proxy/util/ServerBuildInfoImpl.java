/*
 * Copyright (C) 2024 Velocity Contributors
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

package com.velocitypowered.proxy.util;

import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import com.velocitypowered.api.util.ServerBuildInfo;
import com.velocitypowered.proxy.VelocityServer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.jar.Manifest;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * This is internal it does not need a javadoc CHECKSTYLE.
 */
@AutoService(ServerBuildInfo.class)
public record ServerBuildInfoImpl(
        Key brandId,
        String brandName,
        /*String velocityVersionId,*/
        String velocityVersionName,
        OptionalInt buildNumber,
        Instant buildTime,
        Optional<String> gitBranch,
        Optional<String> gitCommit
) implements ServerBuildInfo {
  private static final String ATTRIBUTE_BRAND_ID = "Brand-Id";
  private static final String ATTRIBUTE_BRAND_NAME = "Brand-Name";
  private static final String ATTRIBUTE_BUILD_TIME = "Build-Time";
  private static final String ATTRIBUTE_BUILD_NUMBER = "Build-Number";
  private static final String ATTRIBUTE_GIT_BRANCH = "Git-Branch";
  private static final String ATTRIBUTE_GIT_COMMIT = "Git-Commit";
  private static final String ATTRIBUTE_VERSION = "Implementation-Version";

  private static final String BRAND_PAPER_NAME = "Velocity";

  private static final String BUILD_DEV = "DEV";

  public ServerBuildInfoImpl() {
    this(JarManifests.manifest(VelocityServer.class));
  }

  private ServerBuildInfoImpl(final Manifest manifest) {
    this(
            getManifestAttribute(manifest, ATTRIBUTE_BRAND_ID)
                    .map(Key::key)
                    .orElse(BRAND_VELOCITY_ID),
            getManifestAttribute(manifest, ATTRIBUTE_BRAND_NAME)
                    .orElse(BRAND_PAPER_NAME),
            getManifestAttribute(manifest, ATTRIBUTE_VERSION)
                    .orElse("Unknown"),
            getManifestAttribute(manifest, ATTRIBUTE_BUILD_NUMBER)
                    .map(Integer::parseInt)
                    .map(OptionalInt::of)
                    .orElse(OptionalInt.empty()),
            getManifestAttribute(manifest, ATTRIBUTE_BUILD_TIME)
                    .map(Instant::parse)
                    .orElse(Instant.now()),
            getManifestAttribute(manifest, ATTRIBUTE_GIT_BRANCH),
            getManifestAttribute(manifest, ATTRIBUTE_GIT_COMMIT)
    );
  }

  @Override
  public boolean isBrandCompatible(final @NotNull Key brandId) {
    return brandId.equals(this.brandId);
  }

  @Override
  public @NotNull String asString(final @NotNull StringRepresentation representation) {
    final StringBuilder sb = new StringBuilder();
    sb.append(this.velocityVersionName);
    if (this.buildNumber.isPresent()) {
      sb.append('-'); // eh
      sb.append(this.buildNumber.getAsInt());
    } else if (!this.velocityVersionName.contains("-SNAPSHOT")) {
      sb.append('-'); // eh
      sb.append(BUILD_DEV);
    }
    final boolean hasGitBranch = this.gitBranch.isPresent();
    final boolean hasGitCommit = this.gitCommit.isPresent();
    if (hasGitBranch || hasGitCommit) {
      sb.append('-');
    }
    if (hasGitBranch && representation == StringRepresentation.VERSION_FULL) {
      sb.append(this.gitBranch.get());
      if (hasGitCommit) {
        sb.append('@');
      }
    }
    if (hasGitCommit) {
      sb.append(this.gitCommit.get());
    }
    if (representation == StringRepresentation.VERSION_FULL) {
      sb.append(' ');
      sb.append('(');
      sb.append(this.buildTime.truncatedTo(ChronoUnit.SECONDS));
      sb.append(')');
    }
    return sb.toString();
  }

  private static Optional<String> getManifestAttribute(final Manifest manifest, final String name) {
    final String value = manifest != null ? manifest.getMainAttributes().getValue(name) : null;
    return Optional.ofNullable(Strings.emptyToNull(value));
  }
}
