val h2_version: String by project
val koin_version: String by project
val kotlin_version: String by project
val kotlinx_browser_version: String by project
val kotlinx_html_version: String by project
val logback_version: String by project
val postgres_version: String by project

plugins {
    kotlin("jvm") version "2.2.20"
    id("io.ktor.plugin") version "3.2.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_21)
        localImageName.set("pennywise-ktor")
        imageTag.set("latest")
        portMappings.set(listOf(
            io.ktor.plugin.features.DockerPortMapping(
                8080,
                8080,
                io.ktor.plugin.features.DockerPortMappingProtocol.TCP
            )
        ))
    }
}

dependencies {
    implementation("org.openfolder:kotlin-asyncapi-ktor:3.1.1")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-gson")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinx_html_version")
    implementation("io.ktor:ktor-server-htmx")
    implementation("io.ktor:ktor-htmx-html")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css-jvm:2025.6.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    // CORS
    implementation("io.ktor:ktor-server-cors")
    // Shared parser core
    implementation("com.pennywiseai:parser-core:0.1.0-SNAPSHOT")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
