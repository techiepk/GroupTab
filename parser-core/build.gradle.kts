plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "com.pennywiseai"
version = "0.1.0-SNAPSHOT"

// Use root project's Java toolchain; avoid forcing downloads here

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "parser-core"
            version = version.toString()
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configure JUnit testing
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}



