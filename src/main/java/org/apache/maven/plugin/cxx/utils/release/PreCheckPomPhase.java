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
import java.io.File;
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
        File basedir = ReleaseUtil.getRootProject( reactorProjects ).getBasedir();
        String scmProvider = ScmUrlUtils.getProvider( releaseDescriptor.getScmSourceUrl() );
        if ( StringUtils.equalsIgnoreCase( "svn", scmProvider ) )
        {
            // svn case
            SvnInfo basedirSvnInfo = SvnService.getSvnInfo( basedir, null, basedir.getAbsolutePath(), getLog(), true );
            if ( !basedirSvnInfo.isValide() )
            {
                throw new MojoFailureException( basedir + " is not under scm control" ); 
            }
            else
            {
                logInfo( result, "Base directory " + basedir.getAbsolutePath() + " is under SCM configuration" );
            }
        }
        else
        {
            throw new MojoFailureException( scmProvider + " not yet supported" ); 
        }
        
        return result;
    }
}
