package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.regex.Pattern;

/**
 * LegacyChatColorUtils contains utilities for handling legacy Minecraft color codes. Generally, you should prefer
 * JSON-based components, but for convenience Velocity provides a limited set of tools to handle Minecraft color codes.
 */
public class LegacyChatColorUtils {
    private LegacyChatColorUtils() {
        throw new AssertionError();
    }

    /**
     * Represents the legacy Minecraft format character, the section symbol.
     */
    public static final char FORMAT_CHAR = '\u00a7';

    /**
     * Translates a string with Minecraft color codes prefixed with a different character than the section symbol into
     * a string that uses the section symbol.
     * @param originalChar the char the color codes are prefixed by
     * @param text the text to translate
     * @return the translated text
     */
    public static String translate(char originalChar, @NonNull String text) {
        Preconditions.checkNotNull(text, "text");
        char[] textChars = text.toCharArray();
        int foundSectionIdx = -1;
        for (int i = 0; i < textChars.length; i++) {
            char textChar = textChars[i];
            if (textChar == originalChar) {
                foundSectionIdx = i;
                continue;
            }

            if (foundSectionIdx >= 0) {
                textChar = Character.toLowerCase(textChar);
                if ((textChar >= 'a' && textChar <= 'f') || (textChar >= '0' && textChar <= '9') ||
                        (textChar >= 'l' && textChar <= 'o' || textChar == 'r')) {
                    textChars[foundSectionIdx] = FORMAT_CHAR;
                }
                foundSectionIdx = -1;
            }
        }
        return new String(textChars);
    }

    /**
     * A regex that matches all Minecraft color codes and removes them.
     */
    private static final Pattern CHAT_COLOR_MATCHER = Pattern.compile("(?i)" + Character.toString(FORMAT_CHAR) + "[0-9A-FL-OR]");

    /**
     * Removes all Minecraft color codes from the string.
     * @param text the text to remove color codes from
     * @return a new String without Minecraft color codes
     */
    public static String removeFormatting(@NonNull String text) {
        Preconditions.checkNotNull(text, "text");
        return CHAT_COLOR_MATCHER.matcher(text).replaceAll("");
    }
}
