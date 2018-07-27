package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;

public enum ComponentUtils {
    ;

    public static String asPlainText(Component component) {
        Preconditions.checkNotNull(component, "component");
        StringBuilder builder = new StringBuilder();
        appendPlainText(component, builder);
        return builder.toString();
    }

    private static void appendPlainText(Component component, StringBuilder builder) {
        if (component instanceof TextComponent) {
            builder.append(((TextComponent) component).content());
        }
        if (component instanceof TranslatableComponent) {
            builder.append(((TranslatableComponent) component).key());
        }
        for (Component child : component.children()) {
            appendPlainText(child, builder);
        }
    }
}
