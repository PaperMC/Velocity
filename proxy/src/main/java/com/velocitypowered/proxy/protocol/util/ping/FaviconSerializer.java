package com.velocitypowered.proxy.protocol.util.ping;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.velocitypowered.api.util.Favicon;
import java.io.IOException;

public final class FaviconSerializer extends TypeAdapter<Favicon> {

  public static final FaviconSerializer INSTANCE = new FaviconSerializer();

  private FaviconSerializer() {

  }

  @Override
  public void write(JsonWriter writer, Favicon favicon) throws IOException {
    writer.value(favicon.getBase64Url());
  }

  @Override
  public Favicon read(JsonReader reader) throws IOException {
    return new Favicon(reader.nextString());
  }
}
