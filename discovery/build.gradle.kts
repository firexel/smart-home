plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("com.seraph.discovery.Main")
}

repositories {
    mavenCentral()
}

tasks {
    // Use the native JUnit support of Gradle.
    "test"(Test::class) {
        useJUnitPlatform()
    }
}

var mainClassName: String by project.extra
mainClassName = "com.seraph.discovery.Main"

val kotlin_version: String by rootProject.extra
val kotlin_coroutines_version: String by rootProject.extra
val kotlin_argparser_version: String by rootProject.extra

dependencies {
    implementation("com.xenomachina:kotlin-argparser:$kotlin_argparser_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutines_version")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    implementation(project(":common"))
}