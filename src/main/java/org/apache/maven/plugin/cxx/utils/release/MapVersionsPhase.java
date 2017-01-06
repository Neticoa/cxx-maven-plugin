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
import org.apache.maven.shared.release.policy.version.VersionPolicy;
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
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Robert Scholte
 */
public class MapVersionsPhase
    extends CxxAbstractMavenReleasePluginPhase
{
    private ResourceBundle resourceBundle;

    /**
     * Whether to convert input versions to a snapshot or a release versions.
     */
    //private boolean convertToSnapshot;

    /**
     * branch special case.
     */
    //private boolean convertToBranch;
    
    enum Mode { release, branch, dev };  
    
    private Mode mode = Mode.release;

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

        if ( releaseDescriptor.isAutoVersionSubmodules() && ArtifactUtils.isSnapshot( rootProject.getVersion() ) )
        {
            // get the root project
            MavenProject project = rootProject;

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            String nextVersion = resolveNextVersion( project, projectId, releaseDescriptor, result );

            logInfo( result, "Sub-Module Auto Versionning mode since main component had a Snapshot version ("
                    + rootProject.getVersion() + ")" );
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
            if (mode == Mode.dev)
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
                String v;
                if ( ArtifactUtils.isSnapshot( subProject.getVersion() ) )
                {
                    v = nextVersion;
                }
                else
                {
                    v = subProject.getVersion();
                }
                if (mode == Mode.dev )
                {
                    logInfo( result, projectId + " next development version will be " + nextVersion );
                    releaseDescriptor.mapDevelopmentVersion( projectId, v );
                }
                else if (mode == Mode.branch )
                {
                    logInfo( result, projectId + " next development version will be " + nextVersion );
                    releaseDescriptor.mapReleaseVersion( projectId, v );
                }
                else
                {
                    logInfo( result, "()" + projectId + " next version will be " + nextVersion );
                    releaseDescriptor.mapReleaseVersion( subProjectId, nextVersion );
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
                if (mode == Mode.dev)
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
        String defaultVersion;
        if ( mode == Mode.branch ) //( convertToBranch )
        {
            // no branch modification
            //if ( !( releaseDescriptor.isUpdateBranchVersions()
            //                && ( ArtifactUtils.isSnapshot( project.getVersion() )
            //                                || releaseDescriptor.isUpdateVersionsToSnapshot() ) ) )
            if ( !releaseDescriptor.isUpdateBranchVersions() )
            {
                logInfo( result, "next branch version found for " + projectId + " : " + project.getVersion() );
                return project.getVersion();
            }

            defaultVersion = getReleaseVersion( projectId, releaseDescriptor );
        }
        else if ( mode == Mode.release ) //( !convertToSnapshot ) // map-release-version
        {
            defaultVersion = getReleaseVersion( projectId, releaseDescriptor );
        }
        /*else if ( releaseDescriptor.isBranchCreation() ) // mode ==  Mode.dev
        {
            // no working copy modification
            if ( !( ArtifactUtils.isSnapshot( project.getVersion() )
                          && releaseDescriptor.isUpdateWorkingCopyVersions() ) )
            {
                logInfo( result, "next dev version (branch cycle) found for " + projectId + " : " + project.getVersion() );
                return project.getVersion();
            }

            defaultVersion = getDevelopmentVersion( projectId, releaseDescriptor );
        }*/
        else // mode ==  Mode.dev
        {
            // no working copy modification
            if ( !( releaseDescriptor.isUpdateWorkingCopyVersions() ) )
            {
                logInfo( result, "next dev version found for " + projectId + " : " + project.getVersion() );
                return project.getVersion();
            }

            defaultVersion = getDevelopmentVersion( projectId, releaseDescriptor );
        }
        //@todo validate default version, maybe with DefaultArtifactVersion

        
        String suggestedVersion = null;
        String nextVersion = defaultVersion;
        String messageKey = null;
        logInfo( result, "next version candidate for " + projectId + " : " + nextVersion );
        
        boolean nextSnapShot = nextVersion != null && mode == Mode.branch && releaseDescriptor.isUpdateVersionsToSnapshot();
        try
        {
            boolean snapshotNeeded = ( mode == Mode.dev && releaseDescriptor.isSnapshotDevelopmentVersion() )
                || ( mode == Mode.branch && releaseDescriptor.isUpdateVersionsToSnapshot() );
            //while ( nextVersion == null || ArtifactUtils.isSnapshot( nextVersion ) != convertToSnapshot )
            while ( nextVersion == null || ( ArtifactUtils.isSnapshot( nextVersion ) != snapshotNeeded ) )
            {
                if ( suggestedVersion == null )
                {
                    String baseVersion = null;
                    //if ( convertToSnapshot )
                    if ( mode != Mode.release )
                    {
                        baseVersion = getReleaseVersion( projectId, releaseDescriptor );
                    }
                    // unspecified and unmapped version, so use project version
                    if ( baseVersion == null )
                    {
                        baseVersion = project.getVersion();
                    }

                    try
                    {
                        try
                        {
                            suggestedVersion =
                                resolveSuggestedVersion( snapshotNeeded, nextSnapShot,
                                    baseVersion, releaseDescriptor.getProjectVersionPolicyId() );
                        }
                        catch ( VersionParseException e )
                        {
                            if ( releaseDescriptor.isInteractive() )
                            {
                                suggestedVersion =
                                    resolveSuggestedVersion( snapshotNeeded, nextSnapShot,
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

    private String resolveSuggestedVersion(boolean convertToSnapshot, boolean nextSnapShot, String baseVersion, String policyId )
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
