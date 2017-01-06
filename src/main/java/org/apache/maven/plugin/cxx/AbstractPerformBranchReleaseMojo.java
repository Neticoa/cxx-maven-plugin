package org.apache.maven.plugin.cxx;

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
 
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.Component;
import java.io.File;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.plugin.cxx.utils.release.CxxReleaseDescriptor;
import org.apache.maven.plugin.cxx.utils.release.CxxReleaseManager;

 /**
 * Branch a project in SCM
 *
 * @author 
 * @version 
 * @since 0.0.6
 */
public abstract class AbstractPerformBranchReleaseMojo
    extends AbstractCxxMojo
{
    /**
     * The {@code M2_HOME} parameter to use for forked Maven invocations.
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "${maven.home}" )
    protected File mavenHome;

    /**
     * The {@code JAVA_HOME} parameter to use for forked Maven invocations.
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "${java.home}" )
    protected File javaHome;

    /**
     * The command-line local repository directory in use for this build (if specified).
     *
     * @since 0.0.6
     */
    @Parameter ( defaultValue = "${maven.repo.local}" )
    protected File localRepoDirectory;
    
    /**********************************************************************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/ 
  
    /**
     * The user name (used by svn).
     * 
     * You may use maven setting to store username. 
     * See http://maven.apache.org/guides/mini/guide-encryption.html
     *
     * @since 0.0.6
     */
    @Parameter( property = "username" )
    protected String username = null;

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
    protected String settingsServerId = null;
    
    /**********************************************************************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    
    /**
     * Dry run: don't checkin or tag anything in the scm repository, or modify the checkout.
     * Running <code>mvn -DdryRun=true cxx:branch</code> is useful in order to check that modifications to
     * poms and scm operations (only listed on the console) are working as expected.
     * Modified POMs are written alongside the originals without modifying them.
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "false", property = "dryRun" )
    protected boolean dryRun;
     
    /**********************************************************************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
       
    /**
     * Specify the new version for the working copy.
     * This parameter is only meaningful if {@link #updateWorkingCopyVersions} = {@code true}.
     *
     * @since 0.0.6
     */
    @Parameter( property = "developmentVersion" )
    private String developmentVersion;
    
    /**
     * Shall developpment version use -SNAPSHOT suffix
     */
    @Parameter( defaultValue = "false", property = "snapshotDevelopmentVersion" )
    protected boolean snapshotDevelopmentVersion = false;
    
    /**
     * new artifact id, if provided
     */
    @Parameter( property = "artifactId" )
    protected String artifactId = null;
       
   /**
     * Whether to automatically assign submodules the parent (main) version. 
     * If set to true, each submodule (if any) will be update to computed main version
     * If set to false :
     *   - the user will be prompted for each submodules version 
     *     if settings.isInteractiveMode()/-B,--batch-mode flag not set
     *   - auto-computed if !settings.isInteractiveMode()/-B,--batch-mode flag set
     * !!PLUS!! :
     * Submodule version are updated only if they are "real" child of main module, according to SCM tree.
     * Aka. Only and only if :
     *   - submodule dir is under scm configuration
     *   - submodule dir is not an external dir
     *   - (futur ?) submodule dir is an external dir AND settings.isInteractiveMode()/-B,--batch-mode flag not set
     *      AND user confirm branch and version update
     *    
     * @since 0.0.6
     */
    @Parameter( defaultValue = "false", property = "autoVersionSubmodules" )
    protected boolean autoVersionSubmodules;
    
    /**
     * Whether to update dependencies version to the next development version.
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "true", property = "updateDependencies" )
    protected boolean updateDependencies;


    /**********************************************************************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    
    /**
     * Whether to add a XML schema to the POM if it was previously missing on.
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "true", property = "addSchema" )
    protected boolean addSchema;
    
    /**
     * The checkout directory.
     */
    @Parameter( defaultValue = "${project.build.directory}/checkout", property = "workingDirectory", required = true )
    protected File workingDirectory;
    
    /**
     *  phases manager.
     */  
    @Component
    protected CxxReleaseManager releaseManager;
    
    /**
     * Gets the enviroment settings configured for this release.
     *
     * @return The release environment, never <code>null</code>.
     */
    protected ReleaseEnvironment getReleaseEnvironment()
    {
        return new DefaultReleaseEnvironment().setSettings( settings )
                                              .setJavaHome( javaHome )
                                              .setMavenHome( mavenHome )
                                              .setLocalRepositoryDirectory( localRepoDirectory )
                                              ; //.setMavenExecutorId( mavenExecutorId );
    }
    
    /**
     * Creates the release descriptor from the various goal parameters.
     *
     * @return The release descriptor, never <code>null</code>.
     */
    protected CxxReleaseDescriptor createReleaseDescriptor()
    {
        // Our release descriptor doesn't think, it carries our data, in their inital form
        CxxReleaseDescriptor descriptor = new CxxReleaseDescriptor();
        
        // -B,--batch-mode flag set weather Maven Run in non-interactive (batch) mode
        descriptor.setInteractive( settings.isInteractiveMode() ); 
        descriptor.setWorkingDirectory( basedir.getAbsolutePath() );
        descriptor.setDryRun( dryRun );
        descriptor.setScmPassword( password );
        descriptor.setScmUsername( username );
        descriptor.setScmId( settingsServerId );
        descriptor.setArtifactId( artifactId );
        descriptor.setAutoVersionSubmodules( autoVersionSubmodules );
        descriptor.setUpdateDependencies( updateDependencies );
        descriptor.setAddSchema( addSchema );
        descriptor.setCheckoutDirectory( workingDirectory.getAbsolutePath() );
        descriptor.setDefaultDevelopmentVersion( developmentVersion ); // version to fallback after tag or branch
        descriptor.setSnapshotDevelopmentVersion( snapshotDevelopmentVersion ); // developmentVersion shall be a SnapShot version
        //descriptor.setProjectVersionPolicyId( projectVersionPolicyId ); // TODO ?

/*        descriptor.setPomFileName( pomFileName );

        List<String> profileIds = getActiveProfileIds();
        String additionalProfiles = getAdditionalProfiles();

        String args = this.arguments;
        if ( !profileIds.isEmpty() || StringUtils.isNotBlank( additionalProfiles ) )
        {
            if ( !StringUtils.isEmpty( args ) )
            {
                args += " -P ";
            }
            else
            {
                args = "-P ";
            }

            for ( Iterator<String> it = profileIds.iterator(); it.hasNext(); )
            {
                args += it.next();
                if ( it.hasNext() )
                {
                    args += ",";
                }
            }
            
            if ( additionalProfiles != null )
            {
                if ( !profileIds.isEmpty() )
                {
                    args += ",";
                }
                args += additionalProfiles;
            }
        }
        descriptor.setAdditionalArguments( args );
*/
        return descriptor;
    }
}
