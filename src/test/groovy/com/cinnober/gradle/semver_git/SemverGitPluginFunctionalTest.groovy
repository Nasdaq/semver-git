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
        setupPluginWithClasspath(config)
        init()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 0.1.0-SNAPSHOT")

        where:
        config << configs()
    }

    def "Simple build with commit"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("1.0.0")
        commit()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT")

        where:
        config << configs()
    }

    def "Prefixed tag"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("v1.0.0")
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.0.0")

        where:
        config << configs(prefix: 'v')
    }

    def "Prefixed tag with commit"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("v1.0.0")
        commit()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT")

        where:
        config << configs(prefix: 'v')
    }

    def "Dirty tag with default config"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("1.0.0")
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.0.0")

        where:
        config << configs()
    }

    def "Dirty tag with snapshotSuffix with <dirty>"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("1.0.0")
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.0.0")

        where:
        config << configs(snapshotSuffix: "SNAPSHOT<dirty>")
    }

    def "Dirty untagged commit with default config"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("1.0.0")
        commit()
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT")

        where:
        config << configs()
    }

    def "Dirty untagged commit with snapshotSuffix with <dirty>"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("1.0.0")
        commit()
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT-dirty")

        where:
        config << configs(snapshotSuffix: "SNAPSHOT<dirty>")
    }

    def "Custom nextVersion"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("1.0.0")
        commit()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 2.0.0-SNAPSHOT")

        where:
        config << configs(nextVersion: "major")
    }

    def "Custom snapshotSuffix"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("1.0.0")
        commit()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-volatile")

        where:
        config << configs(snapshotSuffix: "volatile")
    }

    def "Custom dirtyMarker"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("1.0.0")
        commit()
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.1.0-SNAPSHOT_MODIFIED")

        where:
        config << configs(snapshotSuffix: "SNAPSHOT<dirty>", dirtyMarker: "_MODIFIED")
    }

    def "Custom gitDescribeArgs"() {
        when:
        setupPluginWithClasspath(config)
        init()
        String shortCommit = commit()
        tag("1.0.0")
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 1.0.0-0-g${shortCommit}")

        where:
        config << configs(gitDescribeArgs: "--long")
    }

    def "semverGit {} block overrides project.ext settings"() {
        when:
        setupPluginWithClasspath(config)
        init()
        commit()
        tag("new-version-1.0.0")
        commit()
        touchBuildFile()
        BuildResult result = gradleBuild("showVersion")

        then:
        result.getOutput().contains("Version: 2.0.0-new_-NEW_DIRTYsnapshot")

        where:
        config = """
plugins {
    id "com.cinnober.gradle.semver-git" apply false
}

ext.nextVersion = 'minor'
ext.snapshotSuffix = 'LEGACYSNAPSHOT'
ext.dirtyMarker = '-legacydirty'
ext.gitDescribeArgs = '--match *[0-9].abc.[0-9]*.def.[0-9]*'
ext.semverPrefix = 'legacy-version-'

apply plugin: 'com.cinnober.gradle.semver-git'

semverGit {
    nextVersion = 'major'
    snapshotSuffix = 'new_<dirty>snapshot'
    dirtyMarker = '-NEW_DIRTY'
    gitDescribeArgs = '--match *[0-9].[0-9]*.[0-9]* --long'
    prefix = 'new-version-'
}
"""
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

    def setupPluginWithClasspath(String config) {
        build << config
    }

    List<String> configs(Map args = [:]) {
        [legacyConfig(args), extensionConfig(args)]
    }

    String legacyConfig(Map args) {
        return """
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

    String extensionConfig(Map args) {
        return """
plugins {
    id "com.cinnober.gradle.semver-git"
}

semverGit {
    ${args.nextVersion != null ? "nextVersion = '${args.nextVersion}'" : ""}
    ${args.snapshotSuffix != null ? "snapshotSuffix = '${args.snapshotSuffix}'" : ""}
    ${args.dirtyMarker != null ? "dirtyMarker = '${args.dirtyMarker}'" : ""}
    ${args.gitDescribeArgs != null ? "gitDescribeArgs = '${args.gitDescribeArgs}'" : ""}
    ${args.prefix != null ? "prefix = '${args.prefix}'" : ""}
}
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
