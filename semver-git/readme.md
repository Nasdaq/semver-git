# semver-git #

The gradle plugin 'semver-git' sets the `project.version` based on _annotated_ git tags.
Version numbers must follow [Semantic Versioning 2.0.0](http://semver.org/spec/v2.0.0.html), with the syntax _major.minor.patch_.

## Usage ##

In your `build.gradle` file:

    buildscript {
        repositories {
            maven {
                url uri("http://nexus.cinnober.com/nexus/content/repositories/buildtest_snapshots/")
            }
            maven {
                url uri("http://nexus.cinnober.com/nexus/content/repositories/buildtest_releases/")
            }
        }
        dependencies {
            classpath group: 'com.cinnober.gradle', name: 'semver-git', version: '0.1.0-SNAPSHOT'
        }
    }
    // optionally: ext.nextVersion = "major", "minor" (default), "patch" or e.g. "3.0.0-rc2"
    apply plugin 'semver-git'

Then everything should just work. To create a release, create an annotated git tag, e.g.:

    git tag -a 1.0.0 -m "New release"
    git push --tags

When standing on an annotated tag commit, then version is simply the same as the tag (1.0.0 in this example).
After a few commits `git describe` will show something like `1.0.0-5-g524234` in which case the version
is the snapshot of the next version. In this example the next version is minor, and the version will be
`1.1.0-SNAPSHOT`.
