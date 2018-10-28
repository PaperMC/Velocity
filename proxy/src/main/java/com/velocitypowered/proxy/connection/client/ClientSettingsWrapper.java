package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.SkinParts;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

public class ClientSettingsWrapper implements PlayerSettings {
    static PlayerSettings DEFAULT = new ClientSettingsWrapper(new ClientSettings("en_US", (byte) 10, 0, true, (short)127, 1));
    
    private final ClientSettings settings;
    private final SkinParts parts;
    private @Nullable Locale locale;

    ClientSettingsWrapper(ClientSettings settings) {
        this.settings = settings;
        this.parts = new SkinParts((byte) settings.getSkinParts());
    }

    @Override
    public Locale getLocale() {
        return locale == null ? locale = Locale.forLanguageTag(settings.getLocale().replaceAll("_", "-")) : locale; //Will throw error if locale not found
    }

    @Override
    public byte getViewDistance() {
        return settings.getViewDistance();
    }

    @Override
    public ChatMode getChatMode() {
        int chat = settings.getChatVisibility();
        if (chat < 0 || chat > 2) {
            return ChatMode.SHOWN;
        }
        return ChatMode.values()[chat];
    }

    @Override
    public boolean hasChatColors() {
        return settings.isChatColors();
    }

    @Override
    public SkinParts getSkinParts() {
        return parts;
    }

    @Override
    public MainHand getMainHand() {
        return settings.getMainHand() == 1 ? MainHand.RIGHT : MainHand.LEFT;
    }
    
    
}
