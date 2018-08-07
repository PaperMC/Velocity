package com.velocitypowered.api.plugin;

/**
 * @author Hugo Manrique
 * @since 07/08/2018
 */
public class InvalidPluginException extends Exception {
    public InvalidPluginException() {
        super();
    }

    public InvalidPluginException(String message) {
        super(message);
    }

    public InvalidPluginException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPluginException(Throwable cause) {
        super(cause);
    }
}
