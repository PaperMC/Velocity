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

package com.velocitypowered.proxy.util.buildinfo;

import static net.kyori.adventure.text.Component.text;

import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import com.velocitypowered.api.util.buildinfo.ServerBuildInfo;
import com.velocitypowered.api.util.buildinfo.VelocityServerBuildInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.util.JarManifests;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.jar.Manifest;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This is internal it does not need a javadoc CHECKSTYLE.
 */
@AutoService(ServerBuildInfo.class)
public record ServerBuildInfoImpl(
        Key brandId,
        String brandName,
        String velocityVersionId,
        String velocityVersionName,
        OptionalInt buildNumber,
        Instant buildTime,
        Optional<String> gitBranch,
        Optional<String> gitCommit
) implements VelocityServerBuildInfo {
  private static final String ATTRIBUTE_BRAND_ID = "Brand-Id";
  private static final String ATTRIBUTE_BRAND_NAME = "Brand-Name";
  private static final String ATTRIBUTE_BUILD_TIME = "Build-Time";
  private static final String ATTRIBUTE_BUILD_NUMBER = "Build-Number";
  private static final String ATTRIBUTE_GIT_BRANCH = "Git-Branch";
  private static final String ATTRIBUTE_GIT_COMMIT = "Git-Commit";
  private static final String ATTRIBUTE_VERSION = "Implementation-Version";

  private static final String BRAND_VELOCITY_NAME = "Velocity";

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
                    .orElse(BRAND_VELOCITY_NAME),
            getManifestAttribute(manifest, ATTRIBUTE_VERSION)
                    .orElse("VersionId"),
            getManifestAttribute(manifest, ATTRIBUTE_VERSION)
                    .orElse("VersionName"),
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
    sb.append('-');
    final OptionalInt buildNumber = this.buildNumber;
    if (buildNumber.isPresent()) {
      sb.append(buildNumber.getAsInt());
    } else {
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

  @Override
  public @NotNull Component asComponent(final @NotNull StringRepresentation representation) {
    final TextComponent.Builder sb = text();
    sb.append(text(this.velocityVersionName));
    sb.append(text('-'));
    final OptionalInt buildNumber = this.buildNumber;
    if (buildNumber.isPresent()) {
      sb.append(text(buildNumber.getAsInt()));
    } else {
      sb.append(text(BUILD_DEV));
    }
    final boolean hasGitBranch = this.gitBranch.isPresent();
    final boolean hasGitCommit = this.gitCommit.isPresent();
    if (hasGitBranch || hasGitCommit) {
      sb.append(text('-'));
    }
    if (hasGitBranch && representation == StringRepresentation.VERSION_FULL) {
      // In theory, you could add a link to the branch, but that wouldn't work for local branches but would that really matter though?
      // Could also just not do that if the buildNumber is not present (or if DEV) is in the string
      if (buildNumber.isPresent()) {
        sb.append(text()
                .content(this.gitBranch.get())
                .clickEvent(ClickEvent.openUrl(
                        "https://github.com/" + this.brandId.namespace() + "/" + this.brandId.value() + "/tree/" + this.gitBranch.get()
                ))
        );
      } else {
        sb.append(text(this.gitBranch.get()));
      }
      if (hasGitCommit) {
        sb.append(text('@'));
      }
    }
    if (hasGitCommit) {
      sb.append(text()
              .content(this.gitCommit.get())
              .clickEvent(ClickEvent.openUrl(
                      "https://github.com/" + this.brandId.namespace() + "/" + this.brandId.value() + "/commit/" + this.gitCommit.get()
              ))
      );
    }
    if (representation == StringRepresentation.VERSION_FULL) {
      sb.append(text(' '));
      sb.append(text('('));
      sb.append(text(this.buildTime.truncatedTo(ChronoUnit.SECONDS).toString()));
      sb.append(text(')'));
    }
    return sb.build();
  }

  private static Optional<String> getManifestAttribute(final Manifest manifest, final String name) {
    final String value = manifest != null ? manifest.getMainAttributes().getValue(name) : null;
    return Optional.ofNullable(Strings.emptyToNull(value));
  }
}
