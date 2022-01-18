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

import spock.lang.Specification


class SemverGitPluginTest extends Specification {
    def "parseVersion"(String input, expected) {
        expect:
        def actual = SemverGitPlugin.parseVersion(input, null)
        expected == actual

        where:
        input           || expected
        "1.0.0"         || [1,0,0,null]
        "1.2.3"         || [1,2,3,null]
        "1.2.3-beta"    || [1,2,3,"beta"]
        "1.2.3-SNAPSHOT"|| [1,2,3,"SNAPSHOT"]
        "12.34.56-rc78" || [12,34,56,"rc78"]
    }

    def "parseVersion prefixed"(String input, prefix, expected) {
        expect:
        def actual = SemverGitPlugin.parseVersion(input, prefix)
        expected == actual

        where:
        input            | prefix || expected
        "v1.0.0"         |  'v'   || [1,0,0,null]
        "v1.2.3"         |  'v'   || [1,2,3,null]
        "v1.2.3-beta"    |  'v'   || [1,2,3,"beta"]
        "v1.2.3-SNAPSHOT"|  'v'   || [1,2,3,"SNAPSHOT"]
        "v12.34.56-rc78" |  'v'   || [12,34,56,"rc78"]
    }

    def "formatVersion"(input, expected) {
        expect:
        def actual = SemverGitPlugin.formatVersion(input)
        expected == actual

        where:
        input               || expected
        [1,0,0,null]        || "1.0.0"
        [1,2,3,null]        || "1.2.3"
        [1,2,3,"beta"]      || "1.2.3-beta"
        [1,2,3,"SNAPSHOT"]  || "1.2.3-SNAPSHOT"
        [12,34,56,"rc78"]   || "12.34.56-rc78"
    }

    def "testFail"(String input) {
        when:
        SemverGitPlugin.parseVersion(input, null)

        then:
        thrown(IllegalArgumentException)

        where:
        input << ["a.b.c", "1", "a1.2.3", "1.2.3a"]
    }

    def "testNextVersion"(String expected, String version, String nextVersion) {
        expect:
        def actual = SemverGitPlugin.getNextVersion(version, nextVersion, null, "SNAPSHOT")
        expected == actual

        where:
        version      | nextVersion  || expected
        "1.2.3"      | "patch"      || "1.2.4-SNAPSHOT"
        "1.2.3-beta" | "patch"      || "1.2.3-SNAPSHOT"
        "1.2.3"      | "minor"      || "1.3.0-SNAPSHOT"
        "1.2.3-beta" | "minor"      || "1.2.3-SNAPSHOT"
        "1.2.0-beta" | "minor"      || "1.2.0-SNAPSHOT"
        "1.2.3"      | "major"      || "2.0.0-SNAPSHOT"
        "1.2.3-beta" | "major"      || "1.2.3-SNAPSHOT"
        "1.0.0-beta" | "major"      || "1.0.0-SNAPSHOT"
    }

}