package io.minimum.minecraft.velocity.util;

public enum ThrowableUtils {
    ;

    public static String briefDescription(Throwable throwable) {
        return throwable.getClass().getName() + ": " + throwable.getMessage();
    }
}
