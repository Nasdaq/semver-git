package com.cinnober.gradle.java

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.Compile

class CinnoberMavenPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.apply plugin: 'maven'
        
        project.repositories {
            mavenLocal()
            maven {
                url "http://nexus.cinnober.com/nexus/content/groups/public/"
            }
        }
        def mavenUser = project.hasProperty('mavenUser') ? project.property('mavenUser') : null;
        def mavenPassword = project.hasProperty('mavenPassword') ? project.property('mavenPassword') : null;
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    repository(url: "http://nexus.cinnober.com/nexus/content/repositories/releases/") {
                        authentication(userName: mavenUser, password: mavenPassword)
                    }
                    snapshotRepository(url: "http://nexus.cinnober.com/nexus/content/repositories/snapshots/") {
                        authentication(userName: mavenUser, password: mavenPassword)
                    }
                }
            }
        }
    }
}