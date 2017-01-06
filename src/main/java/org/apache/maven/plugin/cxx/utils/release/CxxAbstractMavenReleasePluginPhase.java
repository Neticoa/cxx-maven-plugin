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
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * 
 *
 * @author Franck bonin
 */
public abstract class CxxAbstractMavenReleasePluginPhase
    extends CxxAbstractReleasePhase
{
    /**
     * maven release Phase
     * component loaded with plexus, see components.xml
     */
    private org.apache.maven.shared.release.phase.ReleasePhase mavenReleasePhase;
  
    @Override
    public ReleaseResult run( CxxReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects )
        throws MojoExecutionException, MojoFailureException
    {
        ReleaseResult result = new ReleaseResult();
        
        try
        {   
            if ( null != mavenReleasePhase )
            {                
                if ( releaseDescriptor.getDryRun() )
                {
                     result = mavenReleasePhase.simulate( releaseDescriptor, releaseEnvironment, reactorProjects );
                }
                else
                {
                     result = mavenReleasePhase.execute( releaseDescriptor, releaseEnvironment, reactorProjects );
                }
            }
            else
            {
                result = getReleaseResultSuccess();
            }
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.toString() );
        }
        catch ( ReleaseFailureException e )
        {
            throw new MojoFailureException( e.toString() );
        }
        
        return result;
    }
    
    public ReleaseResult clean( List<MavenProject> reactorProjects )
    {
        if ( null != mavenReleasePhase )
        {
            return mavenReleasePhase.clean( reactorProjects );
        }
        else
        {
            return getReleaseResultSuccess();
        }
    }
}
