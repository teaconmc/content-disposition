plugins {
    id("java-library")
    id("maven-publish")
}

apply {
    group = "org.teacon"
    version = "1.0.0"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(11)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    test {
        useJUnitPlatform()
    }
    processResources.get().apply {
        from(file("LICENSE")) { into("META-INF/") }
    }
    jar.get().manifest.attributes(
        mapOf(
            "Specification-Title" to "content-disposition",
            "Specification-Version" to "${project.version}",
            "Specification-Vendor" to "teacon.org",
            "Implementation-Title" to "content-disposition",
            "Implementation-Version" to "${project.version}",
            "Implementation-Vendor" to "teacon.org",
        )
    )
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        groupId = "org.teacon"
        version = "${project.version}"
        artifactId = "content-disposition"
        pom {
            name = ("ContentDisposition")
            url = "https://github.com/teaconmc/content-disposition"
            description = "Java implementation of parsing and constructing content-disposition (RFC6266) header values"
            licenses {
                license {
                    name = "GNU Lesser General Public License, Version 3.0"
                    url = "https://www.gnu.org/licenses/lgpl-3.0.txt"
                }
            }
            developers {
                developer {
                    id = "teaconmc"
                    name = "TeaConMC"
                    email = "contact@teacon.org"
                }
                developer {
                    id = "ustc-zzzz"
                    name = "Yanbing Zhao"
                    email = "zzzz.mail.ustc@gmail.com"
                }
            }
            issueManagement {
                system = "GitHub Issues"
                url = "https://github.com/teaconmc/content-disposition/issues"
            }
            scm {
                url = "https://github.com/teacon/content-disposition"
                connection = "scm:git:git://github.com/teacon/content-disposition.git"
                developerConnection = "scm:git:ssh://github.com/teacon/content-disposition.git"
            }
        }
        artifact(tasks.jar)
    }
}
