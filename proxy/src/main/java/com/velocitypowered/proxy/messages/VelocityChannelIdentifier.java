package com.velocitypowered.proxy.messages;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A dummy {@link ChannelIdentifier} for communication w/ Velocity
 */
public final class VelocityChannelIdentifier implements ChannelIdentifier {

    private VelocityChannelIdentifier() { }

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final ChannelIdentifier INSTANCE = new VelocityChannelIdentifier();

    @Override
    public String getId() {
        return VelocityChannelConstants.CHANNEL_NAME;
    }

    /**
     * Creates a reply {@link PluginMessage} to the client
     */
    public static PluginMessage createMessage(byte[] dataArray) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
