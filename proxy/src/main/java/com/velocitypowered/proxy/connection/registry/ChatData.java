/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.connection.registry;

/*
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
*/

// TODO Implement
public class ChatData {
    /*
    private static final ListBinaryTag EMPTY_LIST_TAG = ListBinaryTag.empty();


    private final String identifier;
    private final int id;
    private final Map<>


    public static class Decoration implements Translatable {

        private final List<String> parameters;
        private final List<TextFormat> style;
        private final String translationKey;

        public List<String> getParameters() {
            return parameters;
        }

        public List<TextFormat> getStyle() {
            return style;
        }

        @Override
        public @NotNull String translationKey() {
            return translationKey;
        }

        public Decoration(List<String> parameters, List<TextFormat> style, String translationKey) {
            this.parameters = Preconditions.checkNotNull(parameters);
            this.style = Preconditions.checkNotNull(style);
            this.translationKey = Preconditions.checkNotNull(translationKey);
            Preconditions.checkArgument(translationKey.length() > 0);
        }

        public static Decoration decodeRegistryEntry(CompoundBinaryTag toDecode) {
            ImmutableList.Builder<String> parameters = ImmutableList.builder();
            ListBinaryTag paramList = toDecode.getList("parameters", EMPTY_LIST_TAG);
            if (paramList != EMPTY_LIST_TAG) {
                paramList.forEach(binaryTag -> parameters.add(binaryTag.toString()));
            }

            ImmutableList.Builder<TextFormat> style = ImmutableList.builder();
            CompoundBinaryTag styleList = toDecode.getCompound("style");
            for (String key : styleList.keySet()) {
                if ("color".equals(key)) {
                    NamedTextColor color = Preconditions.checkNotNull(
                            NamedTextColor.NAMES.value(styleList.getString(key)));
                    style.add(color);
                } else {
                    // Key is a Style option instead
                    TextDecoration deco = TextDecoration.NAMES.value(key);
                    // This wouldn't be here if it wasn't applied, but here it goes anyway:
                    byte val = styleList.getByte(key);
                    if (val != 0) {
                        style.add(deco);
                    }
                }
            }

            String translationKey = toDecode.getString("translation_key");

            return new Decoration(parameters.build(), style.build(), translationKey);
        }

        public void encodeRegistryEntry(CompoundBinaryTag )

    }

    public static enum Priority {
        SYSTEM,
        CHAT
    }
*/
}
