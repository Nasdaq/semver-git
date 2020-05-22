/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 The Semver-Git Authors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cinnober.gradle.semver_git

import org.gradle.api.Project
import org.gradle.api.Plugin

class SemverGitPlugin implements Plugin<Project> {

    def static String getGitVersion(String nextVersion, String snapshotSuffix, String dirtyMarker, String gitArgs, File projectDir = null) {
        def proc = ("git describe --exact-match " + gitArgs).execute(null, projectDir);
        proc.waitFor();
        if (proc.exitValue() == 0) {
            return checkVersion(proc.text.trim());
        }
        proc = ("git describe --tags --dirty --abbrev=7 " + gitArgs).execute(null, projectDir);
        proc.waitFor();
        if (proc.exitValue() == 0) {
            def describe = proc.text.trim()
            def dirty =  describe.endsWith('-dirty')
            if (dirty) {
                describe = describe.substring(0, describe.length() - 6)
            }
            def version = (describe =~ /-[0-9]+-g[0-9a-f]+$/).replaceFirst("")
            def suffixMatcher = (describe =~ /-([0-9]+)-g([0-9a-f]+)$/)
            def count = suffixMatcher[0][1];
            def sha = suffixMatcher[0][2];
            def suffix = snapshotSuffix;
            suffix = suffix.replaceAll("<count>", count);
            suffix = suffix.replaceAll("<sha>", sha);
            suffix = suffix.replaceAll("<dirty>", dirty ? dirtyMarker : '');
            return getNextVersion(version, nextVersion, suffix);
        }
        return getNextVersion("0.0.0", nextVersion, "SNAPSHOT")
    }

    def static String checkVersion(String version) {
        return formatVersion(parseVersion(version));
    }

    def static Object[] parseVersion(String version) {
        def pattern = /^(?:refs\/tags\/)?([0-9]+)\.([0-9]+)\.([0-9]+)(-([a-zA-Z0-9.-]+))?$/
        def matcher = version =~ pattern
        def arr = matcher.collect { it }[0]
        if (arr == null) {
            throw new IllegalArgumentException("Not a valid version: '" + version + "'")
        }
        return [arr[1].toInteger(), arr[2].toInteger(), arr[3].toInteger(), arr[5]]
    }

    def static String formatVersion(version) {
        return "" + version[0] + "." + version[1] + "." + version[2] + (version[3] != null ? "-" + version[3] : "");
    }

    def static String getNextVersion(String version, String nextVersion, String snapshotSuffix) {
        def v
        switch (nextVersion) {
            case "major":
                v = parseVersion(version)
                if (v[3] == null) {
                    v[0] += 1
                    v[1] = 0
                    v[2] = 0
                }
                v[3] = snapshotSuffix
                return formatVersion(v)
            case "minor":
                v = parseVersion(version)
                if (v[3] == null) {
                    v[1] += 1
                    v[2] = 0
                }
                v[3] = snapshotSuffix
                return formatVersion(v);
            case "patch":
                v = parseVersion(version)
                if (v[3] == null) {
                    v[2] += 1
                }
                v[3] = snapshotSuffix
                return formatVersion(v);
            default:
                return checkVersion(nextVersion);
        }
    }

    void apply(Project project) {
        def nextVersion = "minor"
        def snapshotSuffix = "SNAPSHOT"
        def dirtyMarker = "-dirty"
        def gitDescribeArgs = '--match *[0-9].[0-9]*.[0-9]*'
        if (project.ext.properties.containsKey("nextVersion")) {
            nextVersion = project.ext.nextVersion
        }
        if (project.ext.properties.containsKey("snapshotSuffix")) {
            snapshotSuffix = project.ext.snapshotSuffix
        }
        if (project.ext.properties.containsKey("gitDescribeArgs")) {
            gitDescribeArgs = project.ext.gitDescribeArgs
        }
        if (project.ext.properties.containsKey("dirtyMarker")) {
            dirtyMarker = project.ext.dirtyMarker
        }
        project.version = getGitVersion(nextVersion, snapshotSuffix, dirtyMarker, gitDescribeArgs, project.projectDir)
        project.task('showVersion') {
            group = 'Help'
            description = 'Show the project version'
        }
        project.tasks.showVersion.doLast {
            println "Version: " + project.version
        }
    }
}
