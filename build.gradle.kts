val gsonVersion = "2.13.2"
val junitJupiterVersion = "6.0.2"
val kotestVersion = "6.0.7"
val ktorVersion = "3.3.3"
val kotlinxCoroutinesTestVersion = "1.10.2"
val logbackClassicVersion = "1.5.24"
val logbackEncoderVersion = "9.0"
val testcontainersVersion = "2.0.3"
val wiremockVersion = "3.13.2"

plugins {
    application
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
}

group = "no.navikt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Kafka
    implementation("at.yawk.lz4:lz4-java:1.10.2")
    implementation("org.apache.kafka:kafka-clients:4.1.1") {
        // "Fikser CVE-2025-12183 - lz4-java >1.8.1 har sårbar versjon (transitive dependency fra kafka-clients:4.1.0)"
        exclude("org.lz4", "lz4-java")
    }

    implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesTestVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-kafka:$testcontainersVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")

    constraints {
        implementation("net.minidev:json-smart") {
            version {
                require("2.6.0")
            }
            because(
                "versjoner < 2.5.2 har diverse sårbarheter. Inkludert i kotest-assertions-json:6.0.4",
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        dependsOn(installDist)
        useJUnitPlatform()
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "JobKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    configurations.compileClasspath.get().forEach {
        from(if (it.isDirectory) it else zipTree(it))
    }
}
