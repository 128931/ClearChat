plugins {
    kotlin("jvm") version "1.6.20"
    id("org.sonarqube") version "3.3"
}

group = "me.onetwoeight"
version = "1.0"

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
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

val targetJavaVersion = 8
val javaVersion = JavaVersion.toVersion(targetJavaVersion)

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
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
        // Due to never using the Kotlin Reflect library in this project we can exclude these without any issues/errors occurring
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