/**
 * Copyright (c) 2011-2012 "Neo Technology,"
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

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

/**
 * Base class for defining common properties.
 */
public abstract class AbstractDocsMojo extends AbstractMojo
{
    /**
     * Directories to include in the assembly. This setting replaces the default
     * one, which is to include src/docs/ and target/docs/.
     * 
     * @parameter
     */
    protected List<String> sourceDirectories;
    /**
     * Set to "true" to activate filtering.
     * 
     * @parameter expression="${filter}" default-value="false"
     */
    protected boolean filter;
    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;
    /**
     * @component
     */
    protected MavenProjectHelper projectHelper;
    /**
     * 
     * @component role="org.apache.maven.shared.filtering.MavenResourcesFiltering" role-hint="default"
     * @required
     */
    protected MavenResourcesFiltering resourceFiltering;
    /**
     * @parameter default-value="${session}"
     * @readonly
     * @required
     */
    protected MavenSession session;
}
