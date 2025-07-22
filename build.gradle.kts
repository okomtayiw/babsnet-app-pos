plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "3.0.1"
}

group = "com.babsnet"
version = "1.0.0"


repositories {
    mavenCentral()
}

val junitVersion = "5.10.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("com.babsnet.posapp")
    mainClass.set("com.babsnet.posapp.Main")
}

javafx {
    version = "21.0.7"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("com.github.librepdf:openpdf:1.3.30")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Copy>("prepareInstallerResources") {
    from("config.properties")
    into("installer-resources")
}

tasks.named("jlink") {
    dependsOn("prepareInstallerResources")
}


jlink {
    imageZip.set(layout.buildDirectory.file("distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "POSApp"
    }
    jpackage {
        installerType = "msi"
        appVersion = "1.0.0"
        resourceDir = file("installer-resources")
        installerOptions = listOf(
            "--win-dir-chooser", // agar muncul dialog pilih folder install
            "--win-menu",        // agar ada shortcut di Start Menu
            "--win-shortcut",    // agar ada shortcut di desktop
            "--vendor", "Babsnet",
            "--verbose",
            "--icon", "src/main/resources/icon_babs.ico" // custom icon, opsional
        )
    }
}

