/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Mojang game profile. This class is immutable.
 */
public final class GameProfile {

  private final UUID id;
  private final String undashedId;
  private final String name;
  private final List<Property> properties;

  /**
   * Creates a new Mojang game profile.
   *
   * @param id the UUID for the profile
   * @param name the profile's username
   * @param properties properties for the profile
   */
  public GameProfile(UUID id, String name, List<Property> properties) {
    this(Preconditions.checkNotNull(id, "id"), UuidUtils.toUndashed(id),
        Preconditions.checkNotNull(name, "name"), ImmutableList.copyOf(properties));
  }

  /**
   * Creates a new Mojang game profile.
   *
   * @param undashedId the undashed, Mojang-style UUID for the profile
   * @param name the profile's username
   * @param properties properties for the profile
   */
  public GameProfile(String undashedId, String name, List<Property> properties) {
    this(UuidUtils.fromUndashed(Preconditions.checkNotNull(undashedId, "undashedId")), undashedId,
        Preconditions.checkNotNull(name, "name"), ImmutableList.copyOf(properties));
  }

  private GameProfile(UUID id, String undashedId, String name, List<Property> properties) {
    this.id = id;
    this.undashedId = undashedId;
    this.name = name;
    this.properties = properties;
  }

  /**
   * Returns the undashed, Mojang-style UUID.
   *
   * @return the undashed UUID
   */
  public String getUndashedId() {
    return undashedId;
  }

  /**
   * Returns the UUID associated with this game profile.
   *
   * @return the UUID
   */
  public UUID getId() {
    return id;
  }

  /**
   * Returns the username associated with this profile.
   *
   * @return the username
   */
  public String getName() {
    return name;
  }

  /**
   * Returns an immutable list of profile properties associated with this profile.
   *
   * @return the properties associated with this profile
   */
  public List<Property> getProperties() {
    return properties;
  }

  /**
   * Creates a new {@code GameProfile} with the specified unique id.
   *
   * @param id the new unique id
   * @return the new {@code GameProfile}
   */
  public GameProfile withId(UUID id) {
    return new GameProfile(Preconditions.checkNotNull(id, "id"), UuidUtils.toUndashed(id),
        this.name, this.properties);
  }

  /**
   * Creates a new {@code GameProfile} with the specified undashed id.
   *
   * @param undashedId the new undashed id
   * @return the new {@code GameProfile}
   */
  public GameProfile withUndashedId(String undashedId) {
    return new GameProfile(
        UuidUtils.fromUndashed(Preconditions.checkNotNull(undashedId, "undashedId")), undashedId,
        this.name, this.properties);
  }

  /**
   * Creates a new {@code GameProfile} with the specified name.
   *
   * @param name the new name
   * @return the new {@code GameProfile}
   */
  public GameProfile withName(String name) {
    return new GameProfile(this.id, this.undashedId, Preconditions.checkNotNull(name, "name"),
        this.properties);
  }

  /**
   * Creates a new {@code GameProfile} with the specified properties.
   *
   * @param properties the new properties
   * @return the new {@code GameProfile}
   */
  public GameProfile withProperties(List<Property> properties) {
    return new GameProfile(this.id, this.undashedId, this.name, ImmutableList.copyOf(properties));
  }

  /**
   * Creates a new {@code GameProfile} with the properties of this object plus the specified
   * properties.
   *
   * @param properties the properties to add
   * @return the new {@code GameProfile}
   */
  public GameProfile addProperties(Iterable<Property> properties) {
    return new GameProfile(this.id, this.undashedId, this.name,
        ImmutableList.<Property>builder().addAll(this.properties).addAll(properties).build());
  }

  /**
   * Creates a new {@code GameProfile} with the properties of this object plus the specified
   * property.
   *
   * @param property the property to add
   * @return the new {@code GameProfile}
   */
  public GameProfile addProperty(Property property) {
    return new GameProfile(this.id, this.undashedId, this.name,
        ImmutableList.<Property>builder().addAll(this.properties).add(property).build());
  }

  /**
   * Creates a game profile suitable for use in offline-mode.
   *
   * @param username the username to use
   * @return the new offline-mode game profile
   */
  public static GameProfile forOfflinePlayer(String username) {
    Preconditions.checkNotNull(username, "username");
    return new GameProfile(UuidUtils.generateOfflinePlayerUuid(username), username,
        ImmutableList.of());
  }

  @Override
  public String toString() {
    return "GameProfile{"
        + "id='" + id + '\''
        + ", name='" + name + '\''
        + ", properties=" + properties
        + '}';
  }

  /**
   * Represents a Mojang profile property. Just like {@link GameProfile}, this class is immutable.
   */
  public static final class Property {

    private final String name;
    private final String value;
    private final String signature;

    /**
     * Creates a profile property entry.
     *
     * @param name the name of the property
     * @param value the value of the property
     * @param signature the Mojang signature for the property
     */
    public Property(String name, String value, String signature) {
      this.name = Preconditions.checkNotNull(name, "name");
      this.value = Preconditions.checkNotNull(value, "value");
      this.signature = Preconditions.checkNotNull(signature, "signature");
    }

    /**
     * Returns the name of this property.
     *
     * @return the property name
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the value of this property.
     *
     * @return the property value
     */
    public String getValue() {
      return value;
    }

    /**
     * Returns the Mojang-provided signature for this property.
     *
     * @return the property signature
     */
    public String getSignature() {
      return signature;
    }

    @Override
    public String toString() {
      return "Property{"
          + "name='" + name + '\''
          + ", value='" + value + '\''
          + ", signature='" + signature + '\''
          + '}';
    }
  }
}
