package com.velocitypowered.proxy.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyChatColorUtilsTest {
    private static final String NON_FORMATTED = "Velocity";
    private static final String FORMATTED = "\u00a7cVelocity";
    private static final String FORMATTED_MULTIPLE = "\u00a7c\u00a7lVelocity";
    private static final String FORMATTED_MULTIPLE_VARIED = "\u00a7c\u00a7lVelo\u00a7a\u00a7mcity";
    private static final String INVALID = "\u00a7gVelocity";
    private static final String RAW_SECTION = "\u00a7";

    @Test
    void removeFormattingNonFormatted() {
        assertEquals(NON_FORMATTED, LegacyChatColorUtils.removeFormatting(NON_FORMATTED));
    }

    @Test
    void removeFormattingFormatted() {
        assertEquals(NON_FORMATTED, LegacyChatColorUtils.removeFormatting(FORMATTED));
    }

    @Test
    void removeFormattingFormattedMultiple() {
        assertEquals(NON_FORMATTED, LegacyChatColorUtils.removeFormatting(FORMATTED_MULTIPLE));
    }

    @Test
    void removeFormattingFormattedMultipleVaried() {
        assertEquals(NON_FORMATTED, LegacyChatColorUtils.removeFormatting(FORMATTED_MULTIPLE_VARIED));
    }

    @Test
    void removeFormattingInvalidFormat() {
        assertEquals(INVALID, LegacyChatColorUtils.removeFormatting(INVALID));
    }

    @Test
    void removeFormattingRawSection() {
        assertEquals(RAW_SECTION, LegacyChatColorUtils.removeFormatting(RAW_SECTION));
    }
    
    @Test
    void translate() {
        assertEquals(FORMATTED, LegacyChatColorUtils.translate('&', "&cVelocity"));
    }

    @Test
    void translateMultiple() {
        assertEquals(FORMATTED_MULTIPLE, LegacyChatColorUtils.translate('&', "&c&lVelocity"));
        assertEquals(FORMATTED_MULTIPLE_VARIED, LegacyChatColorUtils.translate('&', "&c&lVelo&a&mcity"));
    }

    @Test
    void translateDifferentChar() {
        assertEquals(FORMATTED, LegacyChatColorUtils.translate('$', "$cVelocity"));
        assertEquals(FORMATTED_MULTIPLE_VARIED, LegacyChatColorUtils.translate('$', "$c$lVelo$a$mcity"));
    }
}