plugins {
    id("java")
}

group = "org.paul8711gamezz"
version = "1.2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.formdev:flatlaf:3.1.1")
    implementation ("com.google.code.gson:gson:2.11.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("GUI-Chatroom")
    manifest {
        attributes(
            "Main-Class" to "org.paul8711gamezz.GUI",
            "Implementation-Version" to project.version
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val dependencies = configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    from(dependencies)

    from(sourceSets.main.get().output)
}

// run with ./gradlew clean fatJar
// output dir: build/libs