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
  private final String undashedId, name;
  private final List<Property> properties;

  public GameProfile(UUID id, String name, List<Property> properties) {
    this(Preconditions.checkNotNull(id, "id"), UuidUtils.toUndashed(id),
        Preconditions.checkNotNull(name, "name"), ImmutableList.copyOf(properties));
  }

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

  public String getId() {
    return undashedId;
  }

  public UUID idAsUuid() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<Property> getProperties() {
    return properties;
  }

  /**
   * Creates a new {@code GameProfile} with the specified unique id.
   *
   * @param id the new unique id
   * @return the new {@code GameProfile}
   */
  public GameProfile withUuid(UUID id) {
    return new GameProfile(Preconditions.checkNotNull(id, "id"), UuidUtils.toUndashed(id),
        this.name, this.properties);
  }

  /**
   * Creates a new {@code GameProfile} with the specified undashed id.
   *
   * @param undashedId the new undashed id
   * @return the new {@code GameProfile}
   */
  public GameProfile withId(String undashedId) {
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

  public static final class Property {

    private final String name;
    private final String value;
    private final String signature;

    public Property(String name, String value, String signature) {
      this.name = Preconditions.checkNotNull(name, "name");
      this.value = Preconditions.checkNotNull(value, "value");
      this.signature = Preconditions.checkNotNull(signature, "signature");
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

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
