plugins {
    java
    checkstyle
}

group = "net.thevalorsmp"
version = providers.environmentVariable("VALORSMP_VERSION").orElse("0.1.0-SNAPSHOT").get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val paperApiVersion = "1.21.4-R0.1-SNAPSHOT"
val hikariVersion = "6.2.1"
val sqliteVersion = "3.49.1.0"

// Integration tests (MockBukkit + real SQLite) live in their own source set so `test` stays fast.
val integrationTest: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("org.jetbrains:annotations:26.0.2")

    // Provided at runtime by Paper's plugin.yml `libraries` loader, so they stay out of the plugin JAR.
    compileOnly("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("org.xerial:sqlite-jdbc:$sqliteVersion")

    testImplementation("com.zaxxer:HikariCP:$hikariVersion")
    testImplementation("org.xerial:sqlite-jdbc:$sqliteVersion")

    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("org.jetbrains:annotations:26.0.2")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    integrationTestImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.35.0")
}

checkstyle {
    toolVersion = "10.21.4"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.named<Jar>("jar") {
    archiveBaseName = "the-valor-smp"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
                "version" to project.version,
                "hikariVersion" to hikariVersion,
                "sqliteVersion" to sqliteVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs MockBukkit integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(integrationTestTask)
}
