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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.cxx.utils.release.CxxReleaseDescriptor;

 /**
 * Branch a project in SCM
 *
 * @author 
 * @version 
 * @since 0.0.6
 */
@Mojo( name = "branch", aggregator = true )
public class PerformBranchMojo
    extends AbstractPerformBranchReleaseMojo
{
    /**
     * Whether to update versions in the working copy.
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "false", property = "updateWorkingCopyVersions" )
    private boolean updateWorkingCopyVersions;
  
    /**
     * Whether to update versions in the branch.
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "true", property = "updateBranchVersions" )
    private boolean updateBranchVersions;
    
    /**
     * Specify the new version for the branch. 
     * This parameter is only meaningful if {@link #updateBranchVersions} = {@code true}.
     *
     * @since 0.0.6
     */
    @Parameter( property = "branchVersion" )
    private String branchVersion;
    
    /**
     * The branch name to use.
     *
     * @required
     * @since 0.0.6
     */
    @Parameter( property = "branchLabel", alias = "branchName", required = true )
    private String branchLabel;
    
    /**
     * The branch base directory in SVN, you must define it if you don't use the standard svn layout
     * (trunk/tags/branches). For example, <code>http://svn.apache.org/repos/asf/maven/plugins/branches</code>. The URL
     * is an SVN URL and does not include the SCM provider and protocol.
     *
     * @since 0.0.6
     */
    @Parameter( property = "branchBase" )
    protected String branchBase;
        
    /**
     * Whether to update to SNAPSHOT versions in the branch.
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "false", property = "updateVersionsToSnapshot" )
    protected boolean updateVersionsToSnapshot;
    
    @Override
    protected CxxReleaseDescriptor createReleaseDescriptor()
    {
        CxxReleaseDescriptor descriptor = super.createReleaseDescriptor();
        // Our release descriptor doesn't think, it carries our data, in their inital form
        descriptor.setScmReleaseLabel( branchLabel );
        descriptor.setScmBranchBase( branchBase );
        descriptor.setUpdateVersionsToSnapshot( updateVersionsToSnapshot );
        descriptor.setBranchCreation( true );
        descriptor.setDefaultReleaseVersion( branchVersion ); // version to branch
        descriptor.setUpdateBranchVersions( updateBranchVersions ); // needed by defaultReleaseVersion
        descriptor.setUpdateWorkingCopyVersions( updateWorkingCopyVersions ); // activate defaultDevelopmentVersion
        
        return descriptor;
    }
    
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        releaseManager.setLog( getLog() );
        
        releaseManager.branch( createReleaseDescriptor(), getReleaseEnvironment(), reactorProjects );
    }
}
