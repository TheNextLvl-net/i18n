plugins {
    id("java")
    id("java-library")
    id("maven-publish")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
    withJavadocJar()
}

tasks.compileJava {
    options.release.set(21)
}

group = "net.thenextlvl"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnlyApi("net.kyori:adventure-text-logger-slf4j:4.26.2-SNAPSHOT")
    compileOnlyApi("net.kyori:adventure-text-minimessage:5.0.0-SNAPSHOT")

    testCompileOnly("org.jspecify:jspecify:1.0.0")
    testImplementation("net.kyori:adventure-text-logger-slf4j:4.26.2-SNAPSHOT")
    testImplementation("net.kyori:adventure-text-minimessage:5.0.0-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.slf4j:slf4j-simple:2.1.0-alpha1")
    testImplementation(platform("org.junit:junit-bom:6.1.0-SNAPSHOT"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showCauses = true
        showExceptions = true
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        pom.scm {
            val repository = "TheNextLvl-net/i18n"
            url.set("https://github.com/$repository")
            connection.set("scm:git:git://github.com/$repository.git")
            developerConnection.set("scm:git:ssh://github.com/$repository.git")
        }
    }
    repositories.maven {
        val channel = if ((version as String).contains("-pre")) "snapshots" else "releases"
        url = uri("https://repo.thenextlvl.net/$channel")
        credentials {
            username = System.getenv("REPOSITORY_USER")
            password = System.getenv("REPOSITORY_TOKEN")
        }
    }
}