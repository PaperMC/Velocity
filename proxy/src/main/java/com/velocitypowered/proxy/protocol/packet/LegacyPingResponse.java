package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.server.ServerPing;
import net.kyori.text.serializer.ComponentSerializers;

public class LegacyPingResponse {
    private int protocolVersion;
    private String serverVersion;
    private String motd;
    private int playersOnline;
    private int playersMax;

    public LegacyPingResponse() {
    }

    public LegacyPingResponse(int protocolVersion, String serverVersion, String motd, int playersOnline, int playersMax) {
        this.protocolVersion = protocolVersion;
        this.serverVersion = serverVersion;
        this.motd = motd;
        this.playersOnline = playersOnline;
        this.playersMax = playersMax;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public int getPlayersOnline() {
        return playersOnline;
    }

    public void setPlayersOnline(int playersOnline) {
        this.playersOnline = playersOnline;
    }

    public int getPlayersMax() {
        return playersMax;
    }

    public void setPlayersMax(int playersMax) {
        this.playersMax = playersMax;
    }

    @Override
    public String toString() {
        return "LegacyPingResponse{" +
                "protocolVersion=" + protocolVersion +
                ", serverVersion='" + serverVersion + '\'' +
                ", motd='" + motd + '\'' +
                ", playersOnline=" + playersOnline +
                ", playersMax=" + playersMax +
                '}';
    }

    public static LegacyPingResponse from(ServerPing ping) {
        return new LegacyPingResponse(ping.getVersion().getProtocol(),
                ping.getVersion().getName(),
                ComponentSerializers.LEGACY.serialize(ping.getDescription()),
                ping.getPlayers().getOnline(),
                ping.getPlayers().getMax());
    }
}
