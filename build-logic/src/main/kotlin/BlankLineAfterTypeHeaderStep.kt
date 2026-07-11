/*
 * Copyright (C) 2026 Velocity Contributors
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

import com.diffplug.spotless.FormatterFunc
import java.io.Serializable

/**
 * Spotless step that forces a single blank line after every type declaration's opening brace,
 * e.g. after `class Foo {`, `interface Bar {`, `enum Baz {` or `record Qux(...) {`.
 *
 * Implemented as a named class (rather than a `custom { ... }` lambda) because [FormatterFunc]
 * extends [Serializable], and Kotlin SAM-conversion lambdas are not serializable — which breaks
 * Gradle's configuration cache when Spotless fingerprints its step list.
 */
class BlankLineAfterTypeHeaderStep : FormatterFunc, Serializable {

    override fun apply(input: String): String =
        REGEX.replace(input) { match -> match.groupValues[1] + "\n\n" }

    companion object {

        private const val serialVersionUID = 1L

        // Anchors on a line starting with optional modifiers + a type keyword, lets the header
        // span multiple lines up to its opening brace, then matches only when the following line
        // is not already blank.
        private val REGEX = Regex(
            """(?m)^([ \t]*(?:(?:public|protected|private|abstract|final|sealed|non-sealed|static|strictfp)[ \t]+)*""" +
                """(?:class|interface|enum|record|@interface)\b[^{]*\{)[ \t]*\r?\n(?![ \t]*\r?\n)""",
        )
    }
}
