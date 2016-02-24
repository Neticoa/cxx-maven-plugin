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
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.net.URL;
import java.net.MalformedURLException;

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

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
// require Maven 3 API :
/*import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;*/
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

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
     * The user name (used by svn).
     */
    @Parameter( property = "username" )
    private String username = null;

    /**
     * The user password (used by svn).
     */
    @Parameter( property = "password" )
    private String password = null;
    
    /**
     * Maven settings.
     */
    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;
    //@Component
    //private Settings settings;
        
    /**
     * The decrypter for passwords.
     */
    /**
     * When this plugin requires Maven 3.0 as minimum, this component can be removed and o.a.m.s.c.SettingsDecrypter be
     * used instead.
     */
    @Component( hint = "mng-4384" )
    private SecDispatcher secDispatcher;
    //@Component
    //private SettingsDecrypter settingsDecrypter;
    
    
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

    /**
     * Returns the list of servers with decrypted passwords.
     *
     * @return list of servers with decrypted passwords.
     */
    /*List<Server> getDecryptedServers()
    {
        final SettingsDecryptionRequest settingsDecryptionRequest = new DefaultSettingsDecryptionRequest();
        settingsDecryptionRequest.setServers( settings.getServers() );
        final SettingsDecryptionResult decrypt = settingsDecrypter.decrypt( settingsDecryptionRequest );
        return decrypt.getServers();
    }*/
    
    final class Credential
    {
        private String username;
        private String password;

        public Credential( String username, String password )
        {
            this.username = username;
            this.password = password;
        }

        public String getUsername()
        {
            return username;
        }

        public String getPassword()
        {
            return password;
        }
        
        public void setUsername( String username )
        {
            this.username = username;
        }

        public void setPassword( String password )
        {
            this.password = password;
        }
    }
    
    /**
     * Load username password from settings if user has not set them in JVM properties
     * 
     * @see http://blog.sonatype.com/2009/10/maven-tips-and-tricks-encrypting-passwords
     * 
     * origin : derived from org.apache.maven.scm.plugin.AbstractScmMojo::loadInfosFromSettings()
     *
     * @param url not null
     */
    private Credential loadInfosFromSettings( String url /*ScmProviderRepositoryWithHost repo*/ )
    {
        Credential ret = new Credential( username, password );
        if ( username == null || password == null )
        {
            URL aURL = null;
            try
            {
                aURL = new URL( url );
            }
            catch ( MalformedURLException e )
            {
                getLog().warn( "Failed to parse url " + url );
                return ret;
            }

            String host = /*repo*/aURL.getHost();

            int port = /*repo*/aURL.getPort();

            if ( port > 0 )
            {
                host += ":" + port;
            }

            Server server = this.settings.getServer( host );

            if ( server != null )
            {
                if ( username == null )
                {
                    //username = server.getUsername();
                    ret.setUsername( server.getUsername() );
                }

                if ( password == null )
                {
                    //password = decrypt( server.getPassword(), host );
                    ret.setPassword( decrypt( server.getPassword(), host ) );
                }

                /*if ( privateKey == null )
                {
                    privateKey = server.getPrivateKey();
                }

                if ( passphrase == null )
                {
                    passphrase = decrypt( server.getPassphrase(), host );
                }*/
            }
        }
        return ret;
    }

    private String decrypt( String str, String server )
    {
        try
        {
            return secDispatcher.decrypt( str );
        }
        catch ( SecDispatcherException e )
        {
            getLog().warn( "Failed to decrypt password/passphrase for server " + server + ", using auth token as is" );
            return str;
        }
    }
    
    private Map getDefaultSvnCommandLineEnv()
    {
        Map enviro = null;
        try
        {
            enviro = ExecutorService.getEnvs();
            enviro.put( "LC_MESSAGES", "C" );
        }
        catch ( IOException e )
        {
            getLog().error( "Could not assign default system enviroment variables.", e );
        }
        return enviro;     
    }
    
    private CommandLine getDefaultSvnCommandLine( String uri, Map enviro )
    {
        CommandLine commandLine = ExecutorService.getExecutablePath( "svn", enviro, basedir );
        
        Credential cred = loadInfosFromSettings( uri );
        
        if ( cred != null && ! StringUtils.isEmpty( cred.getUsername() ) )
        {
            commandLine.addArguments( new String[] { "--username", cred.getUsername() } );
        }
        if ( cred != null && ! StringUtils.isEmpty( cred.getPassword() ) )
        {
            commandLine.addArguments( new String[] { "--password", cred.getPassword() } );
        }
        /* TODO later :
        commandLine.addArguments( new String[] {"--config-dir", "todo" } );
        commandLine.addArgument( "--no-auth-cache" ); 
        commandLine.addArgument( "--non-interactive" );
        commandLine.addArgument( "--trust-server-cert" );
        */
        
        return commandLine;
    }
    
    /**
     * svn info <uri> request result
     * 
     */
    protected class SvnInfo extends DefaultHandler
    {
        private String url = null;
        private String root = null;
        private String relativeUrl = null;
        private long revision = -1;
        
        public void reset()
        {
            url = null;
            root = null;
            relativeUrl = null;
            revision = -1;
        }
        
        public boolean isValide()
        {
            return url != null && root != null && relativeUrl != null && revision != -1;
        }
        
        public String getSvnUrl()
        {
            return url;
        }
        
        public String getSvnRoot()
        {
            return root;
        }
        
        public String getSvnRelativeUrl()
        {
            return relativeUrl;
        }
         
        public long getRevision()
        {
            return revision;
        }
        
        public String toString()
        {
           return "r" + revision + " " + root + " " + relativeUrl;
        }
        
        private boolean bUrl = false;
        private boolean bRoot = false;
        private boolean bRelativeUrl = false;
        
        @Override
        public void startElement( String uri, String localName, String qName, Attributes attributes )
            throws SAXException
        {
            if ( qName.equalsIgnoreCase( "entry" ) )
            {
                String sRev = attributes.getValue( "revision" );
                try
                {
                    revision = Long.parseLong( sRev );
                }
                catch ( Exception e )
                {
                    throw new SAXException( "URI or folder svn revision not found", e );
                } 
            }
            else if ( qName.equalsIgnoreCase( "url" ) )
            {
                bUrl = true;
            }
            else if ( qName.equalsIgnoreCase( "root" ) )
            {
                bRoot = true;
            }
            else if ( qName.equalsIgnoreCase( "relative-url" ) )
            {
                bRelativeUrl = true;
            }
        }
        
        @Override
        public void characters( char ch[], int start, int length ) throws SAXException
        {
            if ( bUrl )
            {
                url = new String( ch, start, length );
                bUrl = false;
            }
            else if ( bRoot )
            {
                root = new String( ch, start, length );
                bRoot = false;
            }
            else if ( bRelativeUrl )
            {
                relativeUrl = new String( ch, start, length );
                bRelativeUrl = false;
            }
        }
    }
    
    protected SvnInfo getSvnInfo( String uri ) throws MojoExecutionException
    {
        Map enviro = getDefaultSvnCommandLineEnv();
        CommandLine commandLine = getDefaultSvnCommandLine( uri, enviro );

        // svn info <uri> --xml
        commandLine.addArguments( new String[] {"info", uri, "--xml"} );
        
        Executor exec = new DefaultExecutor();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try
        {
            getLog().debug( "Execute command '" + commandLine + "'" );
            int resultCode = ExecutorService.executeCommandLine( exec, commandLine, enviro, out,
                System.err, System.in );
        }
        catch ( ExecuteException e )
        {
            throw new MojoExecutionException( "Command '" + commandLine + "' execution failed.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Command '" + commandLine + "' execution failed.", e );
        }
        
        SvnInfo svnInfo = new SvnInfo();
        try
        {
            SAXParserFactory sfactory = SAXParserFactory.newInstance();
            SAXParser parser = sfactory.newSAXParser();
            XMLReader xmlparser = parser.getXMLReader();
            xmlparser.setContentHandler( svnInfo );
            xmlparser.parse( new InputSource( new ByteArrayInputStream( out.toByteArray( ) ) ) );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "svn info xml parsing failed.", e );
        }        
        return svnInfo;
    }
    
    public String artifactToString( Artifact artifact )
    {
        StringBuffer dependencyStr = new StringBuffer();
        dependencyStr.append( artifact.getGroupId() );
        dependencyStr.append( ":" );
        dependencyStr.append( artifact.getArtifactId() );
        dependencyStr.append( ":" );
        dependencyStr.append( artifact.getVersion() );
        dependencyStr.append( ":" );
        dependencyStr.append( artifact.getType() );
        return dependencyStr.toString();
    }
    
    protected void doExecute()
        throws MojoExecutionException
    {
        DependencyStatusSets dss = getDependencySets( true );
        
        // if we use svn, we need an externals dir
        SvnInfo externalsSvnInfo = null;

        for ( Artifact artifact : dss.getResolvedDependencies() )
        {
            String dependencyStr = artifactToString( artifact );
          
            MavenProject project = buildProjectFromArtifact( artifact );
            Scm scm = project.getScm();

            if ( scm == null )
            {
                throw new MojoExecutionException( "No SCM specified for artifact " + dependencyStr );
            }

            String scmUri = scm.getConnection();
            if ( StringUtils.equals( connectionType, "developerConnection" ) )
            {
                scmUri = scm.getDeveloperConnection();
            }
            
            if ( StringUtils.isEmpty( scmUri ) )
            {
                throw new MojoExecutionException( "No SCM Uri specified for artifact " + dependencyStr );
            }
            
            String[] scmType = scmUri.split( ":", 3 );
            
            if ( scmType.length < 3 || ! StringUtils.equalsIgnoreCase( scmType[0], "scm" ) )
            {
                throw new MojoExecutionException( "SCM Uri content invalide : " + scmUri );
            }
            
            if ( StringUtils.equalsIgnoreCase( scmType[1], "svn" ) )
            {
                String dependencySvnUri = scmType[2];
                
                // check svn::externals dir only once
                if ( null == externalsSvnInfo )
                {
                    File externalsDir = new File( basedir.toString() + File.separator + svnExternalsPropertyDir );
                    if ( ! externalsDir.isDirectory() )
                    {
                        throw new MojoExecutionException( "Svn externals dir does not exists or is not a directory : "
                            + externalsDir );
                    }
                    
                    externalsSvnInfo = getSvnInfo( externalsDir.getAbsolutePath() );
                    
                    if ( ! externalsSvnInfo.isValide() )
                    {
                        throw new MojoExecutionException( "Svn info not available for externals dir : "
                            + externalsDir );
                    }
                                     
                    getLog().info( "Svn externals dir is '" + externalsSvnInfo.toString() + "'" );
                }
                
                // todo la suite dans #15816, point 5
                
                SvnInfo dependencySvnInfo = getSvnInfo( dependencySvnUri );
                
                if ( ! dependencySvnInfo.isValide() )
                {
                    throw new MojoExecutionException( "Svn info not available for dependency : "
                        + dependencyStr + " at " + dependencySvnUri );
                }
                getLog().info( "Svn info for dependency : " + dependencyStr + " are " + dependencySvnInfo.toString() );
            } 
            else
            {
                throw new MojoExecutionException( "SCM unsupported yet : " + scmType[1] );
            }
        }  
    }
}