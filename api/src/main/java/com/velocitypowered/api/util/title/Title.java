package com.velocitypowered.api.util.title;

/**
 * Represents a title that can be sent to a Minecraft client.
 */
public interface Title {
    /**
     * A title that, when sent to the client, will cause all title data to be reset and any existing title to be hidden.
     */
    Title RESET = new Title() {
        @Override
        public String toString() {
            return "reset title";
        }
    };

    /**
     * A title that, when sent to the client, will cause any existing title to be hidden. The title may be restored by
     * a {@link TextTitle} with no title or subtitle (only a time).
     */
    Title HIDE = new Title() {
        @Override
        public String toString() {
            return "hide title";
        }
    };

    /**
     * Returns the {@link #RESET} title.
     * @return the reset title
     */
    static Title reset() {
        return RESET;
    }

    /**
     * Returns the {@link #HIDE} title.
     * @return the hide title
     */
    static Title hide() {
        return HIDE;
    }

    /**
     * Returns a builder for {@link TextTitle}s.
     * @return a builder for text titles
     */
    static TextTitle.Builder text() {
        return TextTitle.builder();
    }
}
