package com.velocitypowered.api.playersettings;

import java.util.Locale;

public interface PlayerSettings {

    Locale getLocate();

    byte getViewDistance();

    ChatMode getChatMode();

    boolean hasChatColors();

    SkinParts getSkinParts();

    MainHand getMainHand();

    public enum ChatMode {
        SHOWN,
        COMMANDS_ONLY,
        HIDDEN
    }

    public enum MainHand {
        LEFT,
        RIGHT
    }
}
