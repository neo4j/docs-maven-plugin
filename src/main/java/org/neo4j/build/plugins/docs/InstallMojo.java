/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.build.plugins.docs;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Goal which installs docs. Note: requires Maven 3 for plugin management (as it
 * uses the install plugin to install the file).
 * 
 * @goal install
 * @requiresDirectInvocation
 * @requiresDependencyResolution test
 */
public class InstallMojo extends AbstractMojo
{
    /**
     * Directories to include in the assembly.
     * 
     * @parameter
     */
    private List<String> sourceDirectories;

    /**
     * Test to execute.
     * 
     * @parameter expression="${test}"
     */
    private String test;

    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The Maven Session Object
     * 
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * The Maven PluginManager Object
     * 
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException
    {
        System.out.println( test );
        if ( test != null )
        {
            executeTest();
        }
        assembleInstall();
    }

    private void executeTest() throws MojoExecutionException
    {
        executeMojo(
                plugin( groupId( "org.apache.maven.plugins" ),
                        artifactId( "maven-surefire-plugin" ), version( "2.10" ) ),
                        goal( "test" ),
                        configuration( element( name( "test" ), test ) ),
                        executionEnvironment( project, session, pluginManager ) );
    }

    private void assembleInstall() throws MojoExecutionException
    {
        final File destFile = DocsAssembler.assemble( project, getLog(),
                projectHelper, sourceDirectories );

        final String file = destFile.getAbsolutePath();
        final String pomFile = new File( project.getBasedir(), "pom.xml" ).getAbsolutePath();
        final String groupId = project.getGroupId();
        final String artifactId = project.getArtifactId();
        final String version = project.getVersion();
        final String classifier = "docs";
        final String packaging = "jar";
        final String generatePom = "false";
        executeMojo(
                plugin( groupId( "org.apache.maven.plugins" ),
                        artifactId( "maven-install-plugin" ), version( "2.3.1" ) ),
                        goal( "install-file" ),
                        configuration( element( name( "file" ), file ),
                                element( name( "pomFile" ), pomFile ),
                                element( name( "groupId" ), groupId ),
                                element( name( "artifactId" ), artifactId ),
                                element( name( "version" ), version ),
                                element( name( "classifier" ), classifier ),
                                element( name( "packaging" ), packaging ),
                                element( name( "generatePom" ), generatePom ) ),
                                executionEnvironment( project, session, pluginManager ) );
    }
}

