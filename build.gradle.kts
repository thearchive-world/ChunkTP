plugins {
    java
}

group = "world.thearchive"
version = property("pluginVersion") as String
description = "Lets you teleport to chunk coordinates"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        val props = project.properties
        filesMatching("**/*.yml") {
            expand(props)
        }
    }
}
