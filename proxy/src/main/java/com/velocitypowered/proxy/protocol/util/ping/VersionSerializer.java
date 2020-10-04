package com.velocitypowered.proxy.protocol.util.ping;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.velocitypowered.api.proxy.server.ServerPing.Version;
import java.io.IOException;

public class VersionSerializer extends TypeAdapter<Version> {

  public static final VersionSerializer INSTANCE = new VersionSerializer();

  private VersionSerializer() {

  }

  @Override
  public void write(JsonWriter jsonWriter, Version version) throws IOException {
    jsonWriter.beginObject();
    jsonWriter.name("protocol");
    jsonWriter.value(version.getProtocol());
    jsonWriter.name("name");
    jsonWriter.value(version.getName());
    jsonWriter.endObject();
  }

  @Override
  public Version read(JsonReader jsonReader) throws IOException {
    jsonReader.beginObject();

    String name = "";
    int protocol = -1;
    for (int i = 0; i < 2; i++) {
      String elem = jsonReader.nextName();
      if (elem.equals("name")) {
        name = jsonReader.nextString();
      } else if (elem.equals("protocol")) {
        protocol = jsonReader.nextInt();
      } else {
        throw new IllegalStateException("Invalid version specification.");
      }
    }

    jsonReader.endObject();
    return new Version(protocol, name);
  }
}
