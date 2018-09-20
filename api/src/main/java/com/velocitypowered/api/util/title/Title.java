package com.velocitypowered.api.util.title;

/**
 * Represents a title that can be sent to a Minecraft client.
 */
public interface Title {
    Title RESET = new Title() {
        @Override
        public String toString() {
            return "reset title";
        }
    };

    Title HIDE = new Title() {
        @Override
        public String toString() {
            return "hide title";
        }
    };

    static Title reset() {
        return RESET;
    }

    static Title hide() {
        return HIDE;
    }

    static TextTitle.Builder text() {
        return TextTitle.builder();
    }
}
