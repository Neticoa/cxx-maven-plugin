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

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Map projects to their new versions after release / into the next development cycle.
 *
 * The map-phases per goal are:
 * <dl>
 *  <dt>release:prepare</dt><dd>map-release-versions + map-development-versions; RD.isBranchCreation() = false</dd>
 *  <dt>release:branch</dt><dd>map-branch-versions + map-development-versions; RD.isBranchCreation() = true</dd>
 *  <dt>release:update-versions</dt><dd>map-development-versions; RD.isBranchCreation() = false</dd>
 * </dl>
 *
 * <p>
 * <table>
 *   <tr>
 *     <th>MapVersionsPhase field</th><th>map-release-versions</th><th>map-branch-versions</th>
 *     <th>map-development-versions</th>
 *   </tr>
 *   <tr>
 *     <td>convertToSnapshot</td>     <td>false</td>               <td>true</td>               <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>convertToBranch</td>       <td>false</td>               <td>true</td>               <td>false</td>
 *   </tr>
 * </table>
 *
 * @author Franck Bonin
 */
public class MapVersionsPhase
    extends CxxAbstractMavenReleasePluginPhase
{
    private ResourceBundle resourceBundle;
    
    enum Mode
    { 
        release,
        branch,
        dev
    };
    
    private Mode mode = Mode.release;
    
    
    public void setMode( String mode )
    {
        this.mode = Mode.valueOf( mode );
    }

    /**
     * Component used to prompt for input.
     */
    private Prompter prompter;

    /**
     * Component used for custom or default version policy
     */
    private Map<String, CxxVersionPolicy> versionPolicies;

    void setPrompter( Prompter prompter )
    {
        this.prompter = prompter;
    }

    @Override
    public ReleaseResult run( CxxReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                           List<MavenProject> reactorProjects )
        throws MojoExecutionException, MojoFailureException
    {
        ReleaseResult result = new ReleaseResult();
        
        logInfo( result, "Mode is " + mode.name() ) ;

        resourceBundle = getResourceBundle( releaseEnvironment.getLocale() );

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );

        //if ( releaseDescriptor.isAutoVersionSubmodules() && ArtifactUtils.isSnapshot( rootProject.getVersion() ) )
        if ( releaseDescriptor.isAutoVersionSubmodules() )
        {
            logInfo( result, "Sub-Module Auto Versionning mode" );
            // get the root project
            MavenProject project = rootProject;

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            String nextVersion = resolveNextVersion( project, projectId, releaseDescriptor, result );

            /*if ( convertToSnapshot )
            {
                if ( releaseDescriptor.isBranchCreation() && convertToBranch )
                {
                    logInfo( result, "(branch & snapshot) " + projectId + " next version will be " + nextVersion );
                    releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
                }
                else
                {
                    logInfo( result, "(snapshot) " + projectId + " next development version will be " + nextVersion );
                    releaseDescriptor.mapDevelopmentVersion( projectId, nextVersion );
                }
            }
            else
            {
                logInfo( result, projectId + " next version will be " + nextVersion );
                releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
            }*/
            if ( mode == Mode.dev )
            {
                logInfo( result, projectId + " next development version will be " + nextVersion );
                releaseDescriptor.mapDevelopmentVersion( projectId, nextVersion );
            }
            else
            {
                logInfo( result, projectId + " next version will be " + nextVersion );
                releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
            }

            for ( MavenProject subProject : reactorProjects )
            {
                String subProjectId =
                    ArtifactUtils.versionlessKey( subProject.getGroupId(), subProject.getArtifactId() );
                    
                if ( subProjectId.equals( projectId ) ) 
                {
                    logInfo( result, "skip re-versionning \"" + subProjectId 
                        + "\" since it is the main root project :\"" + projectId + "\"" );
                    continue;
                }

                /*if ( convertToSnapshot )
                {
                    String v;
                    if ( ArtifactUtils.isSnapshot( subProject.getVersion() ) )
                    {
                        v = nextVersion;
                    }
                    else
                    {
                        v = subProject.getVersion();
                    }

                    if ( releaseDescriptor.isBranchCreation() && convertToBranch )
                    {
                        logInfo( result, "(branch & snapshot) " + projectId + " next version will be " + v );
                        releaseDescriptor.mapReleaseVersion( subProjectId, v );
                    }
                    else
                    {
                        logInfo( result, "(snapshot) " + projectId + " next development version will be " + v );
                        releaseDescriptor.mapDevelopmentVersion( subProjectId, v );
                    }
                }
                else
                {
                    logInfo( result, "()" + projectId + " next version will be " + nextVersion );
                    releaseDescriptor.mapReleaseVersion( subProjectId, nextVersion );
                }*/
                String v = nextVersion;
                if ( !ArtifactUtils.isSnapshot( subProject.getVersion() ) )
                // Todo : other reasons for subProjectId version to (not) be updated ?
                {
                    v = subProject.getVersion();
                }
                if ( mode == Mode.dev )
                {
                    logInfo( result, subProjectId + " next development version will be " + v );
                    releaseDescriptor.mapDevelopmentVersion( subProjectId, v );
                }
                else 
                {
                    logInfo( result, subProjectId + " next branch version will be " + v );
                    releaseDescriptor.mapReleaseVersion( subProjectId, v );
                }
            }
        }
        else
        {
            logInfo( result, "Sub-Module Normal Versionning mode" );
            for ( MavenProject project : reactorProjects )
            {
                String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                String nextVersion = resolveNextVersion( project, projectId, releaseDescriptor, result );

                /*if ( convertToSnapshot )
                {
                    if ( releaseDescriptor.isBranchCreation() && convertToBranch )
                    {
                        logInfo( result,  + projectId + " next version will be " + nextVersion );
                        releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
                    }
                    else
                    {
                        logInfo( result, projectId + " next development version will be " + nextVersion );
                        releaseDescriptor.mapDevelopmentVersion( projectId, nextVersion );
                    }
                }
                else
                {
                    logInfo( result, projectId + " next version will be " + nextVersion );
                    releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
                }*/
                if ( mode == Mode.dev )
                {
                    logInfo( result, projectId + " next development version will be " + nextVersion );
                    releaseDescriptor.mapDevelopmentVersion( projectId, nextVersion );
                }
                else
                {
                    logInfo( result, projectId + " next version will be " + nextVersion );
                    releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
                }
            }
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private String resolveNextVersion( MavenProject project,
                                   String projectId,
                                   CxxReleaseDescriptor releaseDescriptor,
                                   ReleaseResult result )
        throws MojoExecutionException
    {
        String nextVersion;
        
        /* flags to take care of are
        * in release Mode :
        *    none
        * 
        * in branch Mode :
        *   P1 :updateBranchVersions
        *   P2 :updateVersionsToSnapshot
        * 
        * in dev mode :
        *   P1 : updateWorkingCopyVersions
        *   P2 : snapshotDevelopmentVersion
        *   P3 : branchCreation
        *
        * Not at this level : autoVersionSubmodules
        */
        
        boolean snapshotNeeded = false;
        boolean snapShotOfCurrentVersion = false;
        if ( mode == Mode.release )
        {
            logInfo( result, "search for a previously provided release version for " + projectId );
            nextVersion = getReleaseVersion( projectId, releaseDescriptor );
        }
        else if ( mode == Mode.branch )
        {
            // no branch version modification
            if ( !releaseDescriptor.isUpdateBranchVersions() )
            {
                logInfo( result, "next branch version for " + projectId
                    + " shall stay current version : " + project.getVersion() );
                return project.getVersion();
            }
            logInfo( result, 
                "UpdateBranchVersion flag set, search for a previously provided branch version for "
                + projectId );
            
            snapshotNeeded = releaseDescriptor.isUpdateVersionsToSnapshot();

            nextVersion = getReleaseVersion( projectId, releaseDescriptor );
            
            snapShotOfCurrentVersion = nextVersion != null && releaseDescriptor.isUpdateVersionsToSnapshot();
        }
        else // if mode == Mode.dev
        {
            // no working copy modification
            if ( !releaseDescriptor.isUpdateWorkingCopyVersions() )
            {
                logInfo( result, "next dev version for " + projectId 
                    + " shall stay current version : " + project.getVersion() );
                return project.getVersion();
            }
            logInfo( result,
                "UpdateWorkingCopyVersions flag set, search for a previously provided dev version for "
                + projectId );
            
            snapshotNeeded = releaseDescriptor.isSnapshotDevelopmentVersion();
            
            nextVersion = getDevelopmentVersion( projectId, releaseDescriptor );
            
            snapShotOfCurrentVersion = nextVersion != null && snapshotNeeded;
            
            // no working copy modification special case
            if ( releaseDescriptor.isBranchCreation() && nextVersion != null
                 && ArtifactUtils.isSnapshot( nextVersion ) == snapshotNeeded )
            {
                logInfo( result, "next dev version for " + projectId + " is correctly provided : " + nextVersion );
                return nextVersion;
            }
        }
        
        String defaultNextVersion = nextVersion;
        String suggestedVersion = null;
        String messageKey = null;
        logInfo( result, "next version candidate found for " + projectId + " : " + nextVersion );
        
        // shall suggestedVersion be just snapshot of current version ? (and not next version snapshot)
        logInfo( result, "snapShotOfCurrentVersion ? " + snapShotOfCurrentVersion );
        logInfo( result, "snapshotNeeded ? " + snapshotNeeded );
        try
        {
            //while ( nextVersion == null || ArtifactUtils.isSnapshot( nextVersion ) != convertToSnapshot )
            while ( nextVersion == null || ( ArtifactUtils.isSnapshot( nextVersion ) != snapshotNeeded ) )
            {
                logInfo( result, "suggested nextVersion is : " + nextVersion );
                logInfo( result, "nextVersion is snapShot ? " + ArtifactUtils.isSnapshot( nextVersion ) );
                if ( suggestedVersion == null )
                {
                    //String baseVersion = null;
                    String baseVersion = defaultNextVersion;
                    /*
                    //if ( convertToSnapshot )
                    if ( mode != Mode.release )
                    {
                        baseVersion = getReleaseVersion( projectId, releaseDescriptor );
                    }
                    */
                    // unspecified and unmapped version, so use project version
                    if ( baseVersion == null )
                    {
                        baseVersion = project.getVersion();
                        logInfo( result, 
                            "unspecified and unmapped version, so use project version as base for suggestion : "
                            + baseVersion );
                    }

                    try
                    {
                        try
                        {
                            suggestedVersion =
                                resolveSuggestedVersion( snapshotNeeded, snapShotOfCurrentVersion,
                                    baseVersion, releaseDescriptor.getProjectVersionPolicyId() );
                        }
                        catch ( VersionParseException e )
                        {
                            if ( releaseDescriptor.isInteractive() )
                            {
                                suggestedVersion =
                                    resolveSuggestedVersion( snapshotNeeded, snapShotOfCurrentVersion,
                                        "1.0", releaseDescriptor.getProjectVersionPolicyId() );
                            }
                            else
                            {
                                throw new MojoExecutionException( "Error parsing version, cannot determine next "
                                    + "version: " + e.getMessage(), e );
                            }
                        }
                    }
                    catch ( PolicyException e )
                    {
                        throw new MojoExecutionException( e.getMessage(), e );
                    }
                    catch ( VersionParseException e )
                    {
                        throw new MojoExecutionException( e.getMessage(), e );
                    }
                }

                if ( releaseDescriptor.isInteractive() )
                {
                    if ( messageKey == null )
                    {
                        messageKey = getMapversionPromptKey( releaseDescriptor );
                    }
                    String message =
                        MessageFormat.format( resourceBundle.getString( messageKey ), project.getName(), projectId );
                    nextVersion = prompter.prompt( message, suggestedVersion );

                  //@todo validate next version, maybe with DefaultArtifactVersion
                }
                else
                {
                    nextVersion = suggestedVersion;
                }
            }
        }
        catch ( PrompterException e )
        {
            throw new MojoExecutionException( "Error reading version from input handler: " + e.getMessage(), e );
        }
        return nextVersion;
    }

    private String resolveSuggestedVersion( boolean convertToSnapshot, boolean nextSnapShot,
        String baseVersion, String policyId )
        throws PolicyException, VersionParseException
    {
        CxxVersionPolicy policy = versionPolicies.get( policyId );
        VersionPolicyRequest request = new VersionPolicyRequest().setVersion( baseVersion );

        //return convertToSnapshot ? policy.getDevelopmentVersion( request ).getVersion()
        //                : policy.getReleaseVersion( request ).getVersion();
        return nextSnapShot ? policy.getSnapshotVersion( request ).getVersion() // next snapshot version only
            : convertToSnapshot ? policy.getDevelopmentVersion( request ).getVersion() // current snapshot version
            : ( mode == Mode.dev ) ? policy.getBranchVersion( request ).getVersion() // no snapshot, just next version
            : ( mode == Mode.branch ) ? policy.getBranchVersion( request ).getVersion()
            : policy.getReleaseVersion( request ).getVersion(); // no snapshot, just cleaned-up current version
    }

    private String getDevelopmentVersion( String projectId, ReleaseDescriptor releaseDescriptor )
    {
        String defaultVersion = releaseDescriptor.getDefaultDevelopmentVersion();
        if ( StringUtils.isEmpty( defaultVersion ) )
        {
            defaultVersion = ( String ) releaseDescriptor.getDevelopmentVersions().get( projectId );
        }
        return defaultVersion;
    }

    private String getReleaseVersion( String projectId, ReleaseDescriptor releaseDescriptor )
    {
        String nextVersion = releaseDescriptor.getDefaultReleaseVersion();
        if ( StringUtils.isEmpty( nextVersion ) )
        {
            nextVersion = ( String ) releaseDescriptor.getReleaseVersions().get( projectId );
        }
        return nextVersion;
    }

    private String getMapversionPromptKey( ReleaseDescriptor releaseDescriptor )
    {
        String messageKey;
        if ( mode == Mode.branch )//( convertToBranch )
        {
            messageKey = "mapversion.branch.prompt";
        }
        else if ( mode == Mode.dev )//( convertToSnapshot )
        {
            if ( releaseDescriptor.isBranchCreation() )
            {
                messageKey = "mapversion.workingcopy.prompt";
            }
            else
            {
                messageKey = "mapversion.development.prompt";
            }
        }
        else
        {
            messageKey = "mapversion.release.prompt";
        }
        return messageKey;
    }
}
