# semver-git #
[![Build Status](https://travis-ci.org/cinnober/semver-git.svg?branch=master)](https://travis-ci.org/cinnober/semver-git)

The gradle plugin 'semver-git' sets the `project.version` based on _annotated_ git tags.
Version numbers must follow [Semantic Versioning 2.0.0](http://semver.org/spec/v2.0.0.html), with the syntax _major.minor.patch_.

## Usage ##

In your `build.gradle` file:

    buildscript {
        repositories {
            mavenCentral()
        }
        dependencies {
            classpath group: 'com.cinnober.gradle', name: 'semver-git', version: '2.2.0'
        }
    }
    // optionally: ext.nextVersion = "major", "minor" (default), "patch" or e.g. "3.0.0-rc2"
    // optionally: ext.snapshotSuffix = "SNAPSHOT" (default) or a pattern, e.g. "<count>.g<sha><dirty>-SNAPSHOT"
    // optionally: ext.dirtyMarker = "-dirty" (default) replaces <dirty> in snapshotSuffix
    // optionally: ext.gitDescribeArgs = '--match *[0-9].[0-9]*.[0-9]*' (default) or other arguments for git describe.
    // optionally: ext.prefix = "" (default) "prefix-" ignore prefix in tag "prefix-1.0.0-rc2"
    apply plugin: 'com.cinnober.gradle.semver-git'
    
Note: Use this method instead of the newer `plugins` method if you want to change `nextVersion` and `snapshotSuffix`.

Then everything should just work. To create a release, create an
annotated git tag, e.g.:

    git tag -a 1.0.0 -m "New release"
    git push --tags

When standing on an annotated tag commit, then version is simply the
same as the tag (1.0.0 in this example).  After a few commits `git
describe` will show something like `1.0.0-5-g5242341` in which case
the version is the snapshot of the next version. In this example the
next version is minor, and the version will be `1.1.0-SNAPSHOT`.

The snapshot suffix pattern can contain `<count>` and `<sha>` which
will be replaced with the commit count (`5`) and the commit sha
(`5242341`) respectively.
For example the snapshot suffix `<count>.g<sha>` will generate the
version `1.1.0-5.g5242341`, i.e. a non-snapshot pre-release.
For example the snapshot suffix pattern is `<count>.g<sha>-SNAPSHOT`
which will generate the version `1.1.0-5.g5242341-SNAPSHOT`, which is
a unique snapshot version.

### Release ###

Versions are stored as annotated tags in git. [Semantic versioning](http://semver.org) is used.

To create a new release, e.g. 1.2.3:

    git tag -a 1.2.3 -m "New release"
    git push --tags

If changes are made after version 1.2.3 then the version number be '1.3.0-SNAPSHOT' (default a minor change).

To upload the archives to the Maven Central (through the OSSRH), run:

    gradle clean build uploadArchives

To upload the plugin to the Gradle Plugin Portal, run:

    gradle clean build publishPlugins

Note that credentials are required for uploads. They should be placed in e.g. your
~/.gradle/gradle.properties for `uploadArchives`.
See [gradle.properties](gradle.properties) for more information.
