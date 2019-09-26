package com.saantiaguilera.gradle.plugin.fusrodah

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

open class FusRoDahPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val android = target.findProperty("android")
        if (android is BaseAppModuleExtension) {
            val extension = target.extensions.create("fusRoDah", FusRoDahExtension::class.java)
            android.registerTransform(FusRoDahTransformer(extension, android))
        }
    }

}

open class FusRoDahExtension(
    var checkEmulator: Boolean = true
)
