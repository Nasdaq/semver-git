package com.cinnober.gradle.java

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.Compile

class CinnoberJavaPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.apply plugin: 'cinnober-maven'
        project.apply plugin: 'java'
        project.apply plugin: 'eclipse'
        
        project.task('sourcesJar', type: Jar) {
            dependsOn 'classes'
            classifier = 'sources' 
            group = 'Build'
            description = 'Assembles a jar archive containing the main source code.'
            from project.sourceSets.main.allSource
        } 
        
        project.task('javadocJar', type: Jar) {
            dependsOn 'javadoc'
            classifier = 'javadoc' 
            group = 'Documentation'
            description = 'Assembles a jar archive containing the javadoc.'
            from project.tasks.javadoc.destinationDir
        } 
        
        project.artifacts { 
            archives project.tasks.sourcesJar
            archives project.tasks.javadocJar
        }

        if (project.hasProperty('xlint')) {
            project.property('xlint').split(',').each { value ->
                project.tasks.withType(Compile) {
                    options.compilerArgs << "-Xlint:" + value
                }
            }
        }
    }
}