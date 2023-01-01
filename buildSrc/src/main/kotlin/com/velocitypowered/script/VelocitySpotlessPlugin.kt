/*
 * Copyright (C) 2023 Velocity Contributors
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

package com.velocitypowered.script

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import java.io.File

class VelocitySpotlessPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.configure()

    private fun Project.configure() {
        apply<SpotlessPlugin>()

        extensions.configure<SpotlessExtension> {
            java {
                if (project.name == "velocity-api") {
                    licenseHeaderFile(project.file("HEADER.txt"))
                } else {
                    licenseHeaderFile(project.rootProject.file("HEADER.txt"))
                }

                removeUnusedImports()
            }
        }
    }
}