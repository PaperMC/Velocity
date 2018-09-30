package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import com.velocitypowered.proxy.tablist.VelocityTabList;
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
    private static final int MAX_PLUGIN_CHANNELS = 1024;

    private final ConnectedPlayer player;
    private boolean spawned = false;
    private final List<UUID> serverBossBars = new ArrayList<>();
    private final Set<String> clientPluginMsgChannels = new HashSet<>();
    private final Queue<PluginMessage> loginPluginMessages = new ArrayDeque<>();
    private final VelocityServer server;
    private TabCompleteRequest outstandingTabComplete;

    public ClientPlaySessionHandler(VelocityServer server, ConnectedPlayer player) {
        this.player = player;
        this.server = server;
    }

    @Override
    public void activated() {
        PluginMessage register = PluginMessageUtil.constructChannelsPacket(player.getProtocolVersion(), server.getChannelRegistrar().getModernChannelIds());
        player.getConnection().write(register);
    }

    @Override
    public boolean handle(KeepAlive packet) {
        VelocityServerConnection serverConnection = player.getConnectedServer();
        if (serverConnection != null && packet.getRandomId() == serverConnection.getLastPingId()) {
            player.setPing(System.currentTimeMillis() - serverConnection.getLastPingSent());
            serverConnection.getConnection().write(packet);
            serverConnection.resetLastPingId();
        }
        return true;
    }

    @Override
    public boolean handle(ClientSettings packet) {
        player.setPlayerSettings(packet);
        return false; // will forward onto the handleGeneric below, which will write the packet to the remote server
    }

    @Override
    public boolean handle(Chat packet) {
        String msg = packet.getMessage();
        if (msg.startsWith("/")) {
            try {
                if (!server.getCommandManager().execute(player, msg.substring(1))) {
                    return false;
                }
            } catch (Exception e) {
                logger.info("Exception occurred while running command for {}", player.getProfile().getName(), e);
                player.sendMessage(TextComponent.of("An error occurred while running this command.", TextColor.RED));
                return true;
            }
        } else {
            VelocityServerConnection serverConnection = player.getConnectedServer();
            if (serverConnection == null) {
                return true;
            }
            PlayerChatEvent event = new PlayerChatEvent(player, msg);
            server.getEventManager().fire(event)
                    .thenAcceptAsync(pme -> {
                        if (pme.getResult().equals(PlayerChatEvent.ChatResult.allowed())){
                            serverConnection.getConnection().write(packet);
                        } else if (pme.getResult().isAllowed() && pme.getResult().getMessage().isPresent()){
                            serverConnection.getConnection().write(Chat.createServerbound(pme.getResult().getMessage().get()));
                        }
                    }, serverConnection.getConnection().eventLoop());
        }
        return true;
    }

    @Override
    public boolean handle(TabCompleteRequest packet) {
        // Record the request so that the outstanding request can be augmented later.
        if (!packet.isAssumeCommand() && packet.getCommand().startsWith("/")) {
            int spacePos = packet.getCommand().indexOf(' ');
            if (spacePos > 0) {
                String cmd = packet.getCommand().substring(1, spacePos);
                if (server.getCommandManager().hasCommand(cmd)) {
                    Optional<List<String>> suggestions = server.getCommandManager().offerSuggestions(player, packet.getCommand().substring(1));
                    if (suggestions.isPresent()) {
                        TabCompleteResponse resp = new TabCompleteResponse();
                        resp.getOffers().addAll(suggestions.get());
                        player.getConnection().write(resp);
                        return true;
                    }
                }
            }
        }
        outstandingTabComplete = packet;
        return false;
    }

    @Override
    public boolean handle(PluginMessage packet) {
        if (PluginMessageUtil.isMCRegister(packet)) {
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
                PluginMessage newRegisterPacket = PluginMessageUtil.constructChannelsPacket(player.getProtocolVersion(), actuallyRegistered);
                player.getConnectedServer().getConnection().write(newRegisterPacket);
            }
        } else if (PluginMessageUtil.isMCUnregister(packet)) {
            List<String> channels = PluginMessageUtil.getChannels(packet);
            clientPluginMsgChannels.removeAll(channels);
            player.getConnectedServer().getConnection().write(packet);
        } else if (PluginMessageUtil.isMCBrand(packet)) {
            player.getConnectedServer().getConnection().write(PluginMessageUtil.rewriteMCBrand(packet));
        } else if (player.getConnectedServer().isLegacyForge() && !player.getConnectedServer().hasCompletedJoin()) {
            if (packet.getChannel().equals(VelocityConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)) {
                // Always forward the FML handshake to the remote server.
                player.getConnectedServer().getConnection().write(packet);
            } else {
                // The client is trying to send messages too early. This is primarily caused by mods, but it's further
                // aggravated by Velocity. To work around these issues, we will queue any non-FML handshake messages to
                // be sent once the JoinGame packet has been received by the proxy.
                loginPluginMessages.add(packet);
            }
        } else {
            ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
            if (id == null) {
                player.getConnectedServer().getConnection().write(packet);
            } else {
                PluginMessageEvent event = new PluginMessageEvent(player, player.getConnectedServer(), id, packet.getData());
                server.getEventManager().fire(event)
                        .thenAcceptAsync(pme -> {
                            if (pme.getResult().isAllowed()) {
                                player.getConnectedServer().getConnection().write(packet);
                            }
                        }, player.getConnectedServer().getConnection().eventLoop());
            }
        }
        return true;
    }

    @Override
    public void handleGeneric(MinecraftPacket packet) {
        VelocityServerConnection serverConnection = player.getConnectedServer();
        if (serverConnection == null) {
            // No server connection yet, probably transitioning.
            return;
        }

        // If we don't want to handle this packet, just forward it on.
        if (serverConnection.hasCompletedJoin()) {
            serverConnection.getConnection().write(packet);
        }
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        VelocityServerConnection serverConnection = player.getConnectedServer();
        if (serverConnection == null) {
            // No server connection yet, probably transitioning.
            return;
        }

        if (serverConnection.hasCompletedJoin()) {
            serverConnection.getConnection().write(buf.retain());
        }
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

    @Override
    public void writabilityChanged() {
        VelocityServerConnection server = player.getConnectedServer();
        if (server != null) {
            boolean writable = player.getConnection().getChannel().isWritable();
            server.getConnection().getChannel().config().setAutoRead(writable);
        }
    }

    public void handleBackendJoinGame(JoinGame joinGame) {
        if (!spawned) {
            // Nothing special to do with regards to spawning the player
            spawned = true;
            player.getConnection().delayedWrite(joinGame);

            // We have something special to do for legacy Forge servers - during first connection the FML handshake
            // will transition to complete regardless. Thus, we need to ensure that a reset packet is ALWAYS sent on
            // first switch.
            //
            // As we know that calling this branch only happens on first join, we set that if we are a Forge
            // client that we must reset on the next switch.
            //
            // The call will handle if the player is not a Forge player appropriately.
            player.getConnection().setCanSendLegacyFMLResetPacket(true);
        } else {
            // Clear tab list to avoid duplicate entries
            TabList tabList = player.getTabList();
            tabList.getEntries().forEach(entry -> tabList.removeEntry(entry.getProfile().idAsUuid()));

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
            player.getConnectedServer().getConnection().delayedWrite(PluginMessageUtil.constructChannelsPacket(
                    player.getConnection().getProtocolVersion(), toRegister));
        }

        // If we had plugin messages queued during login/FML handshake, send them now.
        PluginMessage pm;
        while ((pm = loginPluginMessages.poll()) != null) {
            player.getConnectedServer().getConnection().delayedWrite(pm);
        }

        // Clear any title from the previous server.
        player.getConnection().delayedWrite(TitlePacket.resetForProtocolVersion(player.getProtocolVersion()));

        // Flush everything
        player.getConnection().flush();
        player.getConnectedServer().getConnection().flush();
        player.getConnectedServer().setHasCompletedJoin(true);
        if (player.getConnectedServer().isLegacyForge()) {
            // We only need to indicate we can send a reset packet if we complete a handshake, that is,
            // logged onto a Forge server.
            //
            // The special case is if we log onto a Vanilla server as our first server, FML will treat this
            // as complete and **will** need a reset packet sending at some point. We will handle this
            // during initial player connection if the player is detected to be forge.
            //
            // This is why we use an if statement rather than the result of VelocityServerConnection#isLegacyForge()
            // because we don't want to set it false if this is a first connection to a Vanilla server.
            //
            // See LoginSessionHandler#handle for where the counterpart to this method is
            player.getConnection().setCanSendLegacyFMLResetPacket(true);
        }
    }

    public List<UUID> getServerBossBars() {
        return serverBossBars;
    }

    public Set<String> getClientPluginMsgChannels() {
        return clientPluginMsgChannels;
    }

    public void handleTabCompleteResponse(TabCompleteResponse response) {
        logger.info("Got {}", response);
        logger.info("Request {}", outstandingTabComplete);
        if (outstandingTabComplete != null) {
            logger.info("HANDLING");
            if (!outstandingTabComplete.isAssumeCommand()) {
                String command = outstandingTabComplete.getCommand().substring(1);
                try {
                    Optional<List<String>> offers = server.getCommandManager().offerSuggestions(player, command);
                    offers.ifPresent(strings -> response.getOffers().addAll(strings));
                } catch (Exception e) {
                    logger.error("Unable to provide tab list completions for {} for command '{}'", player.getUsername(),
                            command, e);
                }
                outstandingTabComplete = null;
            }
            player.getConnection().write(response);
        }
    }
}
