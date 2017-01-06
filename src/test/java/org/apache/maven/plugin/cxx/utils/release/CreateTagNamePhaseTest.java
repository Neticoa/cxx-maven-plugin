package org.apache.maven.plugin.cxx.utils.release;
//package org.apache.maven.shared.release.phase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Test the variable input phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CreateTagNamePhaseTest
    extends PlexusTestCase
{
    private CreateTagNamePhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        phase = (CreateTagNamePhase) lookup( CxxReleasePhase.ROLE, "CreateTagNamePhase" );
    }
    
    protected void setPrompter( CreateTagNamePhase phase, Prompter prompter )
        throws Exception
    {
        org.apache.maven.shared.release.phase.InputVariablesPhase embeded_phase = 
            ( org.apache.maven.shared.release.phase.InputVariablesPhase) phase.getMavenReleasePhase();       
        org.apache.maven.plugin.cxx.utils.ClassAccessHelper.setFieldValue( embeded_phase, "prompter", prompter );   
    }

    public void testInputVariablesInteractive()
        throws Exception
    {
        // prepare
        Prompter mockPrompter = mock( Prompter.class );
        when( mockPrompter.prompt( isA( String.class ), eq( "artifactId-1.0" ) ) ).thenReturn( "tag-value", "simulated-tag-value" );
        //phase.setPrompter( mockPrompter );
        setPrompter( phase, mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseDescriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "tag-value", releaseDescriptor.getScmReleaseLabel() );

        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseDescriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        //verify
        assertEquals( "Check tag", "simulated-tag-value", releaseDescriptor.getScmReleaseLabel() );
        
        verify( mockPrompter, times( 2 ) ).prompt( isA( String.class ), eq( "artifactId-1.0" ) );
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testUnmappedVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();

        try
        {
            phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertNull( "check no cause", e.getCause() );
        }

        releaseDescriptor = new CxxReleaseDescriptor();

        try
        {
            releaseDescriptor.setDryRun( true );
            phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertNull( "check no cause", e.getCause() );
        }
    }

    public void testInputVariablesNonInteractive()
        throws Exception
    {
        // prepare
        Prompter mockPrompter = mock( Prompter.class );
        //phase.setPrompter( mockPrompter );
        setPrompter( phase, mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseDescriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "artifactId-1.0", releaseDescriptor.getScmReleaseLabel() );

        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseDescriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "artifactId-1.0", releaseDescriptor.getScmReleaseLabel() );
        
        // never use prompter
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testInputVariablesNonInteractiveConfigured()
        throws Exception
    {
        // prepare
        Prompter mockPrompter = mock( Prompter.class );
        //phase.setPrompter( mockPrompter );
        setPrompter( phase, mockPrompter );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setScmReleaseLabel( "tag-value" );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "tag-value", releaseDescriptor.getScmReleaseLabel() );

        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setScmReleaseLabel( "simulated-tag-value" );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "simulated-tag-value", releaseDescriptor.getScmReleaseLabel() );
        
        // never use prompter
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testInputVariablesInteractiveConfigured()
        throws Exception
    {
        // prepare
        Prompter mockPrompter = mock( Prompter.class );
        //phase.setPrompter( mockPrompter );
        setPrompter( phase, mockPrompter );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setScmReleaseLabel( "tag-value" );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "tag-value", releaseDescriptor.getScmReleaseLabel() );

        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setScmReleaseLabel( "simulated-tag-value" );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "simulated-tag-value", releaseDescriptor.getScmReleaseLabel() );
        
        // never use prompter
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testPrompterException()
        throws Exception
    {
        // prepare
        Prompter mockPrompter = mock( Prompter.class );
        when( mockPrompter.prompt( isA( String.class ), isA( String.class ) ) ).thenThrow( new PrompterException( "..." ) );
        //phase.setPrompter( mockPrompter );
        setPrompter( phase, mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseDescriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );

        // execute
        try
        {
            phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "check cause", PrompterException.class, e.getCause().getClass() );
        }

        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseDescriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );

        // execute
        try
        {
            releaseDescriptor.setDryRun( true );
            phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "check cause", PrompterException.class, e.getCause().getClass() );
        }
        
        //verify
        verify( mockPrompter, times( 2 ) ).prompt( isA( String.class ), isA( String.class ) );
        verifyNoMoreInteractions( mockPrompter );
    }

    //MRELEASE-110
    public void testCvsTag()
        throws Exception
    {
        // prepare
        Prompter mockPrompter = mock( Prompter.class );
        //phase.setPrompter( mockPrompter );
        setPrompter( phase, mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseConfiguration = new CxxReleaseDescriptor();
        releaseConfiguration.setInteractive( false );
        releaseConfiguration.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseConfiguration.setScmSourceUrl( "scm:cvs:pserver:anoncvs@localhost:/tmp/scm-repo:module" );

        // execute
        phase.run( releaseConfiguration, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "artifactId-1_0", releaseConfiguration.getScmReleaseLabel() );

        // prepare
        releaseConfiguration = new CxxReleaseDescriptor();
        releaseConfiguration.setInteractive( false );
        releaseConfiguration.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseConfiguration.setScmSourceUrl( "scm:cvs:pserver:anoncvs@localhost:/tmp/scm-repo:module" );

        // execute
        releaseConfiguration.setDryRun( true );
        phase.run( releaseConfiguration, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "artifactId-1_0", releaseConfiguration.getScmReleaseLabel() );
        
        // never use prompter
        verifyNoMoreInteractions( mockPrompter );
    }

    //MRELEASE-159
    public void testCustomTagFormat()
        throws Exception
    {
        // prepare
        Prompter mockPrompter = mock( Prompter.class );
        //phase.setPrompter( mockPrompter );
        setPrompter( phase, mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );
        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseDescriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "artifactId-1.0", releaseDescriptor.getScmReleaseLabel() );

        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );
        releaseDescriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );
        releaseDescriptor.setScmTagNameFormat( "simulated-@{artifactId}-@{version}" );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check tag", "simulated-artifactId-1.0", releaseDescriptor.getScmReleaseLabel() );
        
        // never use prompter
        verifyNoMoreInteractions( mockPrompter );
    }

    private static MavenProject createProject( String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( "groupId" );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        return new MavenProject( model );
    }

}
