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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.plugin.logging.Log;

/**
 * A phase in the cxx release/branch goal.
 *
 * $FB derived from org.apache.maven.shared.release.phase.ReleasePhase;
 * @author 
 */
public interface CxxReleasePhase
{
    /**
     * The Plexus role.
     */
    String ROLE = CxxReleasePhase.class.getName();

    /**
     * Execute the phase.
     *
     * @param releaseDescriptor the configuration to use
     * @param releaseEnvironment the environmental configuration, such as Maven settings, Maven home, etc.
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException an exception during the execution of the phase
     * @throws ReleaseFailureException   a failure during the execution of the phase
     * @return the release result
     */
    ReleaseResult run( CxxReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                           List<MavenProject> reactorProjects )
        throws MojoExecutionException, MojoFailureException;

    /**
     * Clean up after a phase if it leaves any additional files in the checkout.
     *
     * @param reactorProjects the reactor projects
     * @return the release result
     */
    ReleaseResult clean( List<MavenProject> reactorProjects );
    
    /**
     *
     *
     * @param log
     */
    void setLog( Log log );
}

