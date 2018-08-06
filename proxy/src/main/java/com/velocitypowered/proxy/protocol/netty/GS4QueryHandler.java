package com.velocitypowered.proxy.protocol.netty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GS4QueryHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private final static short QUERY_MAGIC_FIRST = 0xFE;
    private final static short QUERY_MAGIC_SECOND = 0xFD;
    private final static byte QUERY_TYPE_HANDSHAKE = 0x09;
    private final static byte QUERY_TYPE_STAT = 0x00;
    private final static byte[] QUERY_RESPONSE_FULL_PADDING = new byte[] { 0x73, 0x70, 0x6C, 0x69, 0x74, 0x6E, 0x75, 0x6D, 0x00, (byte) 0x80, 0x00 };
    private final static byte[] QUERY_RESPONSE_FULL_PADDING2 = new byte[] { 0x01, 0x70, 0x6C, 0x61, 0x79, 0x65, 0x72, 0x5F, 0x00, 0x00 };
    private final static List<String> QUERY_BASIC_RESPONSE_CONTENTS = Arrays.asList(
            "hostname",
            "gametype",
            "map",
            "numplayers",
            "maxplayers",
            "hostport",
            "hostip"
    );

    private final static Random random = new Random();
    private final static Cache<InetAddress, Integer> sessions = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        ByteBuf queryMessage = msg.content();

        // Verify query packet magic
        if (queryMessage.readUnsignedByte() != QUERY_MAGIC_FIRST && queryMessage.readUnsignedByte() != QUERY_MAGIC_SECOND) {
            throw new IllegalStateException("Invalid query packet magic");
        }

        // Read packet header
        short type = queryMessage.readUnsignedByte();
        int sessionId = queryMessage.readInt();

        // Allocate memory for response
        ByteBuf queryResponse = ctx.alloc().buffer();
        DatagramPacket responsePacket = new DatagramPacket(queryResponse, msg.sender());

        switch(type) {
            case QUERY_TYPE_HANDSHAKE: {
                queryResponse.writeByte(QUERY_TYPE_HANDSHAKE);
                queryResponse.writeInt(sessionId);
                int challengeToken = random.nextInt();
                sessions.put(msg.sender().getAddress(), challengeToken);
                queryResponse.writeByte(challengeToken);
                break;
            }

            case QUERY_TYPE_STAT: {
                int challengeToken = queryMessage.readInt();
                Integer session = sessions.getIfPresent(msg.sender().getAddress());
                if (session == null || session != challengeToken) {
                    queryResponse.release();
                    throw new IllegalStateException("Couldn't find a query session");
                }

                // Check which query response client expects
                if(queryMessage.readableBytes() != 0 && queryMessage.readableBytes() != 4) {
                    queryResponse.release();
                    throw new IllegalStateException("Invalid query packet");
                }

                // Packet header
                queryResponse.writeByte(QUERY_TYPE_STAT);
                queryResponse.writeInt(sessionId);

                // Start writing the response
                ResponseWriter responseWriter = new ResponseWriter(queryResponse, queryMessage.readableBytes() == 0);
                responseWriter.write("hostname", VelocityServer.getServer().getConfiguration().getMotd());
                responseWriter.write("gametype", "SMP");

                responseWriter.write("game_id", "MINECRAFT");
                responseWriter.write("version", ""); // TODO
                responseWriter.write("plugins", ""); // TODO

                responseWriter.write("map", "Velocity"); // TODO
                responseWriter.write("numplayers", Integer.toString(VelocityServer.getServer().getAllPlayers().size()));
                responseWriter.write("maxplayers", Integer.toString(VelocityServer.getServer().getConfiguration().getShowMaxPlayers()));
                responseWriter.write("hostport", Integer.toString(VelocityServer.getServer().getConfiguration().getBind().getPort()));
                responseWriter.write("hostip", VelocityServer.getServer().getConfiguration().getBind().getHostString());

                responseWriter.writePlayers(VelocityServer.getServer().getAllPlayers());
                break;
            }

            default: {
                queryResponse.release();
                throw new IllegalStateException("Invalid query type: " + type);
            }
        }

        // Send the response
        ctx.writeAndFlush(responsePacket);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    private static void writeString(ByteBuf buf, String string) {
        buf.writeBytes((string + '\0').getBytes(StandardCharsets.UTF_8));
    }

    private static class ResponseWriter {
        private final ByteBuf buf;
        private final boolean isBasic;

        ResponseWriter(ByteBuf buf, boolean isBasic) {
            this.buf = buf;
            this.isBasic = isBasic;

            if(!isBasic) {
                buf.writeBytes(QUERY_RESPONSE_FULL_PADDING);
            }
        }

        void write(String key, String value) {
            if(!isBasic) {
                writeString(buf, key);
                writeString(buf, value);
            } else {
                if(!QUERY_BASIC_RESPONSE_CONTENTS.contains(key)) {
                    return;
                }

                // Special case host port
                if(key.equals("hostport")) {
                    buf.writeShortLE(Integer.parseInt(value));
                } else {
                    writeString(buf, value);
                }
            }
        }

        void writePlayers(Collection<Player> players) {
            if(isBasic) {
                return;
            }
            buf.writeByte(0x00);
            buf.writeBytes(QUERY_RESPONSE_FULL_PADDING2);

            players.forEach(player -> writeString(buf, player.getUsername()));
            buf.writeByte(0x00);
        }
    }
}
