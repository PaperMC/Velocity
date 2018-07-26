package com.velocitypowered.proxy.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class UuidUtilsTest {
    private static final UUID VALID_UUID = UUID.fromString("6b501978-d3be-4f33-bcf6-6e7808f37a0d");
    private static final String VALID_MOJANG_UUID = "6b501978d3be4f33bcf66e7808f37a0d";

    private static final UUID TEST_OFFLINE_PLAYER_UUID = UUID.fromString("708f6260-183d-3912-bbde-5e279a5e739a");
    private static final String TEST_OFFLINE_PLAYER = "tuxed";

    @Test
    public void fromMojang() {
        Assert.assertEquals("UUIDs do not match", VALID_UUID, UuidUtils.fromMojang(VALID_MOJANG_UUID));
    }

    @Test
    public void generateOfflinePlayerUuid() {
        Assert.assertEquals("UUIDs do not match", TEST_OFFLINE_PLAYER_UUID, UuidUtils.generateOfflinePlayerUuid(TEST_OFFLINE_PLAYER));
    }
}