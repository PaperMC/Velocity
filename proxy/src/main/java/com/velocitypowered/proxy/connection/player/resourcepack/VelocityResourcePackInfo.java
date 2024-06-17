/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

package com.velocitypowered.proxy.connection.player.resourcepack;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import io.netty.buffer.ByteBufUtil;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Implements {@link ResourcePackInfo}.
 */
public final class VelocityResourcePackInfo implements ResourcePackInfo {

  private final UUID id;
  private final String url;
  private final byte @Nullable [] hash;
  private final boolean shouldForce;
  private final @Nullable Component prompt; // 1.17+ only
  private final Origin origin;
  private Origin originalOrigin;

  private VelocityResourcePackInfo(UUID id, String url, byte @Nullable [] hash, boolean shouldForce,
                                   @Nullable Component prompt, Origin origin) {
    this.id = id;
    this.url = url;
    this.hash = hash;
    this.shouldForce = shouldForce;
    this.prompt = prompt;
    this.origin = origin;
    this.originalOrigin = origin;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public @Nullable Component getPrompt() {
    return prompt;
  }

  @Override
  public boolean getShouldForce() {
    return shouldForce;
  }

  @Override
  public byte @Nullable[] getHash() {
    return hash == null ? null : hash.clone(); // Thanks spotbugs, very helpful.
  }

  @Override
  public Origin getOrigin() {
    return origin;
  }

  public void setOriginalOrigin(Origin originalOrigin) {
    this.originalOrigin = originalOrigin;
  }

  @Override
  public Origin getOriginalOrigin() {
    return originalOrigin;
  }

  @Override
  public Builder asBuilder() {
    return new BuilderImpl(url)
        .setId(id)
        .setShouldForce(shouldForce)
        .setHash(hash)
        .setPrompt(prompt);
  }

  @Override
  public Builder asBuilder(String newUrl) {
    return new BuilderImpl(newUrl)
        .setId(id)
        .setShouldForce(shouldForce)
        .setHash(hash)
        .setPrompt(prompt);
  }

  @Override
  public @NotNull ResourcePackRequest asResourcePackRequest() {
    return ResourcePackRequest.resourcePackRequest()
            .packs(net.kyori.adventure.resource.ResourcePackInfo.resourcePackInfo()
                    .id(this.id)
                    .uri(URI.create(this.url))
                    .hash(this.hash == null ? "" : ByteBufUtil.hexDump(this.hash))
                    .build())
            .required(this.shouldForce)
            .prompt(this.prompt)
            .build();
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static ResourcePackInfo fromAdventureRequest(
          final ResourcePackRequest request,
          final net.kyori.adventure.resource.ResourcePackInfo pack
  ) {
    return new BuilderImpl(pack.uri().toString())
            .setHash(pack.hash().isEmpty() ? null : ByteBufUtil.decodeHexDump(pack.hash()))
            .setId(pack.id())
            .setShouldForce(request.required())
            .setPrompt(request.prompt())
            .build();
  }

  /**
   * Implements the builder for {@link ResourcePackInfo} instances.
   */
  public static final class BuilderImpl implements ResourcePackInfo.Builder {

    private UUID id;
    private final String url;
    private boolean shouldForce;
    private byte @Nullable [] hash;
    private @Nullable Component prompt;
    private Origin origin = Origin.PLUGIN_ON_PROXY;

    public BuilderImpl(String url) {
      this.url = Preconditions.checkNotNull(url, "url");
      this.id = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public BuilderImpl setId(UUID id) {
      this.id = id;
      return this;
    }

    @Override
    public BuilderImpl setShouldForce(boolean shouldForce) {
      this.shouldForce = shouldForce;
      return this;
    }

    @Override
    public BuilderImpl setHash(final byte @Nullable [] hash) {
      if (hash != null) {
        Preconditions.checkArgument(hash.length == 20, "Hash length is not 20");
        this.hash = hash.clone(); // Thanks spotbugs, very helpful.
      } else {
        this.hash = null;
      }
      return this;
    }

    @Override
    public BuilderImpl setPrompt(@Nullable Component prompt) {
      this.prompt = prompt;
      return this;
    }

    @Override
    public ResourcePackInfo build() {
      return new VelocityResourcePackInfo(id, url, hash, shouldForce, prompt, origin);
    }

    public BuilderImpl setOrigin(Origin origin) {
      this.origin = origin;
      return this;
    }
  }

}