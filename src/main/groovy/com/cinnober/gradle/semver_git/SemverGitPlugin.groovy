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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property

interface SemverGitPluginExtension {
    /**
     * One of <code>"minor"</code> (default) or <code>"major"</code> to bump the respective SemVer part,
     * or a literal value such as <code>"3.0.0-rc2"</code> to use as the next version.
     */
    Property<String> getNextVersion()

    /**
     * Pattern for the suffix to append when the current commit is not version tagged.
     *
     * <p>
     * Example: <code>"&lt;count&gt;.g&lt;sha&gt;&lt;dirty&gt;-SNAPSHOT"</code>.
     * </p>
     *
     * <p>
     * Pattern may include the following placeholders:
     * </p>
     *
     * <ul>
     *   <li><code>&lt;count&gt;</code>: Number of commits (including this) since the last version tag.</li>
     *   <li><code>&lt;sha&gt;</code>: Abbreviated ID of the current commit.</li>
     *   <li><code>&lt;dirty&gt;</code>: The value of {@link #getDirtyMarker() dirtyMarker} if there are unstaged changes,
     *   otherwise empty. Note that untracked files do not count.</li>
     * </ul>
     *
     * <p>
     * Default: <code>"SNAPSHOT"</code>
     * </p>
     */
    Property<String> getSnapshotSuffix()

    /**
     * The value to substitute for <code>&lt;dirty&gt;</code> in {@link #getSnapshotSuffix() snapshotSuffix}
     * if there are unstaged changes.
     *
     * <p>
     * Note that this has no effect if {@link #getSnapshotSuffix() snapshotSuffix}
     * does not include a <code>&lt;dirty&gt;</code> placeholder.
     * </p>
     *
     * <p>
     * Default: <code>"-dirty"</code>
     * </p>
     */
    Property<String> getDirtyMarker()

    /**
     * Additional arguments for the <code>git describe</code> subprocess.
     *
     * <p>
     * Default: <code>"--match *[0-9].[0-9]*.[0-9]*"</code>
     * </p>
     */
    Property<String> getGitDescribeArgs()

    /**
     * A prefix that may be stripped from tag names to create a version number.
     *
     * <p>
     * For example: if your tags are named like <code>v3.0.0</code>, set this to <code>"v"</code>.
     * </p>
     *
     * <p>
     * Default: null
     * </p>
     */
    Property<String> getPrefix()
}

class DeferredVersion {
    private final File projectDir;
    private final SemverGitPluginExtension extension;
    private String version = null;

    DeferredVersion(File projectDir, SemverGitPluginExtension extension) {
        this.projectDir = projectDir;
        this.extension = extension;
    }

    private synchronized String evaluate() {
        if (version == null) {
            version = SemverGitPlugin.getGitVersion(
              this.extension.nextVersion.getOrNull(),
              this.extension.snapshotSuffix.getOrNull(),
              this.extension.dirtyMarker.getOrNull(),
              this.extension.gitDescribeArgs.getOrNull(),
              this.extension.prefix.getOrNull(),
              this.projectDir
            )
        }
        return version;
    }

    @Override
    // Gradle uses the toString() of the version object,
    // see: https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#getVersion--
    public String toString() {
        return evaluate();
    }
}

class SemverGitPlugin implements Plugin<Project> {

    def static String getGitVersion(String nextVersion, String snapshotSuffix, String dirtyMarker,
                                    String gitArgs, String semverPrefix, File projectDir = null) {
        def proc = ("git describe --exact-match " + gitArgs).execute(null, projectDir);
        proc.waitFor();
        if (proc.exitValue() == 0) {
            return checkVersion(proc.text.trim(), semverPrefix)
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
            return getNextVersion(version, nextVersion, semverPrefix, suffix);
        }
        return getNextVersion("0.0.0", nextVersion, semverPrefix, "SNAPSHOT")
    }

    def static String checkVersion(String version, String prefix) {
        return formatVersion(parseVersion(version, prefix))
    }

    def static Object[] parseVersion(String version, String prefix) {
        def pattern
        if (prefix) {
            pattern = /^$prefix?([0-9]+)\.([0-9]+)\.([0-9]+)(-([a-zA-Z0-9.-]+))?$/
        } else {
            pattern = /^([0-9]+)\.([0-9]+)\.([0-9]+)(-([a-zA-Z0-9.-]+))?$/
        }
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

    def static String getNextVersion(String version, String nextVersion, String prefix, String snapshotSuffix) {
        def v
        switch (nextVersion) {
            case "major":
                v = parseVersion(version, prefix)
                if (v[3] == null) {
                    v[0] += 1
                    v[1] = 0
                    v[2] = 0
                }
                v[3] = snapshotSuffix
                return formatVersion(v)
            case "minor":
                v = parseVersion(version, prefix)
                if (v[3] == null) {
                    v[1] += 1
                    v[2] = 0
                }
                v[3] = snapshotSuffix
                return formatVersion(v);
            case "patch":
                v = parseVersion(version, prefix)
                if (v[3] == null) {
                    v[2] += 1
                }
                v[3] = snapshotSuffix
                return formatVersion(v);
            default:
                return checkVersion(nextVersion, prefix);
        }
    }

    void apply(Project project) {
        def extension = project.extensions.create("semverGit", SemverGitPluginExtension)
        extension.nextVersion.convention("minor").finalizeValueOnRead()
        extension.snapshotSuffix.convention("SNAPSHOT").finalizeValueOnRead()
        extension.dirtyMarker.convention("-dirty").finalizeValueOnRead()
        extension.gitDescribeArgs.convention('--match *[0-9].[0-9]*.[0-9]*').finalizeValueOnRead()
        extension.prefix.convention(null).finalizeValueOnRead()

        if (project.ext.properties.containsKey("nextVersion")) {
            extension.nextVersion.set(project.ext.nextVersion)
        }
        if (project.ext.properties.containsKey("snapshotSuffix")) {
            extension.snapshotSuffix.set(project.ext.snapshotSuffix)
        }
        if (project.ext.properties.containsKey("gitDescribeArgs")) {
            extension.gitDescribeArgs.set(project.ext.gitDescribeArgs)
        }
        if (project.ext.properties.containsKey("dirtyMarker")) {
            extension.dirtyMarker.set(project.ext.dirtyMarker)
        }
        if (project.ext.properties.containsKey("semverPrefix")) {
            extension.prefix.set(project.ext.semverPrefix)
        }

        project.version = new DeferredVersion(project.projectDir, extension)

        project.tasks.register('showVersion') {
            group = 'Help'
            description = 'Show the project version'
            doLast {
                println "Version: " + project.version
            }
        }
    }
}