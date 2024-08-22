plugins {
    id("java")
}

apply {
    group = "org.teacon"
    version = "1.0.0"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11));
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
