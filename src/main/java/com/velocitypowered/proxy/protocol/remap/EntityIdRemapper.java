package com.velocitypowered.proxy.protocol.remap;

import com.velocitypowered.proxy.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;

/**
 * Represents a protocol-specific entity ID remapper for certain Minecraft packets. This is mostly required to support
 * old versions of Minecraft. For Minecraft 1.9 clients and above, Velocity can use a more efficient method based on
 * sending JoinGame packets multiple times.
 */
public interface EntityIdRemapper {
    /**
     * Remaps the entity IDs in this packet so that they apply to the player.
     * @param original the packet to remap
     * @param direction the direction of the packet
     * @return a remapped packet, which may either be a retained version of the original buffer or an entirely new buffer
     */
    ByteBuf remap(ByteBuf original, ProtocolConstants.Direction direction);

    int getClientEntityId();

    int getServerEntityId();

    void setServerEntityId(int id);

    static EntityIdRemapper getMapper(int eid, int protocolVersion) {
        return NoopEntityIdRemapper.INSTANCE;
    }
}
