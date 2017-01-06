package org.apache.maven.plugin.cxx.utils.release;

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

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.nio.file.Path;
import java.net.URL;
import java.net.MalformedURLException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.StringUtils;
import org.apache.maven.plugin.cxx.utils.Credential;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.apache.maven.scm.provider.ScmUrlUtils;
import org.apache.maven.plugin.cxx.utils.svn.SvnService;
import org.apache.maven.plugin.cxx.utils.svn.SvnInfo;
import org.apache.maven.artifact.ArtifactUtils;

/**
 * 
 * @author Franck Bonin
 * 
 * @plexus.component role="org.apache.maven.plugin.cxx.utils.release.CxxReleasePhase" role-hint="PreCheckPomPhase"
 */
public class PreCheckPomPhase
    extends CxxAbstractMavenReleasePluginPhase
{
   
    /**
     * The decrypter for passwords.
     * 
     * When this plugin requires Maven 3.0 as minimum, this component can be removed and o.a.m.s.c.SettingsDecrypter be
     * used instead.
     * component loaded with plexus, see components.xml
     */
    private SecDispatcher secDispatcher;
   
    @Override
    public ReleaseResult run( CxxReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects )
        throws MojoExecutionException, MojoFailureException
    {
        // checkPomPhase may fillup an empty scmId, prevent it !
        String previousScmId = releaseDescriptor.getScmId();
        
        ReleaseResult result = super.run( releaseDescriptor, releaseEnvironment,  reactorProjects );
        
        // restore scmId, if empty
        if ( StringUtils.isEmpty( releaseDescriptor.getScmId() ) )
        {
            releaseDescriptor.setScmId( previousScmId );
        }
        
        /*for ( MavenProject project : reactorProjects )
        {
            if ( ArtifactUtils.isSnapshot( project.getVersion() ) )
            {
                logInfo( result, "is Snapshot = " + ArtifactUtils.versionlessKey( project.getArtifact() ) );
            }
            else
            {
                logInfo( result, "is not a Snapshot = " + ArtifactUtils.versionlessKey( project.getArtifact() ) );
            }
        }*/        
        
        logInfo( result, "SCM URL selected : " + releaseDescriptor.getScmSourceUrl() );
        
        //1/ resolve scm credential !
        Credential credential = Credential.createCredential(
                    ScmUrlUtils.getProviderSpecificPart( releaseDescriptor.getScmSourceUrl() ),
            /*use org.apache.maven.scm.provider.ScmUrlUtils from maven-scm-api artifact
            to extract uri from releaseDescriptor.getScmSourceUrl(),*/
                    releaseDescriptor.getScmUsername(),
                    releaseDescriptor.getScmPassword(),
                    releaseDescriptor.getScmId(),
                    releaseEnvironment.getSettings(),
                    secDispatcher,
                    getLog() );
                    
        releaseDescriptor.setScmUsername( credential.getUsername() );
        releaseDescriptor.setScmPassword( credential.getPassword() );
        logInfo( result, "SCM credential selected : " + credential.getUsername() 
            + ( StringUtils.isEmpty( credential.getUsername() ) ? "" : "/XXXXXXXX" ) );
        
        // 2/ check basedir is in scm (svn info, see cxx)
        //    check all reactor project baseDir ?
        
        // choose which basedir to trust !
        /*
         * basedir = ReleaseUtil.getRootProject( reactorProjects ).getBasedir().getAbsolutePath();
         * basedir = releaseDescriptor.getWorkingDirectory();
         * basedir = ReleaseUtil.getCommonBasedir( reactorProjects );
         */ 
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        File rootBasedir = rootProject.getBasedir();
        Path rootPath = rootBasedir.toPath();
        String rootProjectId =
                ArtifactUtils.versionlessKey( rootProject.getGroupId(), rootProject.getArtifactId() );
        String rootScmProvider = ScmUrlUtils.getProvider( releaseDescriptor.getScmSourceUrl() );
        SvnInfo rootBasedirSvnInfo = new SvnInfo();
        Path rootSvnDir = null;
        if ( StringUtils.equalsIgnoreCase( "svn", rootScmProvider ) )
        {
            // svn case
            rootBasedirSvnInfo = SvnService.getSvnInfo( rootBasedir, null,
                rootBasedir.getAbsolutePath(), getLog(), true );
            if ( !rootBasedirSvnInfo.isValide() )
            {
                throw new MojoFailureException( "Main project \"" + rootProjectId + "\" local directory ("
                    + rootBasedir + ") is not under scm control, while scm (" + rootScmProvider + ") claimed to be " );
            }
            else
            {
                try 
                {
                    rootSvnDir = new File( new URL( rootBasedirSvnInfo.getSvnUrl() ).getPath() ).toPath();
                }
                catch ( MalformedURLException e )
                {
                }
                logInfo( result, "Main project \"" + rootProjectId + "\" local directory (" + rootBasedir 
                    + ") is under SCM configuration" );
            }
        }
        else
        {
            logInfo( result, "Main project SCM concistancy check not supported for \"" + rootScmProvider + "\"" );
            //throw new MojoFailureException( scmProvider + " not yet supported" ); 
        }
        
        List<MavenProject> projectToExclude = new ArrayList<MavenProject>();
        
        // check dir tree and scm tree
        for ( MavenProject subProject : reactorProjects )
        {

            String subProjectId =
                ArtifactUtils.versionlessKey( subProject.getGroupId(), subProject.getArtifactId() );
                
            /*if (subProjectId.contains( "module" ) )
            {
                logInfo( result, "remove \"" + subProjectId + "\" because !" );
                projectToExclude.add( subProject );
                continue;
            }*/
                
            if ( subProjectId.equals( rootProjectId ) ) 
            {
                logInfo( result, "skip exam of \"" + subProjectId + "\" since it is the root project :\""
                    + rootProjectId + "\"" );
                continue;
            }
            
            File basedir = subProject.getBasedir();
            
            Path path = basedir.toPath();
         
            if ( ! path.startsWith( rootPath ) )
            {
                logInfo( result, "Sub project \"" + subProjectId 
                + "\" excluded since its local directory location is not included under root project local directory ("
                + rootBasedir + ")" );
                projectToExclude.add( subProject );
                continue;     
            }
            else
            {
                logInfo( result, "Sub project \"" + subProjectId + "\" local directory (" + basedir 
                    + ") is under root project local directory (" + rootBasedir + ")" );
            }

            if ( StringUtils.equalsIgnoreCase( "svn", rootScmProvider ) )
            {
                // svn case
                SvnInfo basedirSvnInfo = SvnService.getSvnInfo( basedir, null, basedir.getAbsolutePath(), getLog(), true );
                if ( !basedirSvnInfo.isValide() )
                {
                    logInfo( result, "Sub project \"" + subProjectId 
                        + "\" excluded since its local directory is not under SCM control" );
                    projectToExclude.add( subProject );
                    continue;
                }
                else
                {
                    logInfo( result, "Sub project \"" + subProjectId + "\" local directory (" 
                        + basedir + ") is under SCM control" );
                    if ( rootBasedirSvnInfo.isValide() )
                    {
                        if ( !StringUtils.equalsIgnoreCase( rootBasedirSvnInfo.getSvnRoot(),
                            basedirSvnInfo.getSvnRoot() ) )
                        {
                            logInfo( result, "Sub project \"" + subProjectId
                                + "\" excluded since its svn root differs from root project's one (" 
                                + rootBasedirSvnInfo.getSvnRoot() + ")" );
                            projectToExclude.add( subProject );
                        }
                        else 
                        {
                            logInfo( result, "Sub project \"" + subProjectId + "\" svn root location (" 
                                + rootBasedirSvnInfo.getSvnRoot() + ") is the same as root project's one \"" 
                                +  rootProjectId + "\"" );
                            Path svnDir = null;
                            try
                            {
                                svnDir = new File( new URL( basedirSvnInfo.getSvnUrl() ).getPath() ).toPath();
                            }
                            catch ( MalformedURLException e )
                            {
                            }
                            if ( null != rootSvnDir && null != svnDir )
                            {
                                if ( !svnDir.startsWith( rootSvnDir ) )
                                {
                                    logInfo( result, "Sub project \"" + subProjectId 
                                        + "\" excluded since its svn location (" + svnDir 
                                        + ") is not included under root project svn location (" +  rootSvnDir + ")"  );
                                    projectToExclude.add( subProject );
                                    continue;     
                                }
                                else
                                {
                                    logInfo( result, "Sub project \"" + subProjectId + "\" svn location ("
                                        + svnDir + ") is under root project svn location (" +  rootSvnDir + ")" );
                                }
                            }
                        }
                    }
                }
            }
        }
        
        for ( MavenProject subProject : projectToExclude )
        {
            
            reactorProjects.remove( subProject );
        } 
        
        return result;
    }
}
