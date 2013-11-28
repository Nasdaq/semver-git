# cinnober-java #

The gradle plugin 'cinnober-java' applies the plugins 'java', 'maven' and 'eclipse'.
The Cinnober nexus repositories are also added, both for dependencies and publishing.
Javadoc and sources jar artifacts are added to the configuration.

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
            classpath group: 'com.cinnober.gradle', name: 'cinnober-java', version: '0.1.0-SNAPSHOT'
        }
    }
    apply plugin 'cinnober-java'
    group = 'com.cinnober' // or something similar

Then everything should just work as usual with the java, maven and eclipse plugins.

If you think it takes to long to generate javadoc all the time, the 'javadocJar' task can
be excluded in your build:

    gradle -x javadocJar clean build

