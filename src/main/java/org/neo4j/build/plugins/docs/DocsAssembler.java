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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.FileUtils;

final class DocsAssembler
{
    static final String CLASSIFIER = "docs";

    static final String TYPE = "jar";

    private static final int BUFFER_SIZE = 4096;

    private static final String DOCS_DIRNAME = "docs";

    private static final List<String> NON_FILTERED_FILE_EXTENSIONS;

    private final Log log;

    private final MavenProject project;

    private final MavenProjectHelper projectHelper;

    private MavenResourcesFiltering resourceFiltering;

    private MavenSession session;

    private int currentBaseDirPathLength;

    private ZipOutputStream zipOut;

    static
    {
        // these are always non-filtered anyhow:
        // jpg, jpeg, gif, bmp, png
        // as defined in
        // org.apache.maven.shared.filtering.DefaultMavenResourcesFiltering

        NON_FILTERED_FILE_EXTENSIONS = Arrays.asList( new String[] { "tiff",
                "tif", "pdf", "zip", "gz" } );
    }

    private Log getLog()
    {
        return log;
    }

    private DocsAssembler( final MavenProject project, final Log log,
            final MavenProjectHelper projectHelper,
            final MavenResourcesFiltering resourceFiltering,
            MavenSession session )
    {
        this.project = project;
        this.log = log;
        this.projectHelper = projectHelper;
        this.resourceFiltering = resourceFiltering;
        this.session = session;
    }

    static File assemble( final List<String> sourceDirectories,
            final boolean filter, final Log log, MavenSession session,
            final MavenProject project, final MavenProjectHelper projectHelper,
            MavenResourcesFiltering resourceFiltering )
            throws MojoExecutionException
    {
        DocsAssembler assembler = new DocsAssembler( project, log,
                projectHelper, resourceFiltering, session );
        return assembler.doAssembly( sourceDirectories, filter );
    }

    private File doAssembly( final List<String> sourceDirectories,
            final boolean filter ) throws MojoExecutionException
    {
        getLog().info( "Filtering is: " + ( filter ? "on" : "off" ) );
        List<File> dirs = getDirectories( sourceDirectories );
        if ( dirs.size() == 0 )
        {
            getLog().warn( "There are no docs to assemble." );
            return null;
        }

        File destFile = null;
        if ( filter )
        {
            File target = new File( new File( project.getBuild()
                    .getDirectory() ), "filtered-docs" );
            deleteRecursively( target );
            filterResources( dirs, target );
            destFile = createArchive( Collections.singletonList( target ) );
        }
        else
        {
            destFile = createArchive( dirs );
        }

        projectHelper.attachArtifact( project, TYPE, CLASSIFIER, destFile );

        return destFile;
    }

    private File createArchive( final List<File> directories )
            throws MojoExecutionException
    {
        getLog().info( "Creating docs archive." );

        final String filename = project.getArtifactId() + "-"
                                + project.getVersion() + "-" + CLASSIFIER + "."
                                + TYPE;
        final File destFile = new File( project.getBuild()
                .getDirectory(), filename );
        if ( destFile.exists() && !destFile.delete() )
        {
            throw new MojoExecutionException( "Could not delete: "
                                              + destFile.getAbsolutePath() );
        }

        FileOutputStream fileOut = null;
        zipOut = null;
        try
        {
            fileOut = new FileOutputStream( destFile );
            zipOut = new ZipOutputStream( fileOut );

            for ( File dir : directories )
            {
                currentBaseDirPathLength = dir.getAbsolutePath()
                        .length();
                getLog().info( "Adding source directory: " + dir );
                zipDirectory( dir );
            }
        }
        catch ( FileNotFoundException e )
        {
            getLog().error( e );
        }
        finally
        {
            if ( zipOut != null )
            {
                try
                {
                    zipOut.close();
                }
                catch ( ZipException e )
                {
                    if ( "ZIP file must have at least one entry".equals( e.getMessage() ) )
                    {
                        getLog().warn( "There were no docs to assemble." );
                    }
                    else
                    {
                        getLog().error( e );
                    }
                }
                catch ( IOException e )
                {
                    getLog().error( e );
                }
            }
        }
        return destFile;
    }

    private void filterResources( final List<File> directories, File targetDir )
    {
        getLog().info( "Filter target: " + targetDir );
        int baseDirLength = project.getBasedir()
                .getAbsolutePath()
                .length() + 1;
        List<Resource> resources = new ArrayList<Resource>();
        for ( File dir : directories )
        {
            Resource resource = new Resource();
            resource.setDirectory( dir.getAbsolutePath()
                    .substring( baseDirLength ) );
            resource.setFiltering( true );
            getLog().info( "Adding source directory: " + dir );
            resources.add( resource );
        }

        MavenResourcesExecution resourcesExecution = new MavenResourcesExecution(
                resources, targetDir, project, "UTF-8",
                Collections.emptyList(), NON_FILTERED_FILE_EXTENSIONS, session );
        resourcesExecution.setResourcesBaseDirectory( project.getBasedir() );
        resourcesExecution.addFilterWrapper( new FileUtils.FilterWrapper()
        {
            @Override
            public Reader getReader( final Reader reader )
            {
                return reader;
            }
        } );
        try
        {
            resourceFiltering.filterResources( resourcesExecution );
        }
        catch ( MavenFilteringException e )
        {
            getLog().error( e );
        }
    }

    private void zipDirectory( final File dir ) throws MojoExecutionException
    {
        byte[] buf = new byte[BUFFER_SIZE];

        for ( File file : dir.listFiles() )
        {
            if ( file.isDirectory() )
            {
                zipDirectory( file );
                continue;
            }
            FileInputStream inFile = null;
            try
            {
                getLog().debug( "Adding: " + file.getAbsolutePath() );
                inFile = new FileInputStream( file.getAbsolutePath() );
                zipOut.putNextEntry( new ZipEntry( file.getAbsolutePath()
                        .substring( currentBaseDirPathLength ) ) );
                int len;
                while ( ( len = inFile.read( buf ) ) > 0 )
                {
                    zipOut.write( buf, 0, len );
                }
            }
            catch ( FileNotFoundException e )
            {
                getLog().error( e );
            }
            catch ( IOException e )
            {
                getLog().error( e );
            }
            finally
            {
                try
                {
                    zipOut.closeEntry();
                    inFile.close();
                }
                catch ( IOException e )
                {
                    getLog().error( e );
                }
            }
        }
    }

    private List<File> getDirectories( final List<String> sourceDirectories )
            throws MojoExecutionException
    {
        List<File> directories = new ArrayList<File>();
        if ( sourceDirectories == null )
        {
            getLog().info( "No directories configured, using defaults." );
            // add default directories
            // ./src/docs and ./target/docs
            addDirectory( new File( new File( project.getBasedir(), "src" ),
                    DOCS_DIRNAME ), directories );
            addDirectory( new File( project.getBuild()
                    .getDirectory(), DOCS_DIRNAME ), directories );
        }
        else
        {
            for ( String dir : sourceDirectories )
            {
                addDirectory( new File( dir ), directories );
            }
        }
        return directories;
    }

    private void addDirectory( final File dir, final List<File> directories )
            throws MojoExecutionException
    {
        if ( !dir.exists() )
        {
            getLog().info( "Skipping, does not exist: " + dir );
            return;
        }
        if ( dir.listFiles().length == 0 )
        {
            getLog().info( "Skipping, is empty: " + dir );
            return;
        }
        if ( !dir.isDirectory() )
        {
            throw new MojoExecutionException( "Not a directory: "
                                              + dir.getAbsolutePath() );
        }
        if ( !dir.canRead() )
        {
            throw new MojoExecutionException( "Can not read directory: "
                                              + dir.getAbsolutePath() );
        }
        directories.add( dir );
    }

    private static void deleteRecursively( File file )
    {
        if ( !file.exists() )
        {
            return;
        }

        if ( file.isDirectory() )
        {
            for ( File child : file.listFiles() )
            {
                deleteRecursively( child );
            }
        }
        if ( !file.delete() )
        {
            throw new RuntimeException(
                    "Couldn't delete directory. Offending file:" + file );
        }
    }
}
