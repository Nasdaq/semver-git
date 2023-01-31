package com.cinnober.gradle.semver_git

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

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
        setupPluginWithClasspath(prefix: 'v')
        init()
        commit()
        tag("v1.0.0")
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.0.0")
    }

    def "Prefixed tag with commit"() {
        when:
        setupPluginWithClasspath(prefix: 'v')
        init()
        commit()
        tag("v1.0.0")
        commit()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT")
    }

    def "Dirty tag with default config"() {
        when:
        setupPluginWithClasspath()
        init()
        commit()
        tag("1.0.0")
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.0.0")
    }

    def "Dirty tag with snapshotSuffix with <dirty>"() {
        when:
        setupPluginWithClasspath(snapshotSuffix: "SNAPSHOT<dirty>")
        init()
        commit()
        tag("1.0.0")
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.0.0")
    }

    def "Dirty untagged commit with default config"() {
        when:
        setupPluginWithClasspath()
        init()
        commit()
        tag("1.0.0")
        commit()
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT")
    }

    def "Dirty untagged commit with snapshotSuffix with <dirty>"() {
        when:
        setupPluginWithClasspath(snapshotSuffix: "SNAPSHOT<dirty>")
        init()
        commit()
        tag("1.0.0")
        commit()
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT-dirty")
    }

    def "Custom nextVersion"() {
        when:
        setupPluginWithClasspath(nextVersion: "major")
        init()
        commit()
        tag("1.0.0")
        commit()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 2.0.0-SNAPSHOT")
    }

    def "Custom snapshotSuffix"() {
        when:
        setupPluginWithClasspath(snapshotSuffix: "volatile")
        init()
        commit()
        tag("1.0.0")
        commit()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-volatile")
    }

    def "Custom dirtyMarker"() {
        when:
        setupPluginWithClasspath(snapshotSuffix: "SNAPSHOT<dirty>", dirtyMarker: "_MODIFIED")
        init()
        commit()
        tag("1.0.0")
        commit()
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT_MODIFIED")
    }

    def "Custom gitDescribeArgs"() {
        when:
        setupPluginWithClasspath(gitDescribeArgs: "--long")
        init()
        String shortCommit = commit()
        tag("1.0.0")
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.0.0-0-g${shortCommit}")
    }

    def git(String argument) {
        def proc = ("git " + argument).execute(null, projectDir)
        proc.waitFor()
        return proc
    }

    def init() {
        git("init")
        git("add .")

        // Disable GPG signing in the test directory, otherwise user might be prompted for
        // manual intervention if they have this config set to true
        return git("config commit.gpgSign false")
    }

    def commit() {
        git("commit --allow-empty -m empty")
        return git("rev-parse --short HEAD").text.trim()
    }

    def tag(String name) {
        return git("tag -a $name -m tag")
    }

    def touchBuildFile() {
        build << "\n// foo\n"
    }

    def setupPluginWithClasspath(Map args = [:]) {
        build << """
plugins {
    id "com.cinnober.gradle.semver-git" apply false
}

${args.nextVersion != null ? "ext.nextVersion = '${args.nextVersion}'" : ""}
${args.snapshotSuffix != null ? "ext.snapshotSuffix = '${args.snapshotSuffix}'" : ""}
${args.dirtyMarker != null ? "ext.dirtyMarker = '${args.dirtyMarker}'" : ""}
${args.gitDescribeArgs != null ? "ext.gitDescribeArgs = '${args.gitDescribeArgs}'" : ""}
${args.prefix != null ? "ext.semverPrefix = '${args.prefix}'" : ""}

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
