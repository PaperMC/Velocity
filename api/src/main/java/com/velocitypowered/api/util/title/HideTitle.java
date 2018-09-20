package com.velocitypowered.api.util.title;

/**
 * A virtual "title" you may send to hide the title on the client side.
 */
public class HideTitle implements Title {
    private static final HideTitle INSTANCE = new HideTitle();

    private HideTitle() {

    }

    /**
     * Returns the hide title.
     * @return the hide title
     */
    public static HideTitle of() {
        return INSTANCE;
    }
}
