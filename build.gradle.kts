plugins {
    kotlin("jvm") version "1.6.21"
    id("org.sonarqube") version "3.3"
}

group = "io.github"
version = "1.02"

val kotlinJvmTarget = JavaVersion.toVersion(8)

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    // Can't use exclude with kotlin("stdlib-jdk8").
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.21") {
        exclude(module = "annotations")
    }
    // For some reason Kotlin uses version 13.0 which was literally released in Dec 17, 2013.
    implementation("org.jetbrains:annotations:23.0.0")

    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")

    testImplementation(kotlin("test"))
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "$kotlinJvmTarget"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "$kotlinJvmTarget"
        }
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
    test {
        useJUnitPlatform()
    }
    jar {
        val dependencies = configurations.runtimeClasspath.get().map(::zipTree)
        from(dependencies)
        // We may omit them without any troubles or errors because we never use the Kotlin Reflect library in this project.
        exclude(
            "**/*.kotlin_metadata",
            "**/*.kotlin_module",
            "**/*.kotlin_builtins",
            "META-INF/maven/**",
            "META-INF/versions/**"
        )
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "128931_ClearChat")
        property("sonar.organization", "128931")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
