package org.codehaus.mojo;

/*
 * Copyright (C) 2011-2016, Neticoa SAS France - Tous droits réservés.
 * Author(s) : Franck Bonin, Neticoa SAS France
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
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

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;

import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.codehaus.utils.ClassifierRegexFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;  

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.model.Scm;

import org.codehaus.utils.ExecutorService;

/**
 * Goal that retrieve source dependencies from the SCM.
 *
 * @author Franck Bonin
 * @since 0.0.6
 */
@Mojo( name = "scm-dependencies", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true )
public class ScmDependenciesMojo
        extends org.apache.maven.plugin.dependency.fromDependencies.AbstractDependencyFilterMojo
{
    /**
     * Pom directory location
     * 
     * @since 0.0.6
     */
    @Parameter( property = "basedir", readonly = true, required = true )
    protected File basedir;
    
    /**
     * svn:externals directory carrier
     * 
     * @since 0.0.6
     */
    @Parameter( property = "svnExternalsPropertyDir", defaultValue = "." )
    protected String svnExternalsPropertyDir;
    
    /**
     * Comma Separated list of Classifiers to include. Empty String indicates
     * include everything (default).
     *
     * @since 0.0.6
     */
    @Parameter( property = "includeRegexClassifiers", defaultValue = "" )
    protected String includeRegexClassifiers;

    /**
     * Comma Separated list of Classifiers to exclude. Empty String indicates
     * don't exclude anything (default).
     *
     * @since 0.0.6
     */
    @Parameter( property = "excludeRegexClassifiers", defaultValue = "" )
    protected String excludeRegexClassifiers;
    
    /**
     * SCM connection information to use
     * 
     * @since 0.0.6
     */
    @Parameter( property = "connectionType", defaultValue = "connection" )
    protected String connectionType;
    
    
    /**
     * origin : derived from org.apache.maven.plugin.dependency.fromDependencies.UnpackDependenciesMojo
     * $FB duplicate with UnPackDependenciesMojo
     */
    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new MarkerFileFilter( this.overWriteReleases, this.overWriteSnapshots, this.overWriteIfNewer,
                                     new DefaultFileMarkerHandler( this.markersDirectory ) );
    }
  
    /**
     * origin : org.apache.maven.plugin.dependency.fromDependencies.AbstractDependencyFilterMojo
     * $FB duplicate with UnPackDependenciesMojo
     */
    @Component
    MavenProjectBuilder myProjectBuilder;
    
    /**
     * origin : org.apache.maven.plugin.dependency.fromDependencies.AbstractDependencyFilterMojo
     * $FB duplicate with UnPackDependenciesMojo
     */
    private MavenProject buildProjectFromArtifact( Artifact artifact )
        throws MojoExecutionException
    {
        try
        {
            return myProjectBuilder.buildFromRepository( artifact, remoteRepos, getLocal() );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
  
    /**
     * origin : org.apache.maven.plugin.dependency.fromDependencies.AbstractDependencyFilterMojo
     * $FB duplicate with UnPackDependenciesMojo
     */
    private void addParentArtifacts( MavenProject project, Set<Artifact> artifacts )
        throws MojoExecutionException
    {
        while ( project.hasParent() )
        {
            project = project.getParent();

            if ( project.getArtifact() == null )
            {
                // Maven 2.x bug
                Artifact artifact =
                    factory.createBuildArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                                 project.getPackaging() );
                project.setArtifact( artifact );
            }

            if ( !artifacts.add( project.getArtifact() ) )
            {
                // artifact already in the set
                break;
            }
            try
            {
                resolver.resolve( project.getArtifact(), this.remoteRepos, this.getLocal() );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }
    }
    
    /**
     * Method creates filters and filters the projects dependencies. This method
     * also transforms the dependencies if classifier is set. The dependencies
     * are filtered in least specific to most specific order
     * 
     * origin : derived from org.apache.maven.plugin.dependency.fromDependencies.AbstractDependencyFilterMojo
     *
     * @param stopOnFailure
     * @return DependencyStatusSets - Bean of TreeSets that contains information
     *         on the projects dependencies
     * @throws MojoExecutionException
     */
    protected DependencyStatusSets getDependencySets( boolean stopOnFailure, boolean includeParents )
        throws MojoExecutionException
    {
        // add filters in well known order, least specific to most specific
        FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter( new ProjectTransitivityFilter( project.getDependencyArtifacts(), this.excludeTransitive ) );

        filter.addFilter( new ScopeFilter( DependencyUtil.cleanToBeTokenizedString( this.includeScope ),
                                           DependencyUtil.cleanToBeTokenizedString( this.excludeScope ) ) );

        /*filter.addFilter( new TypeFilter( DependencyUtil.cleanToBeTokenizedString( this.includeTypes ),
                                          DependencyUtil.cleanToBeTokenizedString( this.excludeTypes ) ) );*/
        filter.addFilter( new TypeFilter( DependencyUtil.cleanToBeTokenizedString( "pom" ),
                                          DependencyUtil.cleanToBeTokenizedString( null ) ) );

        filter.addFilter( new ClassifierFilter( DependencyUtil.cleanToBeTokenizedString( this.includeClassifiers ),
                                                DependencyUtil.cleanToBeTokenizedString( this.excludeClassifiers ) ) );
                                                
        filter.addFilter( new ClassifierRegexFilter (
            DependencyUtil.cleanToBeTokenizedString( this.includeRegexClassifiers ),
            DependencyUtil.cleanToBeTokenizedString( this.excludeRegexClassifiers ) ) );  

        filter.addFilter( new GroupIdFilter( DependencyUtil.cleanToBeTokenizedString( this.includeGroupIds ),
                                             DependencyUtil.cleanToBeTokenizedString( this.excludeGroupIds ) ) );

        filter.addFilter( new ArtifactIdFilter( DependencyUtil.cleanToBeTokenizedString( this.includeArtifactIds ),
                                                DependencyUtil.cleanToBeTokenizedString( this.excludeArtifactIds ) ) );
                                                                               

        // start with all artifacts.
        @SuppressWarnings( "unchecked" ) Set<Artifact> artifacts = project.getArtifacts();

        if ( includeParents )
        {
            // add dependencies parents
            for ( Artifact dep : new ArrayList<Artifact>( artifacts ) )
            {
                addParentArtifacts( buildProjectFromArtifact( dep ), artifacts );
            }

            // add current project parent
            addParentArtifacts( project, artifacts );
        }

        // perform filtering
        try
        {
            artifacts = filter.filter( artifacts );
        }
        catch ( ArtifactFilterException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        // transform artifacts if classifier is set
        DependencyStatusSets status;
        if ( StringUtils.isNotEmpty( classifier ) )
        {
            status = getClassifierTranslatedDependencies( artifacts, stopOnFailure );
        }
        else
        {
            status = filterMarkedDependencies( artifacts );
        }

        return status;
    }
    
    protected class SvnInfo extends DefaultHandler
    {
        private String root = null;
        private String relativeUrl = null;
        
        public void reset()
        {
            root = null;
            relativeUrl = null;
        }
        
        public boolean isValide()
        {
            return root != null && relativeUrl != null;
        }
        
        public String getSvnRoot()
        {
            return root;
        }
        
        public String getSvnRelativeUrl()
        {
            return relativeUrl;
        }
        
        private boolean bRoot = false;
        private boolean bRelativeUrl = false;
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException
        {
            if ( qName.equalsIgnoreCase( "root" ) )
            {
                bRoot = true;
            }
            else if ( qName.equalsIgnoreCase( "relative-url" ) )
            {
                bRelativeUrl = true;
            }
        }
        
        @Override
        public void characters(char ch[], int start, int length) throws SAXException
        {
            if (bRoot)
            {
                root = new String( ch, start, length );
                bRoot = false;
            }
            else if (bRelativeUrl)
            {
                relativeUrl = new String( ch, start, length );
                bRelativeUrl = false;
            }
        }
    }
    
    protected SvnInfo getSvnInfo(File externalsDir) throws MojoExecutionException
    {
        // svn info <externalsDir> --xml
        Map enviro = null;
        try
        {
            enviro = ExecutorService.getEnvs();
        }
        catch ( IOException x )
        {
            getLog().error( "Could not assign default system enviroment variables.", x );
        }      
        CommandLine commandLine = ExecutorService.getExecutablePath( "svn", enviro, externalsDir );
        
        commandLine.addArguments( new String[] {"info", externalsDir.getAbsolutePath(), "--xml"} );

        Executor exec = new DefaultExecutor();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try
        {
            int resultCode = ExecutorService.executeCommandLine( exec, commandLine, enviro, out,
                System.err, System.in );
        }
        catch ( ExecuteException e )
        {
            throw new MojoExecutionException( "Command execution failed.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Command execution failed.", e );
        }
        
        SvnInfo svnInfo = new SvnInfo();
        try
        {
            SAXParserFactory sfactory = SAXParserFactory.newInstance();
            SAXParser parser = sfactory.newSAXParser();
            XMLReader xmlparser = parser.getXMLReader();
            xmlparser.setContentHandler( svnInfo );
            xmlparser.parse( new InputSource( out.toString( "UTF8" ) ) );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "svn info xml parsing failed.", e );
        }        
        return svnInfo;
    }
    
    protected void doExecute()
        throws MojoExecutionException
    {
        DependencyStatusSets dss = getDependencySets( true );

        for ( Artifact artifact : dss.getResolvedDependencies() )
        {
            MavenProject project = buildProjectFromArtifact( artifact );
            Scm scm = project.getScm();

            if ( scm == null ) {
                getLog().error( "No SCM specified for " + artifact.getArtifactId() );
                throw new IllegalStateException();
            }

            String scmUri = scm.getConnection();
            if (StringUtils.equals( connectionType, "developerConnection" ) )
            {
                scmUri = scm.getDeveloperConnection();
            }
            
            if ( StringUtils.isEmpty( scmUri ) )
            {
                getLog().error( "No SCM Uri specified for " + artifact.getArtifactId() );
                throw new IllegalStateException();
            }
            
            String[] scmType = scmUri.split( ":", 2 );
            
            if ( scmType.length < 2 || ! StringUtils.equalsIgnoreCase( scmType[0], "scm" ) )
            {
                getLog().error( "SCM Uri content invalide " + scmUri );
                throw new IllegalStateException();
            }
            
            if ( StringUtils.equalsIgnoreCase( scmType[1], "svn" ) )
            {
                // check svn::externals dir 
                File externalsDir = new File( basedir.toString() + File.separator + svnExternalsPropertyDir );
                if ( ! externalsDir.isDirectory() )
                {
                    getLog().error( "Svn externals dir does not exists or is not a directory " + externalsDir );
                    throw new IllegalStateException();
                }
                
                SvnInfo svnInfo = getSvnInfo(externalsDir);
                
                if ( ! svnInfo.isValide() )
                {
                    getLog().error( "Svn info not available for externals dir " + externalsDir );
                    throw new IllegalStateException();
                }
                
                // todo la suite dans #15816, point 5

            } 
            else
            {
                getLog().error( "SCM unsupported yet " + scmType[1] );
                throw new IllegalStateException();
            }
        }  
    }
}
