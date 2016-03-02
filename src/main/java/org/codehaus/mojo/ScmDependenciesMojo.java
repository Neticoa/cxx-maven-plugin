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
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.Collection;
import java.util.Properties;
import java.net.URL;
import java.net.MalformedURLException;
import java.text.StringCharacterIterator;

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
import org.codehaus.utils.SourceTarget;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
// require Maven 3 API :
/*import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;*/
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import org.codehaus.plexus.util.IOUtil;

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
    * Current Project
    * 
    * @since 0.0.6
    */
    @Parameter( property = "project" )
    protected org.apache.maven.project.MavenProject project;
    
    /**
     * Optional source dependencies sub-directory holder
     * 
     * @since 0.0.6
     */
    @Parameter( property = "sourceSubdir", defaultValue = "." )
    protected String sourceSubdir;
    
    /**
     * svn:externals shall use precise svn revisions retrieved from scm info at execution time
     * 
     * @since 0.0.6
     */
    @Parameter( property = "sourceFreezeRevision", defaultValue = "false" )    
    protected boolean sourceFreezeRevision;
    
    // @formatter:off
    /**
     * List of String prefix to remove when creating dependencies target dirs
     * 
     * Example :
     *<pre>{@code
     *<sourceTargetDirRemovePrefixes>
     *  <sourceRemoveTargetPrefixe>fr/neticoa</sourceRemoveTargetPrefixe>
     *  <sourceRemoveTargetPrefixe>module</sourceRemoveTargetPrefixe>
     *</sourceTargetDirRemovePrefixes>}</pre>
     * @since 0.0.6
     */
    // @formatter:on
    @Parameter( )       
    protected String[] sourceTargetDirRemovePrefixes = null;
    
    // @formatter:off
    /**
     * Provide explicit target path for each source dependency
     * 
     * Exemple :
     *<pre>{@code
     *<sourceTargets>
     *  <sourceTarget>
     *    <dependency>
     *      <groupId>fr.neticoa</groupId>
     *      <artifactId>module</artifactId>
     *    </dependency>
     *    <targetDir>src/module</targetDir>
     *  </sourceTarget>
     *</sourceTargets>}</pre>
     * @since 0.0.6
     */
    // @formatter:on
    @Parameter( )
    private SourceTarget[] sourceTargets = null;
    
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
     * 
     * You may use maven setting to store username. 
     * See http://maven.apache.org/guides/mini/guide-encryption.html
     *
     * @since 0.0.6
     */
    @Parameter( property = "username" )
    private String username = null;

    /**
     * The user password (used by svn).
     * 
     * You may use maven setting to store encrypted password. 
     * See http://maven.apache.org/guides/mini/guide-encryption.html
     *
     * @since 0.0.6
     */
    @Parameter( property = "password" )
    private String password = null;
    
    /**
     * The server id to use in maven settings to retrieve credential.
     * Optionnal, by defaut each scm url "hostname[:port]" is taken as server id to search potential
     * credentials in maven settings
     * 
     * See http://maven.apache.org/guides/mini/guide-encryption.html
     *
     * @since 0.0.6
     */
    @Parameter( property = "settingsServerId" )
    private String settingsServerId = null;
    
    /**
     * Maven settings.
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;
    //@Component
    //private Settings settings;
        
    /**
     * The decrypter for passwords.
     * 
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
            String host = settingsServerId;
            if ( StringUtils.isEmpty( host ) )
            {
                URL aURL = null;
                try
                {
                    aURL = new URL( url );
                }
                catch ( MalformedURLException e )
                {
                    getLog().warn( "Failed to parse url '" + url
                        + "' while trying to find associated maven credentials info from maven settings" );
                    return ret;
                }

                host = /*repo*/aURL.getHost();

                int port = /*repo*/aURL.getPort();

                if ( port > 0 )
                {
                    host += ":" + port;
                }
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
        
    
    protected void execSvnCommand( String uri, String[] specificParams, OutputStream out )
        throws MojoExecutionException
    {
        Properties enviro = null;
        try
        {
            enviro = ExecutorService.getSystemEnvVars();
            enviro.put( "LC_MESSAGES", "C" );
        }
        catch ( IOException e )
        {
            getLog().error( "Could not assign default system environment variables.", e );
        }
        
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

        commandLine.addArguments( specificParams );
        
        Executor exec = new DefaultExecutor();
        
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        execSvnCommand( uri, new String[] {"info", uri, "--xml"}, out );
        
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
    
    private class ExternalEntry
    {
        public String revision = null;
        public String origin = null;
        public String targetDir = null;
        
        boolean isValide()
        {
            return ! StringUtils.isEmpty( origin ) && ! StringUtils.isEmpty( targetDir );
        }
        
        public String toString()
        {
            return "" + ( ( null != revision ) ? revision + " " : "" ) + ( ( null != origin ) ? origin : "" )
                + " " + ( ( null != targetDir ) ? targetDir : "" );
        }
    }
    
    private class ExternalsEntries
    {
        private HashMap<String, ExternalEntry> fromOrigin = new HashMap<String, ExternalEntry>();
        private HashMap<String, ExternalEntry> fromTargetDir = new HashMap<String, ExternalEntry>();
        
        void put( ExternalEntry ee )
        {
            if ( fromOrigin.containsKey( ee.origin ) )
            {
                ExternalEntry eeo = fromOrigin.get( ee.origin );
                fromTargetDir.remove( eeo.targetDir );
            }
            if ( fromTargetDir.containsKey( ee.targetDir ) )
            {
                ExternalEntry eetd = fromTargetDir.get( ee.targetDir );
                fromOrigin.remove( eetd.origin );
            }
            
            fromOrigin.put( ee.origin, ee );
            fromTargetDir.put( ee.targetDir, ee );
        }
        
        Collection<ExternalEntry> values()
        {
            return fromOrigin.values();
        }
    }
    

    protected ExternalsEntries loadSvnExternals( String uri ) throws MojoExecutionException
    {
        ExternalsEntries properties = new ExternalsEntries();
      
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        execSvnCommand( uri, new String[] {"propget", "svn:externals", uri}, out );
        
        BufferedReader in = new BufferedReader( new StringReader( out.toString( ) ) );
        
        String line = null;
        
        try
        {
            /* old svn 1.4 :
              third-party/sounds             http://svn.example.com/repos/sounds
              third-party/skins -r148        http://svn.example.com/skinproj
              third-party/skins/toolkit -r21 http://svn.example.com/skin-maker
            */
            /* svn 1.5 and above (operative rev):
              http://svn.example.com/repos/sounds third-party/sounds
              -r148 http://svn.example.com/skinproj third-party/skins
              -r21  http://svn.example.com/skin-maker third-party/skins/toolkit
              svn 1.5 and above (PEG rev):
              http://svn.example.com/repos/sounds third-party/sounds
              http://svn.example.com/skinproj@148 third-party/skins
              http://svn.example.com/skin-maker@21 third-party/skins/toolkit
            */
            
            // this parser only support svn:externals syntaxe 1.5 and above
            // [revision] origin target_dir
            while ( null != ( line = in.readLine() ) )
            {
                StringCharacterIterator iter = new StringCharacterIterator( line );
                SvnExternalsTokenizer m = new SvnExternalsTokenizer( iter );
                ExternalEntry external = new ExternalEntry();

                SvnExternalsTokenizer.Token tok = m.nextToken();
                if ( SvnExternalsTokenizer.TokenType.revision == tok.tokenType )
                {
                    external.revision = tok.value.toString();
                    
                    tok = m.nextToken();
                    external.origin = SvnExternalsTokenizer.TokenType.libelle == tok.tokenType
                        ? tok.value.toString() : null; 
                    
                    tok = m.nextToken();
                    external.targetDir = SvnExternalsTokenizer.TokenType.libelle == tok.tokenType
                        ? tok.value.toString() : null; 
                }
                else if ( SvnExternalsTokenizer.TokenType.libelle == tok.tokenType )
                {
                    external.origin = tok.value.toString();
                    
                    tok = m.nextToken();
                    external.targetDir = SvnExternalsTokenizer.TokenType.libelle == tok.tokenType
                        ? tok.value.toString() : null; 
                }
                
                if ( null != external.origin && null != external.targetDir )
                {
                    properties.put( external );
                }
                else
                {
                    getLog().warn( "unrecognized svn:externals entry of " + uri + " : " + line );
                }
            }
        }
        catch ( Exception e )
        {
            getLog().warn( "Failed to parse svn:externals of " + uri + " : " + e );
        }
        
        return properties;
    }
    
    
    protected void writeSvnExternals( String uri, ExternalsEntries properties ) throws MojoExecutionException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        String targetDirectoryPath = project.getBuild().getDirectory();
        new File( targetDirectoryPath ).mkdirs();
        File file = new File( targetDirectoryPath, "svn.externals" );

        OutputStream outputStream = null;
        try
        {
            outputStream = new FileOutputStream( file );

            for ( ExternalEntry entry : properties.values() )
            {
                if ( entry.isValide() )
                {
                    outputStream.write( ( entry.toString() + "\n" ).getBytes() );
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error writing intermediate properties file "
                + file.toString() + " : " + e );
        }
        finally
        {
            IOUtil.close( outputStream );
        }
        
        execSvnCommand( uri, new String[] {"propset", "svn:externals", "--file", file.getPath(), uri}, out );
    }
    
    ExternalEntry buildExternalEntryFromProvidedInfos( String targetDir, Artifact artifact,
        MavenProject dependencyProject, SvnInfo dependencySvnInfo, SvnInfo rootSvnInfo )
    {
        if ( null != sourceTargetDirRemovePrefixes )
        {
            for ( String sourceTargetDirRemovePrefix : sourceTargetDirRemovePrefixes )
            {
                targetDir = targetDir.replaceFirst( "^" + Pattern.quote( sourceTargetDirRemovePrefix ), "" );
            }
        }
        targetDir = targetDir.replaceFirst( "^/", "" );
      
        ExternalEntry external = null;
        File f = new File( targetDir );
        try
        {
            f.getCanonicalPath();
        }
        catch ( IOException e )
        {
            getLog().warn( "targetPath for external " + artifactToString( artifact ) + " is not a path : "
                + targetDir );
            return external;
        }
        external = new ExternalEntry();
                
        external.targetDir = targetDir;
        
        external.origin = StringUtils.equals( rootSvnInfo.getSvnRoot(), dependencySvnInfo.getSvnRoot() )
            ? dependencySvnInfo.getSvnRelativeUrl() : dependencySvnInfo.getSvnUrl();
    
        external.revision = sourceFreezeRevision
            ? "-r" + String.valueOf( dependencySvnInfo.getRevision() ) : null;

        return external;
    }
    

    ExternalEntry buildCurrentDependencyExternalFromPluginsConfig( Artifact artifact, MavenProject dependencyProject,
       SvnInfo dependencySvnInfo, SvnInfo rootSvnInfo )
    {
        ExternalEntry external = null;
        // Option 3 : if this.externals contains dependencyProject
        if ( null != sourceTargets )
        {
            for ( SourceTarget curExt : sourceTargets )
            {
                if ( curExt.dependencyMatch( artifact ) )
                {
                    getLog().debug( "Dependency " +  curExt.dependency + " match " + artifactToString( artifact ) );
                    external = buildExternalEntryFromProvidedInfos( curExt.targetDir, artifact,
                        dependencyProject, dependencySvnInfo, rootSvnInfo );
                    if ( null != external )
                    {
                        getLog().info( "Dependency " + artifactToString( artifact )
                            + " external entry computed with plugin config is : " + external.toString() );
                    }
                    break;
                }
            }
        }
        return external;
    }
    
    ExternalEntry buildCurrentDependencyFromDependencieConfig( Artifact artifact, MavenProject dependencyProject,
       SvnInfo dependencySvnInfo, SvnInfo rootSvnInfo )
    {
        ExternalEntry external = null;
        // Option 2 : if dependencyProject.getProperties() contains 'scm.dependencies.source.targetDir'
        Properties ps = dependencyProject.getProperties();
        if ( null != ps )
        {
            String targetDir = ps.getProperty( "scm.dependencies.source.targetDir" );
            if ( ! StringUtils.isEmpty( targetDir ) )
            {
                external = buildExternalEntryFromProvidedInfos( targetDir, artifact,
                    dependencyProject, dependencySvnInfo, rootSvnInfo );
                if ( null != external )
                {
                    getLog().info( "Dependency " + artifactToString( artifact )
                        + " external entry computed with 'scm.dependencies.source.targetDir' property is : "
                        + external.toString() );
                }
            }
            else
            {
                getLog().warn( "Maven project of dependency " + artifactToString( artifact )
                    + " do not define property 'scm.dependencies.source.targetDir'" );
            }
        }
        else
        {
            getLog().warn( "Maven project of dependency " + artifactToString( artifact ) + " has no properties" );
        }
        return external;
    }
    
    ExternalEntry buildCurrentDependencyExternalDefault( Artifact artifact, MavenProject dependencyProject,
       SvnInfo dependencySvnInfo, SvnInfo rootSvnInfo )
    {
        ExternalEntry external = null;
        // Option 1 : use artifact.getGroupId().artifact.getArtifactId()
        String targetDir = dependencyProject.getGroupId() + "." + dependencyProject.getArtifactId();
        // replace prefix while target dir is in "dot form"
        if ( null != sourceTargetDirRemovePrefixes )
        {
            for ( String sourceTargetDirRemovePrefix : sourceTargetDirRemovePrefixes )
            {
                targetDir = targetDir.replaceFirst( "^" + Pattern.quote( sourceTargetDirRemovePrefix ), "" );
            }
        }
        targetDir = targetDir.replaceAll( Pattern.quote( "." ), "/" );
        
        external = buildExternalEntryFromProvidedInfos( targetDir, artifact,
            dependencyProject, dependencySvnInfo, rootSvnInfo );
        if ( null != external )
        {
            getLog().info( "Dependency " + artifactToString( artifact ) + " defaut computed external entry is : "
                + external.toString() );
        }
        return external;
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
        ExternalsEntries externalsEntries = null;
        File targetSourceDir = null;

        for ( Artifact artifact : dss.getResolvedDependencies() )
        {
            String dependencyStr = artifactToString( artifact );
          
            MavenProject dependencyProject = buildProjectFromArtifact( artifact );
            Scm scm = dependencyProject.getScm();

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
                if ( null == externalsSvnInfo || null == externalsEntries )
                {
                    targetSourceDir = new File( basedir.toString() + File.separator + sourceSubdir );
                    if ( ! targetSourceDir.isDirectory() )
                    {
                        throw new MojoExecutionException( "Svn externals dir does not exists or is not a directory : "
                            + targetSourceDir );
                    }
                    
                    externalsSvnInfo = getSvnInfo( targetSourceDir.getAbsolutePath() );
                    
                    if ( ! externalsSvnInfo.isValide() )
                    {
                        throw new MojoExecutionException( "Svn info not available for externals dir : "
                            + targetSourceDir );
                    }
                                     
                    getLog().info( "Svn externals dir is '" + externalsSvnInfo.toString() + "'" );
                    
                    externalsEntries = loadSvnExternals( targetSourceDir.getAbsolutePath() );
                    // externalsEntries not null here
                    
                    getLog().info( "Svn initial externals are '" + externalsEntries.values().toString() + "'" );
                }
                
                SvnInfo dependencySvnInfo = getSvnInfo( dependencySvnUri );
                
                if ( ! dependencySvnInfo.isValide() )
                {
                    throw new MojoExecutionException( "Svn info not available for dependency : "
                        + dependencyStr + " at " + dependencySvnUri );
                }
                getLog().info( "Svn info for dependency : " + dependencyStr + " are " + dependencySvnInfo.toString() );
                              
                ExternalEntry ee = buildCurrentDependencyExternalFromPluginsConfig( artifact, dependencyProject,
                         dependencySvnInfo, externalsSvnInfo );
                if ( null == ee )
                {
                    ee = buildCurrentDependencyFromDependencieConfig( artifact, dependencyProject,
                         dependencySvnInfo, externalsSvnInfo );
                }
                if ( null == ee )
                {
                    ee = buildCurrentDependencyExternalDefault( artifact, dependencyProject, dependencySvnInfo,
                          externalsSvnInfo );
                }
                
                if ( null != ee )
                {
                    externalsEntries.put( ee );
                }
                else
                {
                    throw new MojoExecutionException( "Unable to build external for : " + dependencyStr );
                }
            } 
            else
            {
                throw new MojoExecutionException( "SCM unsupported yet : " + scmType[1] );
            }
        }
        if ( null != externalsEntries && null != targetSourceDir )
        {
            writeSvnExternals( targetSourceDir.getAbsolutePath(), externalsEntries );
        }
    }
}
