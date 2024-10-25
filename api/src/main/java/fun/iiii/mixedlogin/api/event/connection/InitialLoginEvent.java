/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package fun.iiii.mixedlogin.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;

/**
 * This event is fired once the player has been authenticated, but before they connect to a server.
 * Velocity will wait for this event to finish firing before proceeding with the rest of the login
 * process, but you should try to limit the work done in any event that fires during the login
 * process.
 */
@AwaitingEvent
public final class InitialLoginEvent implements ResultedEvent<ResultedEvent.ComponentResult> {
    private final String userName;
    private final String serverId;
    private final String playerIp;
    private final boolean isOnline;

    private boolean success = false;
    private boolean ignoreKey = false;

    private PlayerChatEvent.ComponentResult result;
    private Throwable throwable;
    private Component disconnectComponent = Component.text("未知错误");
    private GameProfile gameProfile;

    public InitialLoginEvent(String userName, String serverId, String playerIp, boolean isOnline) {
        this.userName = userName;
        this.serverId = serverId;
        this.playerIp = playerIp;
        this.isOnline = isOnline;
    }

    public String getPlayerIp() {
        return playerIp;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public boolean isIgnoreKey() {
        return ignoreKey;
    }

    public void setIgnoreKey(boolean ignoreKey) {
        this.ignoreKey = ignoreKey;
    }

    public GameProfile getGameProfile() {
        return gameProfile;
    }

    public void setGameProfile(GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public String getUserName() {
        return userName;
    }

    public String getServerId() {
        return serverId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public ComponentResult getResult() {
        return result;
    }

    public Component getDisconnectComponent() {
        return disconnectComponent;
    }

    public void setDisconnectComponent(Component disconnectComponent) {
        this.disconnectComponent = disconnectComponent;
    }

    @Override
    public void setResult(ComponentResult result) {
        this.result = Preconditions.checkNotNull(result, "result");
    }

}
