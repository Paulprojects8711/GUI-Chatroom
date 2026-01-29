plugins {
    id("java")
    id("application")
}

group = "org.paul8711gamezz"
version = "1.6"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.formdev:flatlaf:3.1.1")
    implementation("com.google.code.gson:gson:2.11.0")
}

application {
    mainClass.set("org.paul8711gamezz.GUI")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("GUI-Chatroom")

    // check for property "release"
    val isRelease = project.findProperty("release") == "true"

    archiveVersion.set(if (isRelease) project.version.toString() else "")

    manifest {
        attributes(
            "Main-Class" to "org.paul8711gamezz.GUI",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // include dependencies
    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    )

    // include compiled classes
    from(sourceSets.main.get().output)
}

// run with ./gradlew clean jar -Prelease=true
// the -Prelease=true sets a variable to true so gradle knows to add the version number to the file name
// this is only used because of the run config in intellij
// output dir: build/libs