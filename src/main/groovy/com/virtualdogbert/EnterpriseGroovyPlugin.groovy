package com.virtualdogbert

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec

/**
 * This plugin adds the Enterprise Groovy library, source sets to match the config for Enterprise Groovy, and the Groovy console which is good for
 * testing and debugging.
 */
class EnterpriseGroovyPlugin implements Plugin<Project> {

    static final  String      conventionsFile       = "conventions.groovy.template"
    static final  String      GrailsConventionsFile = "conventions-grails.groovy.template"
    static final  String      conventionsOut        = "conventions.groovy"
    private final ClassLoader loader                = getClass().getClassLoader()

    void apply(Project project) {

        if (!project.hasProperty('console')) {

            //Adds Groovy console task
            project.task('console', dependsOn: 'classes', type: JavaExec) {
                group = 'enterprise groovy'

                logger.debug("Opening Gradle Console")

                main = 'groovy.ui.Console'

                Configuration consoleRuntime = project.configurations.create("consoleRuntime")
                consoleRuntime.dependencies.add(project.dependencies.localGroovy())

                classpath = project.sourceSets.main.runtimeClasspath + project.sourceSets.test.runtimeClasspath + project.files(consoleRuntime.asPath)
                logger.debug("Gradle Console classpath=$classpath")
            }
        }


        //Adds a task to setup the Enterprise Groovy conventions, coping over the default conventions.groovy.
        project.task('setupEnterpriseGroovyConventions') {
            group = 'enterprise groovy'

            doLast {
                copyFileToProject(conventionsFile, project)
            }
        }

        //Adds a task to setup the Enterprise Groovy conventions, coping over the conventions.groovy for grails.
        project.task('setupEnterpriseGroovyGrailsConventions') {
            group = 'enterprise groovy'

            doLast {
                copyFileToProject(GrailsConventionsFile, project)
            }
        }

        if (project.hasProperty('compilationScript')) {
            // Add the configuration script file
            // to the compiler options.
            project.compileGroovy.groovyOptions.configurationScript = project.file(project.compilationScript)
        }


        //Adds Enterprise Groovy library to the project
        project.dependencies {
            delegate.compile('com.virtualdogbert:enterprise-groovy:1.0')
        }
    }

    void copyFileToProject(String file, Project project) {
        String conventionsIn = loader.getResourceAsStream(file).text
        File conventionsOut = new File("$project.projectDir/$conventionsOut")

        if (!conventionsOut.exists()) {
            conventionsOut.append(conventionsIn)
        }
    }
}
