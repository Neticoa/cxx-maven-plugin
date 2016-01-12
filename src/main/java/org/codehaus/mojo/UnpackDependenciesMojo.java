package org.codehaus.mojo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.filters.MarkerFileFilter;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

/* rewrite of  UnpackDependenciesMojo from maven-dependency plugin, goal 'unpack-dependencies' */
import org.apache.maven.plugin.dependency.fromDependencies.AbstractFromDependenciesMojo;

/* Use enhanced FileSet and FileManager (not the one provided in this project)*/
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.lang.reflect.Field;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.ArchiverException;
// our own personal org.codehaus.plexus.archiver api extension
import org.codehaus.plexus.archiver.ArchiveContentLister;
import org.codehaus.plexus.archiver.ArchiveContentEntry;
import org.codehaus.plexus.archiver.manager.ArchiveContentListerManager;


/**
 * Goal that unpacks the project dependencies from the repository to a defined
 * location. 
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>, Franck Bonin
 * @since 0.0.6
 */
@Mojo( name = "unpack-dependencies", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true )
public class UnpackDependenciesMojo
    extends AbstractFromDependenciesMojo
{
    /**
     * A comma separated list of file patterns to include when unpacking the
     * artifact.  i.e. <code>**\/*.xml,**\/*.properties</code>
     * NOTE: Excludes patterns override the includes.
     * (component code = <code>return isIncluded( name ) AND !isExcluded( name );</code>)
     *
     * @since 0.0.6
     */
    @Parameter( property = "mdep.unpack.includes" )
    private String includes;

    /**
     * A comma separated list of file patterns to exclude when unpacking the
     * artifact.  i.e. <code>**\/*.xml,**\/*.properties</code>
     * NOTE: Excludes patterns override the includes.
     * (component code = <code>return isIncluded( name ) AND !isExcluded( name );</code>)
     *
     * @since 0.0.6
     */
    @Parameter( property = "mdep.unpack.excludes" )
    private String excludes;
    
    
    /**
     * To look up ArchiveContentLister implementations
     */
    @Component
    protected ArchiveContentListerManager archiveContentListerManager;
    
    
    /**
     * Directory where dependencies shall be flattened
     * 
     * @since 0.0.6
     */
    @Parameter(property = "mdep.unpack.flattenDestDirs")
    private List flattenDestDirs = new ArrayList();
    
    //todo doublon with CMakeMojo !!
    /*protected String extractBuildConfig(String classifier)
    {
        //bin-${targetClassifier}-${buildConfig}
        String[] parts = classifier.split("-");
        return (parts.length >= 3)? parts[parts.length-1] : null;
    }
     
    protected String extractSubClassifier(String classifier)
    {
        //bin-${targetClassifier}-${buildConfig}
        String[] parts = classifier.split("-");
        if (parts.length >= 3)
        {
            //parts[1] .. parts[length-2]
            StringBuilder builder = new StringBuilder();
            builder.append(parts[1]);
            for(int i = 2; i <= parts.length-2; i++) 
            {
                builder.append("-");
                builder.append(parts[i]);
            }
            return builder.toString();
        }
        return null;
    }
    */
    /**
     * get the archive content.
     *
     * @param artifact File to be unpacked.
     * @param srcRoot  Location where the whole archive was unpacked.
     * @param location Location where to put the unpacked files.
     * @param includes Comma separated list of file patterns to include i.e. <code>**&#47;.xml,
     *                 **&#47;*.properties</code>
     * @param excludes Comma separated list of file patterns to exclude i.e. <code>**&#47;*.xml,
     *                 **&#47;*.properties</code>
     */
    protected void listAndFlatCopy( Artifact artifact, File srcRoot, File location, String includes, String excludes )
        throws MojoExecutionException
    {
        File file = artifact.getFile(); 
        try
        {
            //logUnpack( file, location, includes, excludes );

            location.mkdirs();

            if ( file.isDirectory() )
            {
                // usual case is a future jar packaging, but there are special cases: classifier and other packaging
                throw new MojoExecutionException( "Artifact has not been packaged yet. When used on reactor artifact, "
                    + "unpack should be executed after packaging: see MDEP-98." );
            }

            ArchiveContentLister archiveContentLister;

            try
            {
                archiveContentLister = archiveContentListerManager.getArchiveContentLister( artifact.getType() );
                getLog().debug( "Found archiveContentLister by type: " + archiveContentLister );
            }
            catch ( NoSuchArchiverException e )
            {
                archiveContentLister = archiveContentListerManager.getArchiveContentLister( file );
                getLog().debug( "Found unArchiver by extension: " + archiveContentLister );
            }

            //unArchiver.setUseJvmChmod( useJvmChmod );

            //unArchiver.setIgnorePermissions( ignorePermissions );

            archiveContentLister.setSourceFile( file );

            //unArchiver.setDestDirectory( location );

            if ( StringUtils.isNotEmpty( excludes ) || StringUtils.isNotEmpty( includes ) )
            {
                // Create the selectors that will filter
                // based on include/exclude parameters
                // MDEP-47
                IncludeExcludeFileSelector[] selectors =
                    new IncludeExcludeFileSelector[]{ new IncludeExcludeFileSelector() };

                if ( StringUtils.isNotEmpty( excludes ) )
                {
                    selectors[0].setExcludes( excludes.split( "," ) );
                }

                if ( StringUtils.isNotEmpty( includes ) )
                {
                    selectors[0].setIncludes( includes.split( "," ) );
                }

                archiveContentLister.setFileSelectors( selectors );
            }
            if ( this.silent )
            {
                silenceArchiveContentLister( archiveContentLister );
            }

            List<ArchiveContentEntry> contents = archiveContentLister.list();
            
            for (ArchiveContentEntry content : contents)
            {
                if (content.getType() == ArchiveContentEntry.FILE)
                {
                    String sSubFileName = (new File(content.getName())).getName();
                    String src = srcRoot.getAbsolutePath() + File.separator + content.getName();
                    String dst = location +  File.separator + sSubFileName;
                    try
                    {
                        Files.copy(Paths.get(src), Paths.get(dst), StandardCopyOption.REPLACE_EXISTING);
                        getLog().info("Copy " + src + " to " + dst);
                    }
                    catch ( IOException e )
                    {
                        getLog().error( "Copy of " + src + " to " + dst + " failed : " + e);
                    }    
                }
            }            
            
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "Unknown archiver type", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException(
                "Error unpacking file: " + file + " to: " + location + "\r\n" + e.toString(), e );
        }
    }
    
    private void silenceArchiveContentLister( ArchiveContentLister archiveContentLister )
    {
        // dangerous but handle any errors. It's the only way to silence the unArchiver.
        try
        {
            Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses( "logger", archiveContentLister.getClass() );

            field.setAccessible( true );

            field.set( archiveContentLister, this.getLog() );
        }
        catch ( Exception e )
        {
            // was a nice try. Don't bother logging because the log is silent.
        }
    }
    
    /**
     * Main entry into mojo. This method gets the dependencies and iterates
     * through each one passing it to DependencyUtil.unpackFile().
     *
     * @throws MojoExecutionException with a message if an error occurs.
     * @see #getDependencies
     * @see DependencyUtil#unpackFile(Artifact, File, File, ArchiverManager,
     *      Log)
     */
    protected void doExecute()
        throws MojoExecutionException
    {
        DependencyStatusSets dss = getDependencySets( this.failOnMissingClassifierArtifact );

        for ( Artifact artifact : dss.getResolvedDependencies() )
        {
            File destDir;
            destDir = DependencyUtil.getFormattedOutputDirectory( useSubDirectoryPerScope, useSubDirectoryPerType,
                                                                  useSubDirectoryPerArtifact, useRepositoryLayout,
                                                                  stripVersion, outputDirectory, artifact );                                 

            unpack( artifact, destDir, getIncludes(), getExcludes() );
            DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler( artifact, this.markersDirectory );
            handler.setMarker();
            
            
            Iterator it = flattenDestDirs.iterator();
            HashSet<String> incudedSet = new HashSet<String>();
            while( it.hasNext() )
            {
                String flattenDestDir = it.next().toString();
                listAndFlatCopy( artifact, destDir, new File(flattenDestDir), getIncludes(), getExcludes() );
            }

            
            
            /*
            String artifactId = artifact.getArtifactId();
            String classifer = artifact.getClassifier();
            String subClassifier = extractSubClassifier(classifer);
            String buildConfig = extractBuildConfig(classifer);    
            
            if (StringUtils.isNotEmpty(subClassifier) && StringUtils.isNotEmpty(buildConfig) )
            {
                getLog().debug("Artifact " + artifactId + " with classifier " +
                    classifer + "( " + subClassifier + ", " + buildConfig + " ) could be flattened");
              
                Iterator it = flattenDestDirs.iterator();
                HashSet<String> incudedSet = new HashSet<String>();
                while( it.hasNext() )
                {
                    String flattenDestDir = it.next().toString();
                    
                    String sourceDir = destDir.getAbsolutePath() + File.separator + extractSubClassifier(classifer) +
                        File.separator + extractBuildConfig(classifer) + File.separator + artifactId;
                        
                    getLog().debug("Artifact " + artifactId + " content " +
                        
                    FileSet afileSet = new FileSet();
                    afileSet.setDirectory( sourceDir );
                    // $FB pour éviter d'avoir TROP de fichiers exclude (inutile) dans la boucle for ci-après
                    afileSet.setUseDefaultExcludes( false ); 
                    if ( StringUtils.isNotEmpty( includes ) )
                    {
                        afileSet.setIncludes( Arrays.asList( includes.split( "," ) ) );
                    }
                    if ( StringUtils.isNotEmpty( excludes ) )
                    {
                        afileSet.setExcludes( Arrays.asList( excludes.split( "," ) ) );
                    }
                    
                    FileSetManager aFileSetManager = new FileSetManager();
                    String[] found = aFileSetManager.getIncludedFiles( afileSet);
                    
                    incudedSet.addAll( new HashSet<String>( Arrays.asList( found) ));
                    
                    if (! incudedSet.isEmpty() )
                    {
                        File newDirs = new File(flattenDestDir);
                        if (Files.notExists(Paths.get(newDirs.getPath())))
                        {
                            getLog().info("dirs to generate : "+ newDirs.getAbsoluteFile());
                            newDirs.mkdirs();
                        }
                    }
                    
                    getLog().info( "file from " + sourceDir + " to flatten to " + flattenDestDir + " are :");
                    for ( Iterator<String> iter = incudedSet.iterator(); iter.hasNext(); )
                    {
                        String sSubFilePath = iter.next();
                        getLog().info(sSubFilePath);
                        String sSubFileName = (new File(sSubFilePath)).getName();
                        String src = sourceDir + File.separator + sSubFilePath;
                        String dst = flattenDestDir + File.separator + sSubFileName;
                        try
                        {
                            Files.copy(Paths.get(src), Paths.get(dst), StandardCopyOption.REPLACE_EXISTING);
                            getLog().info("copy " + src + " to " + dst);
                        }
                        catch ( IOException e )
                        {
                            getLog().error( "Copy of " + src + " to " + dst + " failed : " + e);
                        }
                    }
                }
            }*/
        }

        for ( Artifact artifact : dss.getSkippedDependencies() )
        {
            getLog().info( artifact.getFile().getName() + " already exists in destination." );
        }
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new MarkerFileFilter( this.overWriteReleases, this.overWriteSnapshots, this.overWriteIfNewer,
                                     new DefaultFileMarkerHandler( this.markersDirectory ) );
    }

    /**
     * @return Returns a comma separated list of excluded items
     */
    public String getExcludes()
    {
        return DependencyUtil.cleanToBeTokenizedString( this.excludes );
    }

    /**
     * @param excludes A comma separated list of items to exclude
     *                 i.e. <code>**\/*.xml, **\/*.properties</code>
     */
    public void setExcludes( String excludes )
    {
        this.excludes = excludes;
    }

    /**
     * @return Returns a comma separated list of included items
     */
    public String getIncludes()
    {
        return DependencyUtil.cleanToBeTokenizedString( this.includes );
    }

    /**
     * @param includes A comma separated list of items to include
     *                 i.e. <code>**\/*.xml, **\/*.properties</code>
     */
    public void setIncludes( String includes )
    {
        this.includes = includes;
    }
}
