/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides a version object for the proxy.
 */
public final class ProxyVersion {

  private final String name;
  private final String vendor;
  private final String version;

  /**
   * Creates a new {@link ProxyVersion} instance.
   *
   * @param name the name for the proxy implementation
   * @param vendor the vendor for the proxy implementation
   * @param version the version for the proxy implementation
   */
  public ProxyVersion(String name, String vendor, String version) {
    this.name = Preconditions.checkNotNull(name, "name");
    this.vendor = Preconditions.checkNotNull(vendor, "vendor");
    this.version = Preconditions.checkNotNull(version, "version");
  }

  public String getName() {
    return name;
  }

  public String getVendor() {
    return vendor;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProxyVersion that = (ProxyVersion) o;
    return Objects.equals(name, that.name)
        && Objects.equals(vendor, that.vendor)
        && Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, vendor, version);
  }

  @Override
  public String toString() {
    return "ProxyVersion{"
        + "name='" + name + '\''
        + ", vendor='" + vendor + '\''
        + ", version='" + version + '\''
        + '}';
  }
}
