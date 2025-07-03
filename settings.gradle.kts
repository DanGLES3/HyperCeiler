@file:Suppress("UnstableApiUsage")
var gprUser = System.getenv("GIT_ACTOR") ?:""
var gprKey = System.getenv("GIT_TOKEN") ?: ""

val gprInfoFile = File(rootProject.projectDir, "signing.properties")

if (gprUser.isEmpty() || gprKey.isEmpty()) {
    if (gprInfoFile.exists()) {
        val gprInfo = java.util.Properties().apply {
            gprInfoFile.inputStream().use { load(it) }
        }

        gprUser = gprInfo.getProperty("gpr.user") ?: ""
        gprKey = gprInfo.getProperty("gpr.key") ?: ""
    }

    // No longer throw an exception if credentials are missing
    if (gprUser.isEmpty() || gprKey.isEmpty()) {
        println("Warning: 'gpr.user' and/or 'gpr.key' are not set. Some publishing features may not work.")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/ReChronoRain/HyperCeiler")
            credentials {
                username = gprUser
                password = gprKey
            }
        }
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
    }
}

rootProject.name = "HyperCeiler"
include("app")
include(":library:common-ui", ":library:hook", "library:processor", "library:hidden-api")
