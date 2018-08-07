package com.velocitypowered.proxy.protocol.netty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class GS4QueryHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final Logger logger = LogManager.getLogger(GS4QueryHandler.class);

    private final static short QUERY_MAGIC_FIRST = 0xFE;
    private final static short QUERY_MAGIC_SECOND = 0xFD;
    private final static byte QUERY_TYPE_HANDSHAKE = 0x09;
    private final static byte QUERY_TYPE_STAT = 0x00;
    private final static byte[] QUERY_RESPONSE_FULL_PADDING = new byte[] { 0x73, 0x70, 0x6C, 0x69, 0x74, 0x6E, 0x75, 0x6D, 0x00, (byte) 0x80, 0x00 };
    private final static byte[] QUERY_RESPONSE_FULL_PADDING2 = new byte[] { 0x01, 0x70, 0x6C, 0x61, 0x79, 0x65, 0x72, 0x5F, 0x00, 0x00 };

    // Contents to add into basic stat response. See ResponseWriter class below
    private final static List<String> QUERY_BASIC_RESPONSE_CONTENTS = Arrays.asList(
            "hostname",
            "gametype",
            "map",
            "numplayers",
            "maxplayers",
            "hostport",
            "hostip"
    );

    private final static Cache<InetAddress, Integer> sessions = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        ByteBuf queryMessage = msg.content();
        InetAddress senderAddress = msg.sender().getAddress();

        // Verify query packet magic
        if (queryMessage.readUnsignedByte() != QUERY_MAGIC_FIRST && queryMessage.readUnsignedByte() != QUERY_MAGIC_SECOND) {
            throw new IllegalStateException("Invalid query packet magic");
        }

        // Read packet header
        short type = queryMessage.readUnsignedByte();
        int sessionId = queryMessage.readInt();

        // Allocate buffer for response
        ByteBuf queryResponse = ctx.alloc().buffer();
        DatagramPacket responsePacket = new DatagramPacket(queryResponse, msg.sender());

        switch(type) {
            case QUERY_TYPE_HANDSHAKE: {
                // Generate new challenge token and put it into the sessions cache
                int challengeToken = ThreadLocalRandom.current().nextInt();
                sessions.put(senderAddress, challengeToken);

                // Respond with challenge token
                queryResponse.writeByte(QUERY_TYPE_HANDSHAKE);
                queryResponse.writeInt(sessionId);
                writeString(queryResponse, Integer.toString(challengeToken));
                break;
            }

            case QUERY_TYPE_STAT: {
                // Check if query was done with session previously generated using a handshake packet
                int challengeToken = queryMessage.readInt();
                Integer session = sessions.getIfPresent(senderAddress);
                if (session == null || session != challengeToken) {
                    throw new IllegalStateException("Invalid challenge token");
                }

                // Check which query response client expects
                if (queryMessage.readableBytes() != 0 && queryMessage.readableBytes() != 4) {
                    throw new IllegalStateException("Invalid query packet");
                }

                // Packet header
                queryResponse.writeByte(QUERY_TYPE_STAT);
                queryResponse.writeInt(sessionId);

                // Fetch information
                VelocityServer server = VelocityServer.getServer();
                Collection<Player> players = server.getAllPlayers();

                // Start writing the response
                ResponseWriter responseWriter = new ResponseWriter(queryResponse, queryMessage.readableBytes() == 0);
                responseWriter.write("hostname", server.getConfiguration().getMotd());
                responseWriter.write("gametype", "SMP");

                responseWriter.write("game_id", "MINECRAFT");
                responseWriter.write("version", ProtocolConstants.SUPPORTED_VERSIONS_STRING);
                responseWriter.write("plugins", "");

                responseWriter.write("map", "Velocity"); // TODO
                responseWriter.write("numplayers", players.size());
                responseWriter.write("maxplayers", server.getConfiguration().getShowMaxPlayers());
                responseWriter.write("hostport", server.getConfiguration().getBind().getPort());
                responseWriter.write("hostip", server.getConfiguration().getBind().getHostString());

                responseWriter.writePlayers(players);
                break;
            }

            default: {
                throw new IllegalStateException("Invalid query type: " + type);
            }
        }

        // Send the response
        ctx.writeAndFlush(responsePacket);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Error while trying to handle a query packet from " + ctx.channel().remoteAddress(), cause);
    }

    private static void writeString(ByteBuf buf, String string) {
        buf.writeBytes(string.getBytes(StandardCharsets.UTF_8));
        buf.writeByte(0x00);
    }

    private static class ResponseWriter {
        private final ByteBuf buf;
        private final boolean isBasic;

        ResponseWriter(ByteBuf buf, boolean isBasic) {
            this.buf = buf;
            this.isBasic = isBasic;

            if (!isBasic) {
                buf.writeBytes(QUERY_RESPONSE_FULL_PADDING);
            }
        }

        // Writes k/v to stat packet body if this writer is initialized
        // for full stat response. Otherwise this follows
        // GS4QueryHandler#QUERY_BASIC_RESPONSE_CONTENTS to decide what
        // to write into packet body
        void write(String key, Object value) {
            if (isBasic) {
                // Basic contains only specific set of data
                if (!QUERY_BASIC_RESPONSE_CONTENTS.contains(key)) {
                    return;
                }

                // Special case hostport
                if (key.equals("hostport")) {
                    buf.writeShortLE((Integer) value);
                } else {
                    writeString(buf, value instanceof Integer ? Integer.toString((Integer) value) : (String) value);
                }
            } else {
                writeString(buf, key);
                writeString(buf, value instanceof Integer ? Integer.toString((Integer) value) : (String) value);
            }
        }

        // Ends packet k/v body writing and writes stat player list to
        // the packet if this writer is initialized for full stat response
        void writePlayers(Collection<Player> players) {
            if (isBasic) {
                return;
            }

            // Ends the full stat key-value body with \0
            buf.writeByte(0x00);

            buf.writeBytes(QUERY_RESPONSE_FULL_PADDING2);
            players.forEach(player -> writeString(buf, player.getUsername()));
            buf.writeByte(0x00);
        }
    }
}
