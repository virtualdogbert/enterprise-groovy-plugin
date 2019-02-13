/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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

        if (!project.hasProperty('console') && project.hasProperty('sourceSets')) {

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
            delegate.implementation('com.virtualdogbert:enterprise-groovy:1.0.RC3')
        }
    }

    /**
     * Util method for copying a convention file, to the root of the prodject.
     *
     * @param file The path to the config file.
     * @param project the project to copy the file to.
     */
    void copyFileToProject(String file, Project project) {
        String conventionsIn = loader.getResourceAsStream(file).text
        File conventionsOut = new File("$project.projectDir/$conventionsOut")

        if (!conventionsOut.exists()) {
            conventionsOut.append(conventionsIn)
        }
    }
}
