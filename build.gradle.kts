plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "me.simeonya"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:+")
    implementation("com.sparkjava:spark-core:+")
    implementation("org.projectlombok:lombok:+")
    implementation("com.google.code.gson:gson:+")
    implementation("org.google.code.mp3spi:mp3spi:+")
    implementation("com.fazecast:jSerialComm:+")
    implementation("com.github.kwhat:jnativehook:+")
}

tasks.shadowJar {
    archiveBaseName.set("cs2-pishock")
    archiveVersion.set(version.toString())
    archiveClassifier.set("")
    mergeServiceFiles()

    manifest {
        attributes["Main-Class"] = "me.simeonya.Main"
    }
}