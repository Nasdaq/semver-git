package com.cinnober.gradle.semver_git

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

@Ignore
class SemverGitPluginFunctionalTest extends Specification {
    @Shared
    Boolean slowIo = false

    @TempDir
    Path baseDir
    File projectDir
    File build
    File settings

    def setupSpec() {
        try {
            if (InetAddress.getLocalHost().getHostName().startsWith("SE40MAC")) {
                slowIo = true
            }
        } catch (Exception ignored) {}
    }

    def setup() {
        projectDir = Files.createTempDirectory(baseDir, "test").toFile()
        build = new File(projectDir, "build.gradle")
        settings = new File(projectDir, "settings.gradle")
        settings << "rootProject.name = 'semver-test'"
    }

    def "Simple build"() {
        when:
        setupPluginWithClasspath()
        init()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 0.1.0-SNAPSHOT")
    }

    def "Simple build with commit"() {
        when:
        setupPluginWithClasspath()
        init()
        commit()
        tag("1.0.0")
        commit()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT")
    }

    def "Prefixed tag"() {
        when:
        setupPluginWithClasspath('v')
        init()
        commit()
        tag("v1.0.0")
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.0.0")
    }

    def "Prefixed tag with commit"() {
        when:
        setupPluginWithClasspath('v')
        init()
        commit()
        tag("v1.0.0")
        commit()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT")
    }

    def git(String argument) {
        return ("git " + argument).execute(null, projectDir)
    }

    def init() {
        return git("init")
    }

    def commit() {
        return git("commit --allow-empty -m empty")
    }

    def tag(String name) {
        return git("tag -a $name -m tag")
    }

    def setupPluginWithClasspath(
            String prefix = null
    ) {
        build.write """
plugins {
    id "com.cinnober.gradle.semver-git" apply false
}
// optionally: ext.nextVersion = "major", "minor" (default), "patch" or e.g. "3.0.0-rc2"
// optionally: ext.snapshotSuffix = "SNAPSHOT" (default) or a pattern, e.g. "<count>.g<sha><dirty>-SNAPSHOT"
// optionally: ext.dirtyMarker = "-dirty" (default) replaces <dirty> in snapshotSuffix
// optionally: ext.gitDescribeArgs = '--match *[0-9].[0-9]*.[0-9]*' (default) or other arguments for git describe.
"""
        if (prefix) {
            build << "ext.semverPrefix = '$prefix'"
        }
        build << """
apply plugin: 'com.cinnober.gradle.semver-git'
"""
    }

    BuildResult gradleBuild(String... arguments) {
        if (slowIo) {
            sleep(1000)
        }
        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(arguments)
                .withPluginClasspath()
                .build()
    }
}
