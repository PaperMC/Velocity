package com.velocitypowered.proxy.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UuidUtilsTest {
    private static final UUID EXPECTED_DASHED_UUID = UUID.fromString("6b501978-d3be-4f33-bcf6-6e7808f37a0d");
    private static final String ACTUAL_UNDASHED_UUID = EXPECTED_DASHED_UUID.toString().replace("-", "");

    private static final UUID TEST_OFFLINE_PLAYER_UUID = UUID.fromString("708f6260-183d-3912-bbde-5e279a5e739a");
    private static final String TEST_OFFLINE_PLAYER = "tuxed";

    @Test
    void generateOfflinePlayerUuid() {
        assertEquals(TEST_OFFLINE_PLAYER_UUID, UuidUtils.generateOfflinePlayerUuid(TEST_OFFLINE_PLAYER), "UUIDs do not match");
    }

    @Test
    void fromUndashed() {
        assertEquals(EXPECTED_DASHED_UUID, UuidUtils.fromUndashed(ACTUAL_UNDASHED_UUID), "UUIDs do not match");
    }

    @Test
    void toUndashed() {
        assertEquals(ACTUAL_UNDASHED_UUID, UuidUtils.toUndashed(EXPECTED_DASHED_UUID), "UUIDs do not match");
    }
}
