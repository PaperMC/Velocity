package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.UUID;

public class GameProfile {
    private final String id;
    private final String name;
    private final List<Property> properties;

    public GameProfile(String id, String name, List<Property> properties) {
        this.id = id;
        this.name = name;
        this.properties = ImmutableList.copyOf(properties);
    }

    public String getId() {
        return id;
    }

    public UUID idAsUuid() {
        return UuidUtils.fromUndashed(id);
    }

    public String getName() {
        return name;
    }

    public List<Property> getProperties() {
        return ImmutableList.copyOf(properties);
    }

    public static GameProfile forOfflinePlayer(String username) {
        Preconditions.checkNotNull(username, "username");
        String id = UuidUtils.toUndashed(UuidUtils.generateOfflinePlayerUuid(username));
        return new GameProfile(id, username, ImmutableList.of());
    }

    @Override
    public String toString() {
        return "GameProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", properties=" + properties +
                '}';
    }

    public class Property {
        private final String name;
        private final String value;
        private final String signature;

        public Property(String name, String value, String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
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
            return "Property{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    ", signature='" + signature + '\'' +
                    '}';
        }
    }
}
