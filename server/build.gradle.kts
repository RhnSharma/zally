import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.21"

    // The buildscript is also kotlin, so we apply at the root level
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion

    // We need to declare these here since we are configuring them for
    // subprojects from the top level.
    jacoco
    `maven-publish`
    signing
    id("com.github.ben-manes.versions") version "0.20.0"
    id("org.jetbrains.dokka") version "0.10.0" apply false

    // We apply this so that ktlint can format the top level buildscript
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("org.jlleitschuh.gradle.ktlint-idea") version "9.2.1"
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

subprojects {

    group = "org.zalando"

    apply(plugin = "kotlin")
    apply(plugin = "kotlin-kapt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")
    apply(plugin = "signing")

    kapt {
        includeCompileClasspath = false
    }

    tasks.withType(KotlinCompile::class.java).all {
        kotlinOptions.jvmTarget = "1.8"
    }

    tasks.withType(DokkaTask::class.java).all {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/dokka"
        configuration {
            reportUndocumented = false
        }
    }

    tasks.register("javadocJar", Jar::class) {
        dependsOn(tasks["dokka"])
        archiveClassifier.set("javadoc")
        from(tasks["dokka"])
    }

    tasks.register("sourcesJar", Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    artifacts {
        add("archives", tasks["javadocJar"])
        add("archives", tasks["sourcesJar"])
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                pom {
                    description.set("OpenAPI linter service")
                    url.set("https://github.com/zalando/zally")
                    name.set("OpenAPI linter")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            name.set("Felix Müller")
                            email.set("felix.mueller.berlin@googlemail.com")
                        }
                        developer {
                            name.set("Mikhail Chernykh")
                            email.set("netmisch@gmail.com")
                        }
                        developer {
                            name.set("Maxim Tschumak")
                            email.set("maxim.tschumak@gmail.com")
                        }
                        developer {
                            name.set("Rui Araujo")
                            email.set("rui.araujo@zalando.de")
                        }
                        developer {
                            name.set("Tronje Krop")
                            email.set("tronje.krop@zalando.de")
                        }
                        developer {
                            name.set("Gregor Zeitlinger")
                            email.set("gregor.zeitlinger@zalando.de")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/zalando/zally.git")
                        developerConnection.set("scm:git:ssh://github.com:zalando/zally.git")
                        url.set("https://github.com/zalando/zally/tree/master")
                    }
                }
            }
        }

        repositories {
            maven {
                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                credentials {
                    // defined in travis project settings or in $HOME/.gradle/gradle.properties
                    username = System.getenv("OSSRH_JIRA_USERNAME")
                    password = System.getenv("OSSRH_JIRA_PASSWORD")
                }
            }
        }
    }

    signing {
        sign(publishing.publications["mavenJava"])
    }

    configurations.all {
        resolutionStrategy {
            // 1.2.10 disallows jar:file: resources, hopefully fixed in 1.2.14+
            force("com.github.java-json-tools:json-schema-core:bf09fe87139ac1fde0755194b59130f3b2d63e3a")
        }
    }

    dependencies {
        compile("org.jetbrains.kotlin:kotlin-stdlib")

        // We define this here so all subprojects use the same version of jackson
        compile("com.fasterxml.jackson.module:jackson-module-parameter-names:2.10.2")
        compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.2")
        compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.2")
        compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.2")
        compile("org.yaml:snakeyaml:1.24")

        testCompile("com.jayway.jsonpath:json-path-assert:2.4.0")
        testCompile("org.mockito:mockito-core:2.23.4")
    }

    jacoco {
        toolVersion = "0.8.2"
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
        }
    }

    tasks.jar {
        archiveBaseName.set(project.name)
        archiveVersion.set(version)
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "5.3.1"
}
