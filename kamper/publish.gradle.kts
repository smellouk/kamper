plugins.apply(MavenPublishPlugin::class)

group = "com.smellouk.kamper"
version = rootProject.extra["LIB_VERSION_NAME"].toString()

afterEvaluate {
    configure<PublishingExtension> {
        repositories {
            if (System.getenv().containsKey("CI")) {
                maven {
                    name = "GithubPackages"
                    url = uri("https://maven.pkg.github.com/smellouk/kamper")
                    credentials {
                        username = rootProject.extra["KAMPER_GH_USER"].toString()
                        password = rootProject.extra["KAMPER_GH_PAT"].toString()
                    }
                }
            }

            maven {
                name = "LocalMaven"
                url = uri("file://$rootDir/build/maven")
            }
        }

        publications.withType<MavenPublication> {
            pom {
                val projectName = if (projectDir.parent.contains("modules")) {
                    "${project.name}-module"
                } else {
                    project.name
                }
                artifactId = projectName

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                name.set("kamper:$projectName")
                description.set("Kamper is small KMM/KMP library that provides performance monitoring for your app.")
                url.set("https://github.com/smellouk/kamper")

                issueManagement {
                    system.set("Github")
                    url.set("https://github.com/smellouk/kamper/issues")
                }

                scm {
                    connection.set("https://github.com/smellouk/kamper.git")
                    url.set("https://github.com/smellouk/kamper")
                }
            }
        }
    }
}
