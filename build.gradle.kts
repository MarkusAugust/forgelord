import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("jvm") version "2.4.21"
    // Opens the classes Spring proxies, so @Configuration and @RestController need no `open`.
    kotlin("plugin.spring") version "2.4.21"
    id("org.springframework.boot") version "4.1.2"
}

repositories {
    mavenCentral()
}

val streamlord = "0.23.0"
val lotek = "0.45.0"

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    // Boot 4.1.2 pins jackson-databind 3.1.7, which has CVE-2026-110879. Drop this line once Boot
    // pins 3.1.8 or newer itself.
    implementation(platform("tools.jackson:jackson-bom:3.1.8"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // RestClient.Builder, for calling the Ledger. Boot 4 keeps it out of the web starter.
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    // Reads JSON into Kotlin data classes: the signals, and what the Ledger answers.
    implementation("tools.jackson.module:jackson-module-kotlin")
    // QuestService keeps the board in a StateFlow. Streamlord brings coroutines too, but the app
    // uses them itself, so it says so.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    // Restarts the app when the code is compiled again. Never part of the jar bootJar builds.
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Datastar for Spring MVC, its attributes as kotlinx.html functions, and signals read with
    // the Jackson that Boot already has.
    implementation("io.github.markusaugust.streamlord:streamlord-spring:$streamlord")
    implementation("io.github.markusaugust.streamlord:streamlord-html:$streamlord")
    implementation("io.github.markusaugust.streamlord:streamlord-json-jackson:$streamlord")

    // LO-TEK's builder functions, and its CSS and web components as a WebJar.
    implementation("io.github.markusaugust:lo-tek-interfaces:$lotek")
    implementation("io.github.markusaugust:lo-tek-kotlinx-html:$lotek")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(kotlin("test"))
    // Reads a Datastar response back into events, and the editors' check as a library.
    testImplementation("io.github.markusaugust.streamlord:streamlord-test:$streamlord")
    testImplementation("io.github.markusaugust.streamlord:streamlord-analysis:$streamlord")
    testImplementation("org.jsoup:jsoup:1.24.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

// A component's CSS lives beside its Kotlin, in src/main/kotlin. This puts it on the classpath,
// where the page reads it.
sourceSets.main {
    resources {
        srcDir("src/main/kotlin")
        exclude("**/*.kt")
    }
}

tasks.test {
    useJUnitPlatform()
}
