import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.withType

/**
 * Convention plugin that wires Maven publishing for Kamper library modules.
 *
 * Applied as: id("kamper.publish") in publishing modules' plugins{} block.
 *
 * This plugin is the verbatim migration of kamper/publish.gradle.kts (58 lines) into a
 * Plugin<Project> Kotlin class. Behavior preserved exactly — repository URLs, POM fields,
 * artifactId-rewrite rule for modules under kamper/modules/, credential sourcing from
 * rootProject.extra are all identical.
 *
 * Critical timing constraint (Pitfall 5 / Critical Finding #7):
 *   - rootProject.extra["LIB_VERSION_NAME"] is set in the root build.gradle.kts extra.apply{}
 *     block during root project configuration. That happens AFTER this plugin's apply()
 *     runs on a per-module basis. Reading rootProject.extra in apply() directly would find
 *     it empty.
 *   - All publishing configuration MUST therefore be wrapped in project.afterEvaluate {}.
 *     This is a 1-to-1 match with the existing publish.gradle.kts which uses afterEvaluate {}.
 *
 * Credential loading (KAMPER_GH_USER / KAMPER_GH_PAT) is NOT done here. The root
 * build.gradle.kts continues to load credentials from credentials.properties or env vars
 * and stash them in rootProject.extra. This plugin only READS rootProject.extra.
 */
class KamperPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // maven-publish is a core Gradle plugin — no artifact needed
        project.pluginManager.apply("maven-publish")

        project.group = "com.smellouk.kamper"

        // Wrap ALL configuration in afterEvaluate so rootProject.extra is populated
        project.afterEvaluate {
            project.version = project.rootProject.extra["LIB_VERSION_NAME"].toString()

            project.configure<PublishingExtension> {
                repositories {
                    if (System.getenv().containsKey("CI")) {
                        maven {
                            name = "GithubPackages"
                            url = project.uri("https://maven.pkg.github.com/smellouk/kamper")
                            credentials {
                                username = project.rootProject.extra["KAMPER_GH_USER"].toString()
                                password = project.rootProject.extra["KAMPER_GH_PAT"].toString()
                            }
                        }
                    }

                    maven {
                        name = "LocalMaven"
                        url = project.uri("file://${project.rootDir}/build/maven")
                    }
                }

                publications.withType<MavenPublication>().configureEach {
                    val projectName = if (project.projectDir.parent.contains("modules")) {
                        "${project.name}-module"
                    } else {
                        project.name
                    }
                    artifactId = projectName

                    pom {
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        name.set("kamper:$projectName")
                        description.set(
                            "Kamper is small KMM/KMP library that provides performance monitoring for your app."
                        )
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
    }
}
