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
@Mojo( name = "release", aggregator = true )
public class PerformReleaseMojo
    extends AbstractPerformBranchReleaseMojo
{
  
    /**
     * Default version to use when preparing a release.
     *
     * @since 0.0.6
     */
    @Parameter( property = "releaseVersion" )
    private String releaseVersion;
    
 
    /**
     * The tag base directory in SVN, you must define it if you don't use the standard svn layout (trunk/tags/branches).
     * For example, <code>http://svn.apache.org/repos/asf/maven/plugins/tags</code>. The URL is an SVN URL and does not
     * include the SCM provider and protocol.
     */
    @Parameter( property = "tagBase" )
    protected String tagBase;
    
    /**
     * Format to use when generating the tag name if none is specified. Property interpolation is performed on the
     * tag, but in order to ensure that the interpolation occurs during release, you must use <code>@{...}</code>
     * to reference the properties rather than <code>${...}</code>. The following properties are available:
     * <ul>
     *     <li><code>groupId</code> or <code>project.groupId</code> - The groupId of the root project.
     *     <li><code>artifactId</code> or <code>project.artifactId</code> - The artifactId of the root project.
     *     <li><code>version</code> or <code>project.version</code> - The release version of the root project.
     * </ul>
     *
     * @since 0.0.6
     */
    @Parameter( defaultValue = "@{project.artifactId}-@{project.version}", property = "tagNameFormat" )
    private String tagNameFormat;
    
    /**
     * The SCM tag to use.
     */
    @Parameter( property = "releaseLabel", alias = "tag" )
    private String releaseLabel;
    
    @Override
    protected CxxReleaseDescriptor createReleaseDescriptor()
    {
        CxxReleaseDescriptor descriptor = super.createReleaseDescriptor();
        // Our release descriptor doesn't think, it carries our data, in their inital form
        descriptor.setScmReleaseLabel( releaseLabel );
        descriptor.setScmTagNameFormat( tagNameFormat );
        descriptor.setScmTagBase( tagBase );
        //descriptor.setCommitByProject( commitByProject ); // TODO ?
        //descriptor.setAllowTimestampedSnapshots( allowTimestampedSnapshots );// TODO ?
        descriptor.setDefaultReleaseVersion( releaseVersion ); // version to tag
        
        /*descriptor.setScmCommentPrefix( scmCommentPrefix );
        descriptor.setPushChanges( pushChanges );*/

        return descriptor;
    }
    
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        releaseManager.setLog( getLog() );
        
        releaseManager.release( createReleaseDescriptor(), getReleaseEnvironment(), reactorProjects );
    }
}
