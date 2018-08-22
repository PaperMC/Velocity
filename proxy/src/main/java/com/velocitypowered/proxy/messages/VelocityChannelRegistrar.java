package com.velocitypowered.proxy.messages;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.messages.*;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityChannelRegistrar implements ChannelRegistrar {
    private static final Logger logger = LogManager.getLogger(VelocityChannelRegistrar.class);
    private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, ChannelIdentifier> identifierMap = new ConcurrentHashMap<>();

    @Override
    public void register(MessageHandler handler, ChannelIdentifier... identifiers) {
        for (ChannelIdentifier identifier : identifiers) {
            Preconditions.checkArgument(identifier instanceof LegacyChannelIdentifier || identifier instanceof MinecraftChannelIdentifier,
                    "identifier is unknown");
        }

        for (ChannelIdentifier identifier : identifiers) {
            handlers.put(identifier.getId(), handler);
            identifierMap.put(identifier.getId(), identifier);
        }
    }

    public MessageHandler.ForwardStatus handlePluginMessage(ChannelMessageSource source, ChannelSide side, PluginMessage message) {
        MessageHandler handler = handlers.get(message.getChannel());
        ChannelIdentifier identifier = identifierMap.get(message.getChannel());
        if (handler == null || identifier == null) {
            return MessageHandler.ForwardStatus.FORWARD;
        }

        try {
            return handler.handle(source, side, identifier, message.getData());
        } catch (Exception e) {
            logger.info("Unable to handle plugin message on channel {} for {}", message.getChannel(), source);
            // In case of doubt, do not forward the message on.
            return MessageHandler.ForwardStatus.HANDLED;
        }
    }

    @Override
    public void unregister(ChannelIdentifier... identifiers) {
        for (ChannelIdentifier identifier : identifiers) {
            Preconditions.checkArgument(identifier instanceof LegacyChannelIdentifier || identifier instanceof MinecraftChannelIdentifier,
                    "identifier is unknown");
        }

        for (ChannelIdentifier identifier : identifiers) {
            handlers.remove(identifier.getId());
            identifierMap.remove(identifier.getId());
        }
    }
}
