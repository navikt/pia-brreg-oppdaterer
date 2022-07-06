plugins {
    application
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
}

group = "no.navikt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val kotlinx_serialization_json_version: String by project
val kotlinx_datetime_version: String by project
val junit_version: String by project
val ktor_version: String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_json_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
    implementation("org.junit.jupiter:junit-jupiter:$junit_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:$ktor_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.3")
}

tasks.test {
    useJUnitPlatform()
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