import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm") version "1.6.20"
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
    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    implementation(kotlin("stdlib-jdk8"))
}

val targetJavaVersion = 8
val javaVersion = JavaVersion.toVersion(targetJavaVersion)
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible)
            options.release.set(targetJavaVersion)
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

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = javaVersion.toString()
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = javaVersion.toString()
}