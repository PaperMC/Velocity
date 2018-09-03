package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import com.velocitypowered.proxy.util.ThrowableUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Handles communication with the connected Minecraft client. This is effectively the primary nerve center that
 * joins backend servers with players.
 */
public class ClientPlaySessionHandler implements MinecraftSessionHandler {
    private static final Logger logger = LogManager.getLogger(ClientPlaySessionHandler.class);
    private static final int MAX_PLUGIN_CHANNELS = 128;

    private final ConnectedPlayer player;
    private long lastPingID = -1;
    private long lastPingSent = -1;
    private boolean spawned = false;
    private final List<UUID> serverBossBars = new ArrayList<>();
    private final Set<String> clientPluginMsgChannels = new HashSet<>();
    private final VelocityServer server;

    public ClientPlaySessionHandler(VelocityServer server, ConnectedPlayer player) {
        this.player = player;
        this.server = server;
    }

    @Override
    public void activated() {
        PluginMessage message;
        if (player.getProtocolVersion() >= ProtocolConstants.MINECRAFT_1_13) {
            message = PluginMessageUtil.constructChannelsPacket("minecraft:register", server.getChannelRegistrar().getModernChannelIds());
        } else {
            message = PluginMessageUtil.constructChannelsPacket("REGISTER", server.getChannelRegistrar().getIdsForLegacyConnections());
        }
        player.getConnection().write(message);
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof KeepAlive) {
            KeepAlive keepAlive = (KeepAlive) packet;
            if (keepAlive.getRandomId() != lastPingID) {
                // The last keep alive we got was probably from a different server. Let's ignore it, and hope the next
                // ping is alright.
                return;
            }
            player.setPing(System.currentTimeMillis() - lastPingSent);
            resetPingData();
        }

        if (packet instanceof ClientSettings) {
            player.setPlayerSettings((ClientSettings) packet);
            // forward it on
        }

        if (packet instanceof Chat) {
            // Try to handle any commands on the proxy. If that fails, send it onto the client.
            Chat chat = (Chat) packet;
            String msg = ((Chat) packet).getMessage();
            if (msg.startsWith("/")) {
                try {
                    if (!server.getCommandManager().execute(player, msg.substring(1))) {
                        player.getConnectedServer().getMinecraftConnection().write(chat);
                    }
                } catch (Exception e) {
                    logger.info("Exception occurred while running command for {}", player.getProfile().getName(), e);
                    player.sendMessage(TextComponent.of("An error occurred while running this command.", TextColor.RED));
                    return;
                }
            } else {
                player.getConnectedServer().getMinecraftConnection().write(chat);
            }
            return;
        }

        if (packet instanceof TabCompleteRequest) {
            TabCompleteRequest req = (TabCompleteRequest) packet;
            int lastSpace = req.getCommand().indexOf(' ');
            if (!req.isAssumeCommand() && lastSpace != -1) {
                String command = req.getCommand().substring(1);
                try {
                    Optional<List<String>> offers = server.getCommandManager().offerSuggestions(player, command);
                    if (offers.isPresent()) {
                        TabCompleteResponse response = new TabCompleteResponse();
                        response.setTransactionId(req.getTransactionId());
                        response.setStart(lastSpace);
                        response.setLength(req.getCommand().length() - lastSpace);

                        for (String s : offers.get()) {
                            response.getOffers().add(new TabCompleteResponse.Offer(s, null));
                        }

                        player.getConnection().write(response);
                    } else {
                        player.getConnectedServer().getMinecraftConnection().write(packet);
                    }
                } catch (Exception e) {
                    logger.error("Unable to provide tab list completions for " + player.getUsername() + " for command '" + req.getCommand() + "'", e);
                }
                return;
            }
        }

        if (packet instanceof PluginMessage) {
            handleClientPluginMessage((PluginMessage) packet);
            return;
        }

        // If we don't want to handle this packet, just forward it on.
        player.getConnectedServer().getMinecraftConnection().write(packet);
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        player.getConnectedServer().getMinecraftConnection().write(buf.retain());
    }

    @Override
    public void disconnected() {
        player.teardown();
        server.getEventManager().fireAndForget(new DisconnectEvent(player));
    }

    @Override
    public void exception(Throwable throwable) {
        player.close(TextComponent.builder()
                .content("An exception occurred in your connection: ")
                .color(TextColor.RED)
                .append(TextComponent.of(ThrowableUtils.briefDescription(throwable), TextColor.WHITE))
                .build());
    }

    public void handleBackendJoinGame(JoinGame joinGame) {
        resetPingData(); // reset ping data;
        if (!spawned) {
            // nothing special to do here
            spawned = true;
            player.getConnection().delayedWrite(joinGame);
        } else {
            // Ah, this is the meat and potatoes of the whole venture!
            //
            // In order to handle switching to another server, you will need to send three packets:
            //
            // - The join game packet from the backend server
            // - A respawn packet with a different dimension
            // - Another respawn with the correct dimension
            //
            // The two respawns with different dimensions are required, otherwise the client gets confused.
            //
            // Most notably, by having the client accept the join game packet, we can work around the need to perform
            // entity ID rewrites, eliminating potential issues from rewriting packets and improving compatibility with
            // mods.
            player.getConnection().delayedWrite(joinGame);
            int tempDim = joinGame.getDimension() == 0 ? -1 : 0;
            player.getConnection().delayedWrite(new Respawn(tempDim, joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType()));
            player.getConnection().delayedWrite(new Respawn(joinGame.getDimension(), joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType()));
        }

        // Remove old boss bars. These don't get cleared when sending JoinGame so we need to track these.
        for (UUID serverBossBar : serverBossBars) {
            BossBar deletePacket = new BossBar();
            deletePacket.setUuid(serverBossBar);
            deletePacket.setAction(BossBar.REMOVE);
            player.getConnection().delayedWrite(deletePacket);
        }
        serverBossBars.clear();

        // Tell the server about this client's plugin messages. Velocity will forward them on to the client.
        Collection<String> toRegister = new HashSet<>(clientPluginMsgChannels);
        if (player.getProtocolVersion() >= ProtocolConstants.MINECRAFT_1_13) {
            toRegister.addAll(server.getChannelRegistrar().getModernChannelIds());
        } else {
            toRegister.addAll(server.getChannelRegistrar().getIdsForLegacyConnections());
        }
        if (!toRegister.isEmpty()) {
            String channel = player.getConnection().getProtocolVersion() >= ProtocolConstants.MINECRAFT_1_13 ?
                    "minecraft:register" : "REGISTER";
            player.getConnectedServer().getMinecraftConnection().delayedWrite(PluginMessageUtil.constructChannelsPacket(
                    channel, toRegister));
        }

        // Flush everything
        player.getConnection().flush();
        player.getConnectedServer().getMinecraftConnection().flush();
    }

    public List<UUID> getServerBossBars() {
        return serverBossBars;
    }

    public void handleClientPluginMessage(PluginMessage packet) {
        if (packet.getChannel().equals("REGISTER") || packet.getChannel().equals("minecraft:register")) {
            List<String> actuallyRegistered = new ArrayList<>();
            List<String> channels = PluginMessageUtil.getChannels(packet);
            for (String channel : channels) {
                if (clientPluginMsgChannels.size() >= MAX_PLUGIN_CHANNELS &&
                        !clientPluginMsgChannels.contains(channel)) {
                    throw new IllegalStateException("Too many plugin message channels registered");
                }
                if (clientPluginMsgChannels.add(channel)) {
                    actuallyRegistered.add(channel);
                }
            }

            if (actuallyRegistered.size() > 0) {
                PluginMessage newRegisterPacket = PluginMessageUtil.constructChannelsPacket(packet.getChannel(), actuallyRegistered);
                player.getConnectedServer().getMinecraftConnection().write(newRegisterPacket);
            }

            return;
        }

        if (packet.getChannel().equals("UNREGISTER") || packet.getChannel().equals("minecraft:unregister")) {
            List<String> channels = PluginMessageUtil.getChannels(packet);
            clientPluginMsgChannels.removeAll(channels);
        }

        if (PluginMessageUtil.isMCBrand(packet)) {
            player.getConnectedServer().getMinecraftConnection().write(PluginMessageUtil.rewriteMCBrand(packet));
            return;
        }

        PluginMessageEvent event = new PluginMessageEvent(player, player.getConnectedServer(),
                server.getChannelRegistrar().getFromId(packet.getChannel()), packet.getData());
        server.getEventManager().fire(event)
                .thenAcceptAsync(pme -> {
                    if (pme.getResult().isAllowed()) {
                        player.getConnectedServer().getMinecraftConnection().write(packet);
                    }
                }, player.getConnectedServer().getMinecraftConnection().getChannel().eventLoop());
    }

    public Set<String> getClientPluginMsgChannels() {
        return clientPluginMsgChannels;
    }

    public void setLastPing(long lastPing) {
        this.lastPingID = lastPing;
        this.lastPingSent = System.currentTimeMillis();
    }
    
    private void resetPingData() {
        this.lastPingID = -1;
        this.lastPingSent = -1;
    }
}
