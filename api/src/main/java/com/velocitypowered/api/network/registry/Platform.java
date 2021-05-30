package com.velocitypowered.api.network.registry;

public interface Platform {
  Platform JAVA = new Platform() {
    @Override
    public String toString() {
      return "Java Edition";
    }
  };
}
