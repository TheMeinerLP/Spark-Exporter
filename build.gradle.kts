import xyz.jpenilla.runpaper.task.RunServer

plugins {
    kotlin("jvm") version "1.8.21"
    id("xyz.jpenilla.run-paper") version "2.0.1"
    id("com.github.johnrengelman.shadow") version "7+"
}

group = "dev.themeinerlp"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    // Paper
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    implementation("io.papermc:paperlib:1.0.8")
    implementation("com.influxdb:influxdb-client-java:6.8.0")
    compileOnly("me.lucko:spark-api:0.1-SNAPSHOT")
    testImplementation(kotlin("test"))
}

val supportedVersions = listOf("1.16.5", "1.17", "1.17.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4")
tasks {
    test {
        useJUnitPlatform()
    }
    processResources {
        filesMatching("**/paper-plugin.yml") {
            filter {
                it.replace("\$VERSION\$", project.version.toString())
            }
        }
    }
    runServer {
        minecraftVersion("1.19.4")
        // build(298)
        // minecraftVersion("1.19.2")
        minecraftVersion("1.19.4")
        jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true")
    }
    supportedVersions.forEach {
        register<RunServer>("runServer-$it") {
            dependsOn(shadowJar)
            pluginJars(shadowJar.get().archiveFile)
            minecraftVersion(it)
            jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true")
            group = "run paper"
            runDirectory.set(file("run-$it"))
        }
    }
}

kotlin {
    jvmToolchain(17)
}