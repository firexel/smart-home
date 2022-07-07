plugins {
    kotlin("jvm")
}

val kotlin_version: String by rootProject.extra
val kotlin_coroutines_version: String by rootProject.extra
val ktor_version: String by rootProject.extra

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutines_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}