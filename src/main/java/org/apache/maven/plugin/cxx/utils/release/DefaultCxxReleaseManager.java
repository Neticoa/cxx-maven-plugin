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
import java.util.Map;
import java.util.Collections;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.plugin.logging.Log;

public class DefaultCxxReleaseManager
    implements CxxReleaseManager
{
  
    /**
     * all known phases.
     * component loaded with plexus, see components.xml
     */
    private Map<String, CxxReleasePhase> phases;

    /**
     * The phases to run to perform a branch.
     * list loaded with plexus, see components.xml
     */
    private List<String> branchPhases;

    /**
    * The phases to run to perform a release.
    * list loaded with plexus, see components.xml
    */
    private List<String> releasePhases;
    
    
    private Log log = null;
    
    /**
     *
     * @param log
     */
    public void setLog( Log log )
    {
        this.log = log;
        for ( CxxReleasePhase phase : phases.values() )
        {
            phase.setLog( log );
        }
    }
    
    protected Log getLog()
    {
        return log;
    }
    
    public ReleaseResult branch( CxxReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
          List<MavenProject> reactorProjects )
          throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Running branch cycle ..." );
        return run( branchPhases, releaseDescriptor, releaseEnvironment, reactorProjects );
    }
    
    public ReleaseResult release( CxxReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
          List<MavenProject> reactorProjects )
          throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Running release cycle ..." );
        return run( releasePhases, releaseDescriptor, releaseEnvironment, reactorProjects );
    }
    
    protected ReleaseResult run( List<String> phasesToRun, CxxReleaseDescriptor releaseDescriptor,
              ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects )
              throws MojoExecutionException, MojoFailureException
    {
        ReleaseResult result = new ReleaseResult();
        
        result.setStartTime( System.currentTimeMillis() );
        
        try
        {
            for ( String phaseName : phasesToRun )
            {
                CxxReleasePhase phase = this.phases.get( phaseName );
                
                if ( null != phase )
                {
                    
                    ReleaseResult phaseResult = null;
                    try
                    {
                        getLog().info( "Start " + phaseName +  " phase" );
                        phaseResult = phase.run( releaseDescriptor, releaseEnvironment, reactorProjects );
                    }
                    finally
                    {
                        if ( result != null && phaseResult != null )
                        {
                            result.appendOutput( phaseResult.getOutput() );
                        }
                    }
                }
                else
                {
                    throw new MojoExecutionException( "Try to run " + phaseName 
                        + " but it's not a valide phase name" ); 
                }
            }
            result.setResultCode( ReleaseResult.SUCCESS );
        }
        /*catch ( MojoExecutionException e )
        {
            captureException( result,  e );
        }
        catch ( MojoFailureException e )
        {
            captureException( result,  e );
        }*/
        finally
        {
            result.setEndTime( System.currentTimeMillis() );
            getLog().info( "Cycle end" );
        }
        
        clean( phasesToRun, releaseDescriptor, reactorProjects );
        
        return result;
    }
    
    private void captureException( ReleaseResult result, Exception e )
    {
        result.appendError( e );

        result.setResultCode( ReleaseResult.ERROR );
    }
    
    public void cleanRelease( CxxReleaseDescriptor releaseDescriptor, List<MavenProject> reactorProjects )
    {
        getLog().info( "Cleaning up after release..." );
        
        clean( releasePhases, releaseDescriptor, reactorProjects );
    }
    
    public void cleanBranch( CxxReleaseDescriptor releaseDescriptor, List<MavenProject> reactorProjects )
    {
        getLog().info( "Cleaning up after branch..." );
        
        clean( branchPhases, releaseDescriptor, reactorProjects );
    }
    
    protected void clean( List<String> phasesToRun, CxxReleaseDescriptor releaseDescriptor,
        List<MavenProject> reactorProjects )
    {
        List<String> phasesToRunReverse = phasesToRun.subList( 0, phasesToRun.size() );
        Collections.reverse( phasesToRunReverse );

        for ( String name : phasesToRunReverse )
        {
            CxxReleasePhase phase = phases.get( name );

            phase.clean( reactorProjects );
        }
    }
}

