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
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.GroovyCompile
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

        project.tasks.withType(GroovyCompile) {

            //Adds Enterprise Groovy library to the project
            DependencySet compileDeps = project.getConfigurations().getByName("compileOnly").getDependencies()
            project.getGradle().addListener(new DependencyResolutionListener() {
                @Override
                void beforeResolve(ResolvableDependencies resolvableDependencies) {
                    compileDeps.add(project.getDependencies().create("com.virtualdogbert:enterprise-groovy:1.0"))
                    project.getGradle().removeListener(this)
                }

                @Override
                void afterResolve(ResolvableDependencies resolvableDependencies) {}
            })

            if (project.hasProperty('compilationScript')) {
                // Add the configuration script file to the compiler options.
                project.compileGroovy.groovyOptions.configurationScript = project.file(project.compilationScript)
            } else {
                project.compileGroovy.groovyOptions.configurationScript = project.file('conventions.groovy')
            }

            if (!project.hasProperty('console')) {

                //Adds Groovy console task
                project.task('console', dependsOn: 'classes', type: JavaExec) {
                    group = 'enterprise groovy'

                    main = 'groovy.ui.Console'

                    systemProperties(['enterprise.groovy.console': 'true'])

                    project.repositories {
                        jcenter()
                        mavenLocal()
                    }

                    Configuration consoleRuntime = project.configurations.create("consoleRuntime")
                    consoleRuntime.dependencies.add(project.dependencies.localGroovy())
                    consoleRuntime.dependencies.add(project.getDependencies().create("com.virtualdogbert:enterprise-groovy:1.0"))

                    classpath = project.sourceSets.main.runtimeClasspath + project.files(consoleRuntime.asPath)
                }
            }
        }
    }

    /**
     * Util method for copying a convention file, to the root of the project.
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
