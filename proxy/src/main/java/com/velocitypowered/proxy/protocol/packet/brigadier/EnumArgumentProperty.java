package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class EnumArgumentProperty implements ArgumentType<String> {

    private final String className;

    public EnumArgumentProperty(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readUnquotedString();
    }
}
