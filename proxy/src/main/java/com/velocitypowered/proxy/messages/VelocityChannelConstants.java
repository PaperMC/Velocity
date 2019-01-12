package com.velocitypowered.proxy.messages;

/**
 * Represents all currently constants that are currently used by
 * Velocity Plugin Messaging API
 */
public final class VelocityChannelConstants {

    private VelocityChannelConstants() { }

    public static final String CHANNEL_NAME = "velocity:control";
    public static final short API_VERSION = 1;

    public static final class Actions {

        private Actions() { }

        public static final byte IDENTIFY = 0x00;
        public static final byte FETCH_PLAYERS = 0x01;
        public static final byte CONNECT = 0x02;
        public static final byte FORWARD = 0x03;
        public static final byte LOCATE = 0x05;
        public static final byte SERVER_PLAYERS = 0x06;
    }

    public static final class PlayerRepresentation {

        private PlayerRepresentation() { }

        public static final byte UUID = 0x00;
        public static final byte NAME = 0x01;
    }
}
