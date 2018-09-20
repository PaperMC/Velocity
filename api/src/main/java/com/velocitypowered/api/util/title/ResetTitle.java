package com.velocitypowered.api.util.title;

/**
 * A virtual "title" that instructs the client to reset all title-related data.
 */
public class ResetTitle implements Title {
    private static final ResetTitle INSTANCE = new ResetTitle();

    private ResetTitle() {

    }

    /**
     * Returns the reset title.
     * @return the reset title
     */
    public static ResetTitle of() {
        return INSTANCE;
    }
}
