package com.velocitypowered.proxy.util;

import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentUtilsTest {

    private static final String SIMPLE_COMPONENT_TEXT = "hello";
    private static final TextComponent SIMPLE_COMPONENT = TextComponent.of(SIMPLE_COMPONENT_TEXT, TextColor.RED);
    private static final String COMPLEX_COMPONENT_TEXT = "Hello world! Welcome to Velocity, the Minecraft server proxy built for mass scale.";
    private static final TextComponent COMPLEX_COMPONENT = TextComponent.builder("Hello world! ")
            .decoration(TextDecoration.BOLD, true)
            .append(TextComponent.of("Welcome to "))
            .decoration(TextDecoration.BOLD, false)
            .color(TextColor.GREEN)
            .append(TextComponent.of("Velocity"))
            .color(TextColor.DARK_AQUA)
            .append(TextComponent.of(", the Minecraft server proxy built for mass scale."))
            .resetStyle()
            .build();

    @Test
    void asPlainText() {
        assertEquals(SIMPLE_COMPONENT_TEXT, ComponentUtils.asPlainText(SIMPLE_COMPONENT));
        assertEquals(COMPLEX_COMPONENT_TEXT, ComponentUtils.asPlainText(COMPLEX_COMPONENT));
    }
}