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
    return Objects.equals(name, that.name) &&
        Objects.equals(vendor, that.vendor) &&
        Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, vendor, version);
  }

  @Override
  public String toString() {
    return "ProxyVersion{" +
        "name='" + name + '\'' +
        ", vendor='" + vendor + '\'' +
        ", version='" + version + '\'' +
        '}';
  }
}
