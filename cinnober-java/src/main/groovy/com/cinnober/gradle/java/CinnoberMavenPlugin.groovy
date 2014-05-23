package com.cinnober.gradle.java

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.Compile

class CinnoberMavenPlugin implements Plugin<Project> {

    def static boolean isAlreadyReleased(Project project, String repositoryUrl) {
        def group = project.group;
        def name = project.name;
        def version = project.version;
        if (version.endsWith("-SNAPSHOT")){
            return false // not a release
        }
        def jarName = name + '-' + version + '.jar'
        def artifactPath = group.replace(".", "/") + "/" + name + "/" + version + "/" + jarName
        def artifactUrl = repositoryUrl + artifactPath
        if (urlExists(artifactUrl)) {
            project.logger.warn("Cannot upload already released artifact " + group + ":" + name + ":" + version)
            return true // already exists
        }
        return false // release does not exist
    }
    def static boolean urlExists(String url) {
        try {
            def connection = (HttpURLConnection) new URL(url).openConnection()
            def timeoutInMillis = 10000
            connection.setConnectTimeout(timeoutInMillis)
            connection.setReadTimeout(timeoutInMillis)
            connection.setRequestMethod("HEAD")
            def responseCode = connection.getResponseCode()
            return (200 <= responseCode || responseCode >= 399)
        } catch (IOException ignored) {
            return false
        }
    }

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
        def releaseRepositoryUrl = "http://nexus.cinnober.com/nexus/content/repositories/releases/";
        def snapshotRepositoryUrl = "http://nexus.cinnober.com/nexus/content/repositories/snapshots/";

        project.uploadArchives {
            onlyIf {
                !isAlreadyReleased(project, releaseRepositoryUrl)
            }
            repositories {
                mavenDeployer {
                    repository(url: releaseRepositoryUrl) {
                        authentication(userName: mavenUser, password: mavenPassword)
                    }
                    snapshotRepository(url: snapshotRepositoryUrl) {
                        authentication(userName: mavenUser, password: mavenPassword)
                    }
                }
            }
        }
    }
}