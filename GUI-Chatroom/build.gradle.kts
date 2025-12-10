plugins {
    id("java")
}

group = "org.paul8711gamezz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.formdev:flatlaf:3.1.1")
}

tasks.test {
    useJUnitPlatform()
}
