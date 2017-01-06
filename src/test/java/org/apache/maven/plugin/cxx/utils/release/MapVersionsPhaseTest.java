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

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Matchers.anyString;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Test the version mapping phase.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MapVersionsPhaseTest
    extends PlexusTestCase
{
    /* !IMPORTANT! see
     * https://codehaus-plexus.github.io/guides/developer-guide/building-components/component-testing.html:
     * 
     * Normal [component discovery] is performed during the initialization of the test case.
     * It means that if your component descriptor was already added to /META-INF/plexus/components.xml file it
     * will be visible during tests.
     *
     * In case when you want to override the defaults, the only thing you have to do is to create new xml file
     * having the name which matches the class name but with extentinon ".xml". This file must be placed in
     * !!the same package!! as the test class and be visible in the unit test's classpath.
     * 
     * In case of "DefaultHelloWorldTest" the file should be named "DefaultHelloWorldTest.xml" and placed under the
     * right folder hierarchie
     */
    private static final String TEST_MAP_BRANCH_VERSIONS = "TestMapBranchVersionsPhase";
    private static final String TEST_MAP_DEVELOPMENT_VERSIONS = "TestMapDeveloppementVersionsPhase";
    private static final String TEST_MAP_RELEASE_VERSIONS = "TestMapReleaseVersionsPhase";
    @Mock
    private Prompter mockPrompter;
    
    private Log log = null;

    public void setUp()
        throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks( this );
        
        this.log = new org.apache.maven.monitor.logging.DefaultLog( 
            new org.codehaus.plexus.logging.console.ConsoleLogger( 
                org.codehaus.plexus.logging.console.ConsoleLogger.LEVEL_INFO, "test" ) );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        // this check if all verify(...) have been successfully called for each mockPrompter interactions
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testExecuteSnapshot_MapRelease()
        throws Exception
    {
        log.info( "testExecuteSnapshot_MapRelease" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT" );

        when(
              mockPrompter.prompt( startsWith( "What is the release version for component \"" + project.getName() + "\"?" ),
                                   eq( "1.0" ) ) ).thenReturn( "2.0" );
        //when( mockPrompter.prompt( anyString(), eq( "1.0" ) ) ).thenReturn( "2.0" );
        
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );

        verify( mockPrompter ).prompt( startsWith( "What is the release version for component \"" + project.getName()
                                                       + "\"?" ), eq( "1.0" ) );
        //verify( mockPrompter ).prompt( anyString(), eq( "1.0" ) );
    }

    public void testSimulateSnapshot_MapReleaseVersions()
        throws Exception
    {
        log.info( "testSimulateSnapshot_MapReleaseVersions" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT" );

        when(
              mockPrompter.prompt( startsWith( "What is the release version for component \"" + project.getName() + "\"?" ),
                                   eq( "1.0" ) ) ).thenReturn( "2.0" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );
        verify( mockPrompter  ).prompt( startsWith( "What is the release version for component \"" + project.getName()
                                                       + "\"?" ), eq( "1.0" ) );
    }

    // MRELEASE-403: Release plugin ignores given version number
    public void testMapReleaseVersionsInteractiveAddZeroIncremental()
        throws Exception
    {
        log.info( "testMapReleaseVersionsInteractiveAddZeroIncremental" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT" );

        when(
              mockPrompter.prompt( startsWith( "What is the release version for component \"" + project.getName() + "\"?" ),
                                   eq( "1.0" ) ) ).thenReturn( "1.0.0" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0.0" ),
                      releaseDescriptor.getReleaseVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0.0" ),
                      releaseDescriptor.getReleaseVersions() );
        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the release version for component \"" + project.getName()
                                                       + "\"?" ), eq( "1.0" ) );
    }

    /**
     * Test to release "SNAPSHOT" version MRELEASE-90
     */
    public void testMapReleaseVersionsInteractiveWithSnaphotVersion()
        throws Exception
    {
        log.info( "testMapReleaseVersionsInteractiveWithSnaphotVersion" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );
        MavenProject project = createProject( "artifactId", "SNAPSHOT" );

        when(
              mockPrompter.prompt( startsWith( "What is the release version for component \"" + project.getName() + "\"?" ),
                                   eq( "1.0" ) ) ).thenReturn( "2.0" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );

        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the release version for component \"" + project.getName()
                                                       + "\"?" ), eq( "1.0" ) );
    }

    /**
     * MRELEASE-524: ignores commandline versions in batch mode
     */
    public void testMapReleaseVersionsNonInteractiveWithExplicitVersion()
        throws Exception
    {
        log.info( "testMapReleaseVersionsNonInteractiveWithExplicitVersion" );
        // prepare
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "SNAPSHOT" ) );

        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.addReleaseVersion( "groupId:artifactId", "2.0" );

        phase.setPrompter( mockPrompter );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.addReleaseVersion( "groupId:artifactId", "2.0" );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );
    }

    public void testExecuteSnapshotNonInteractive_MapRelease()
        throws Exception
    {
        log.info( "testExecuteSnapshotNonInteractive_MapRelease" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getReleaseVersions() );
    }

    public void testSimulateSnapshotNonInteractive_MapReleaseVersions()
        throws Exception
    {
        log.info( "testSimulateSnapshotNonInteractive_MapReleaseVersions" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getReleaseVersions() );
    }

    public void testMapDevVersionsInteractive()
        throws Exception
    {
        log.info( "testMapDevVersionsInteractive" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );
        MavenProject project = createProject( "artifactId", "1.0" );

        when(
              mockPrompter.prompt( startsWith( "What is the next development version for component \"" + project.getName() + "\"?" ),
                                   eq( "1.1-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the next development version for component \""
                                                       + project.getName() + "\"?" ), eq( "1.1-SNAPSHOT" ) );
    }

    /**
     * MRELEASE-760: updateWorkingCopyVersions=false still bumps up pom versions to next development version
     */
    public void testMapDevVersionsInteractiveDoNotUpdateWorkingCopy()
        throws Exception
    {
        log.info( "testMapDevVersionsInteractiveDoNotUpdateWorkingCopy" );
        log.info( "" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );
        MavenProject project = createProject( "artifactId", "1.0" );

        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setUpdateWorkingCopyVersions( false );
        releaseDescriptor.setInteractive( false );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getDevelopmentVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setUpdateWorkingCopyVersions( false );
        releaseDescriptor.setInteractive( false );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testMapDevVersionsNonInteractive()
        throws Exception
    {
        log.info( "testMapDevVersionsNonInteractive" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );
        
        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }
    
    public void testMapDevVersionsNonInteractive2()
        throws Exception
    {
        log.info( "testMapDevVersionsNonInteractive" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( false );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.1" ),
                      releaseDescriptor.getDevelopmentVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( false );
        
        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.1" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    /**
     * MRELEASE-524: ignores commandline versions in batch mode
     */
    public void testMapDevVersionsNonInteractiveWithExplicitVersion()
        throws Exception
    {
        log.info( "testMapDevVersionsNonInteractiveWithExplicitVersion" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        phase.setPrompter( mockPrompter );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.addDevelopmentVersion( "groupId:artifactId", "2-SNAPSHOT" );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.addDevelopmentVersion( "groupId:artifactId", "2-SNAPSHOT" );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }
    
    public void testMapDevVersionsNonInteractiveWithExplicitVersion2()
        throws Exception
    {
        log.info( "testMapDevVersionsNonInteractiveWithExplicitVersion" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        phase.setPrompter( mockPrompter );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.addDevelopmentVersion( "groupId:artifactId", "2.4" );
        releaseDescriptor.setSnapshotDevelopmentVersion( false );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.4" ),
                      releaseDescriptor.getDevelopmentVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.addDevelopmentVersion( "groupId:artifactId", "2.4" );
        releaseDescriptor.setSnapshotDevelopmentVersion( false );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.4" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testPrompterException()
        throws Exception
    {
        log.info( "testPrompterException" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        when( mockPrompter.prompt( isA( String.class ), isA( String.class ) ) ).thenThrow( new PrompterException( "..." ) );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();

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

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();

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

        // verify
        verify( mockPrompter, times( 2 ) ).prompt( isA( String.class ), isA( String.class ) );
    }

    public void testAdjustVersionInteractive()
        throws Exception
    {
        log.info( "testAdjustVersionInteractive" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );
        MavenProject project = createProject( "artifactId", "foo" );

        when(
              mockPrompter.prompt( startsWith( "What is the next development version for component \"" + project.getName() + "\"?" ),
                                   eq( "1.1-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        
        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the next development version for component \""
                                                       + project.getName() + "\"?" ), eq( "1.1-SNAPSHOT" ) );
    }
    
    public void testAdjustVersionInteractive2()
        throws Exception
    {
        log.info( "testAdjustVersionInteractive" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );
        MavenProject project = createProject( "artifactId", "foo" );

        when(
              mockPrompter.prompt( startsWith( "What is the next development version for component \"" + project.getName() + "\"?" ),
                                   eq( "1.1" ) ) ).thenReturn( "2.0-SNAPSHOT" ).thenReturn( "2.0" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setSnapshotDevelopmentVersion( false );

        // execute
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getDevelopmentVersions() );

        log.info( "" );
        // prepare
        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setSnapshotDevelopmentVersion( false );

        // execute
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getDevelopmentVersions() );
               
        // call 3 time : first call handled with "2.0-SNAPSHOT" bad answer then all other call with last (good) answer "2.0"
        verify( mockPrompter, times( 3 ) ).prompt( startsWith( "What is the next development version for component \""
                                                       + project.getName() + "\"?" ), eq( "1.1" ) );
    }
    

    public void testAdjustVersionNonInteractive()
        throws Exception
    {
        log.info( "testAdjustVersionNonInteractive" );
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "foo" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        try
        {
            phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "check cause", VersionParseException.class, e.getCause().getClass() );
        }

        releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        try
        {
            releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "check cause", VersionParseException.class, e.getCause().getClass() );
        }
    }
    
    public void testExecuteSnapshotBranchCreation_DefaultDevelopmentVersion_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_DefaultDevelopmentVersion_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
    }

    public void testSimulateSnapshotBranchCreation_DefaultDevelopmentVersion_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_DefaultDevelopmentVersion_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1-SNAPSHOT" );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
    }
  
    public void testExecuteSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testExecuteSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment2()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment2" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testSimulateSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }
    
    public void testSimulateSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment2()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment2" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }
    
    public void testExecuteSnapshotBranchCreation_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_NonInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testSimulateSnapshotBranchCreation_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_NonInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }
   
    public void testExecuteSnapshotDefaultDevelopmentVersion_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotDefaultDevelopmentVersion_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
    }

    public void testSimulateSnapshotDefaultDevelopmentVersion_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotDefaultDevelopmentVersion_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
    }
   
    public void testExecuteSnapshotDefaultDevelopmentVersion_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotDefaultDevelopmentVersion_NonInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testSimulateSnapshotDefaultDevelopmentVersion_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotDefaultDevelopmentVersion_NonInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultDevelopmentVersion( "1.1.1-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }
    
    public void testExecuteSnapshotNonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotNonInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testSimulateSnapshotNonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotNonInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testExecuteSnapshotAutoVersionSubmodules_NotInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotAutoVersionSubmodules_NotInteractive_MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotAutoVersionSubmodules_NotInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotAutoVersionSubmodules_NotInteractive_MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteReleaseAutoVersionSubmodules_NotInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteReleaseAutoVersionSubmodules_NotInteractive_MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateReleaseAutoVersionSubmodules_NotInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateReleaseAutoVersionSubmodules_NotInteractive_MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotAutoVersionSubmodules_NotInteractive_MapRelease()
        throws Exception
    {
        log.info( "testExecuteSnapshotAutoVersionSubmodules_NotInteractive_MapRelease" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotAutoVersionSubmodules_NotInteractive_MapRelease()
        throws Exception
    {
        log.info( "testSimulateSnapshotAutoVersionSubmodules_NotInteractive_MapRelease" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteReleaseAutoVersionSubmodules_NotInteractive_MapRelease()
        throws Exception
    {
        log.info( "testExecuteReleaseAutoVersionSubmodules_NotInteractive_MapRelease" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateReleaseAutoVersionSubmodules_NotInteractive_MapRelease()
        throws Exception
    {
        log.info( "testSimulateReleaseAutoVersionSubmodules_NotInteractive_MapRelease" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        //List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
        //Collections.addAll( reactorProjects, createProject( "artifactId", "1.2-SNAPSHOT" ),
        //                    createProject( "module1", "2.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        
        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        //assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
        //              releaseDescriptor.getDevelopmentVersions() );
        // $FB new behaviour : we don't care if component previous version is or not a snapshot,
        // we obey to UpdateWorkingCopyVersion flag, setted by default
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment2()
        throws Exception
    {
        log.info( "testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment2" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        //List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
        Collections.addAll( reactorProjects, createProject( "artifactId", "1.2" ),
                            createProject( "module1", "2.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );
        
        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        //assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
        //              releaseDescriptor.getDevelopmentVersions() );
                      
        Map<String, String> developmentVersions = new HashMap<String, String>();
        developmentVersions.put( "groupId:artifactId", "1.3-SNAPSHOT" );
        developmentVersions.put( "groupId:module1", "2.0" );
        assertEquals( "Check development versions", developmentVersions, releaseDescriptor.getDevelopmentVersions() );

        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment2_2()
        throws Exception
    {
        log.info( "testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment2_2" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        //List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
        Collections.addAll( reactorProjects, createProject( "artifactId", "1.2" ),
                            createProject( "module1", "2.0-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        
        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        //assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
        //              releaseDescriptor.getDevelopmentVersions() );
                      
        Map<String, String> developmentVersions = new HashMap<String, String>();
        developmentVersions.put( "groupId:artifactId", "1.3" );
        developmentVersions.put( "groupId:module1", "1.3" );
        assertEquals( "Check development versions", developmentVersions, releaseDescriptor.getDevelopmentVersions() );

        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment3()
        throws Exception
    {
        log.info( "testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment3" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        //List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
        //Collections.addAll( reactorProjects, createProject( "artifactId", "1.2-SNAPSHOT" ),
        //                    createProject( "module1", "2.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );
        //releaseDescriptor.setUpdateWorkingCopyVersions( false );
        
        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        //assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2-SNAPSHOT" ),
        //              releaseDescriptor.getDevelopmentVersions() );
        // $FB new behaviour : we don't care if component previous version is or not a snapshot,
        // we obey to UpdateWorkingCopyVersion flag, setted by default
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment4()
        throws Exception
    {
        log.info( "testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment4" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        //List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
        //Collections.addAll( reactorProjects, createProject( "artifactId", "1.2-SNAPSHOT" ),
        //                    createProject( "module1", "2.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        
        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    

    public void testSimulateReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateWorkingCopyVersions( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch()
        throws Exception
    {
        log.info( "testExecuteSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch()
        throws Exception
    {
        log.info( "testSimulateSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch()
        throws Exception
    {
        log.info( "testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision."
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version. For now keep
         * '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch()
        throws Exception
    {
        log.info( "testSimulateReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision."
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version. For now keep
         * '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotBranchCreation_NonInteractive_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_NonInteractive_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotBranchCreation_NonInteractive_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_NonInteractive_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotBranchCreation_DefaultReleaseVersion_NonInteractive_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_DefaultReleaseVersion_NonInteractive_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultReleaseVersion( "2.1-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "2.1-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotBranchCreation_DefaultReleaseVersion_NonInteractive_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_DefaultReleaseVersion_NonInteractive_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultReleaseVersion( "2.1-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "2.1-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotBranchCreation_DefaultReleaseVersion_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_DefaultReleaseVersion_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultReleaseVersion( "2.1-SNAPSHOT" );
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );
        releaseDescriptor.setInteractive( false );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "2.1-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotBranchCreation_DefaultReleaseVersion_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_DefaultReleaseVersion_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setDefaultReleaseVersion( "2.1-SNAPSHOT" );
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "2.1-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotBranchCreation_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

		// updateBranchVersions is set to true, so suggest the next snapshot version
		// org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
		// org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when( mockPrompter.prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
		// org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
		// org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify( mockPrompter ).prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) );
    }

    public void testSimulateSnapshotBranchCreation_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

		// updateBranchVersions is set to true, so suggest the next snapshot version
		// org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
		// org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when( mockPrompter.prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
		// org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
		// org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify( mockPrompter ).prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) );
    }
    
    public void testExecuteReleaseBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch()
        throws Exception
    {
        log.info( "testExecuteReleaseBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

		// updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT (yes, one step back!)
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when( mockPrompter.prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) ) ).thenReturn( "2.1-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT (yes, one step back!)
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify( mockPrompter ).prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) );
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "2.1-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    } 

    public void testSimulateReleaseBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch()
        throws Exception
    {
        log.info( "testSimulateReleaseBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

		// updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT (yes, one step back!)
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when( mockPrompter.prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) ) ).thenReturn( "2.1-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT (yes, one step back!)
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify( mockPrompter ).prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) );
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "2.1-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

		// updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when( mockPrompter.prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify( mockPrompter ).prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) );
    }

    public void testSimulateSnapshotBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateBranchVersions( true );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );

		// updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when( mockPrompter.prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify( mockPrompter ).prompt( startsWith( "What is the branch version for component" ), eq( "1.3-SNAPSHOT" ) );
    }
    
    public void testExecuteReleaseBranchCreation_MapBranch()
        throws Exception
    {
        log.info( "testExecuteReleaseBranchCreation_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision."
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version. For now keep
         * '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateReleaseBranchCreation_MapBranch()
        throws Exception
    {
        log.info( "testSimulateReleaseBranchCreation_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision."
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version. For now keep
         * '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }
    
    public void testExecuteReleaseBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteReleaseBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateWorkingCopyVersions( false );
        releaseDescriptor.setInteractive( false );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getDevelopmentVersions() );
    }

    public void testSimulateReleaseBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateReleaseBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateWorkingCopyVersions( false );
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getDevelopmentVersions() );
    }

    public void testExecuteReleaseBranchCreation_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteReleaseBranchCreation_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        // org.apache.maven.release:maven-release-manager:(,2.4) > true
        // org.apache.maven.release:maven-release-manager:[2.4,) > false
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateWorkingCopyVersions( false );
        
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getDevelopmentVersions() );
    }

    public void testSimulateReleaseBranchCreation_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateReleaseBranchCreation_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        // org.apache.maven.release:maven-release-manager:(,2.4) > true
        // org.apache.maven.release:maven-release-manager:[2.4,) > false
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateWorkingCopyVersions( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getDevelopmentVersions() );
    }

    public void testExecuteSnapshotBranchCreation_MapBranch()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotBranchCreation_MapBranch()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateWorkingCopyVersions( false );
        releaseDescriptor.setInteractive( false );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
    }

    public void testSimulateSnapshotBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateWorkingCopyVersions( false );
        releaseDescriptor.setInteractive( false );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
    }

    public void testExecuteReleaseBranchCreation_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testExecuteReleaseBranchCreation_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateBranchVersions( true );
        // org.apache.maven.release:maven-release-manager:(,2.4) > true
        // org.apache.maven.release:maven-release-manager:[2.4,) > false
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateBranchVersions( false );
        
        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateReleaseBranchCreation_UpdateBranchVersions_MapBranch()
        throws Exception
    {
        log.info( "testSimulateReleaseBranchCreation_UpdateBranchVersions_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); 
        releaseDescriptor.setUpdateBranchVersions( true );
        // org.apache.maven.release:maven-release-manager:(,2.4) > true
        // org.apache.maven.release:maven-release-manager:[2.4,) > false
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateBranchVersions( false );
        
        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "1.2" ),
                      releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotBranchCreation_UpdateWorkingCopyVersions_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotBranchCreation_UpdateWorkingCopyVersions_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); // signifiant for prompt wording
        releaseDescriptor.setUpdateWorkingCopyVersions( true );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        when( mockPrompter.prompt( startsWith( "What is the next working copy version for component" ), eq( "1.3-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( mockPrompter ).prompt( startsWith( "What is the next working copy version for component" ), eq( "1.3-SNAPSHOT" ) );
    }

    public void testSimulateSnapshotBranchCreation_UpdateWorkingCopyVersions_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotBranchCreation_UpdateWorkingCopyVersions_MapDevelopment" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setBranchCreation( true ); // signifiant for prompt wording
        releaseDescriptor.setUpdateWorkingCopyVersions( true );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        when( mockPrompter.prompt( startsWith( "What is the next working copy version for component" ), eq( "1.3-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( mockPrompter ).prompt( startsWith( "What is the next working copy version for component" ), eq( "1.3-SNAPSHOT" ) );
    }

    public void testExecuteMultiModuleAutoVersionSubmodules__MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteMultiModuleAutoVersionSubmodules__MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
        Collections.addAll( reactorProjects, createProject( "artifactId", "1.2-SNAPSHOT" ),
                            createProject( "module1", "2.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        Map<String, String> developmentVersions = new HashMap<String, String>();
        developmentVersions.put( "groupId:artifactId", "1.3-SNAPSHOT" );
        developmentVersions.put( "groupId:module1", "2.0" );
        assertEquals( "Check development versions", developmentVersions, releaseDescriptor.getDevelopmentVersions() );
        assertEquals( "Check release versions", 0, releaseDescriptor.getReleaseVersions().size() );
    }

    public void testSimulateMultiModuleAutoVersionSubmodules__MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateMultiModuleAutoVersionSubmodules__MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
        Collections.addAll( reactorProjects, createProject( "artifactId", "1.2-SNAPSHOT" ),
                            createProject( "module1", "2.0" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        Map<String, String> developmentVersions = new HashMap<String, String>();
        developmentVersions.put( "groupId:artifactId", "1.3-SNAPSHOT" );
        developmentVersions.put( "groupId:module1", "2.0" );
        assertEquals( "Check development versions", developmentVersions, releaseDescriptor.getDevelopmentVersions() );
        assertEquals( "Check release versions", 0, releaseDescriptor.getReleaseVersions().size() );
    }

    public void testExecuteSnapshotAutoVersionSubmodules_DefaultReleaseVersion_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotAutoVersionSubmodules_DefaultReleaseVersion_NonInteractive_MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects =
            Collections.singletonList( createProject( "artifactId", "1.2.1-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        // $FB Bug from maven-release-plugin : develpment version shall not be suggested using main suggested version
        // this should have never worked
        //releaseDescriptor.setDefaultReleaseVersion( "3.0" );
        releaseDescriptor.setDefaultDevelopmentVersion( "3.0" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        //assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.1-SNAPSHOT" ),
        //              releaseDescriptor.getDevelopmentVersions() );
        // $FB corrected behavior : we select direct snapshot of suggested version
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotAutoVersionSubmodules_DefaultReleaseVersion_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotAutoVersionSubmodules_DefaultReleaseVersion_NonInteractive_MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects =
            Collections.singletonList( createProject( "artifactId", "1.2.1-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        // $FB Bug from maven-release-plugin : develpment version shall not be suggested using main suggested version
        // this should have never worked
        //releaseDescriptor.setDefaultReleaseVersion( "3.0" );
        releaseDescriptor.setDefaultDevelopmentVersion( "3.0" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        //assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.1-SNAPSHOT" ),
        //              releaseDescriptor.getDevelopmentVersions() );
        // $FB corrected behavior : we select direct snapshot of suggested version
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotAutoVersionSubmodules_DefaultDevelopmentVersion_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testExecuteSnapshotAutoVersionSubmodules_DefaultDevelopmentVersion_NonInteractive_MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects =
            Collections.singletonList( createProject( "artifactId", "1.2.1-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setDefaultDevelopmentVersion( "3.0-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotAutoVersionSubmodules_DefaultDevelopmentVersion_NonInteractive_MapDevelopment()
        throws Exception
    {
        log.info( "testSimulateSnapshotAutoVersionSubmodules_DefaultDevelopmentVersion_NonInteractive_MapDevelopment" );
        // verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects =
            Collections.singletonList( createProject( "artifactId", "1.2.1-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setDefaultDevelopmentVersion( "3.0-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        // test
        releaseDescriptor.setDryRun( true );
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    // MRELEASE-511
    public void testUnusualVersions1() throws Exception
    {
        log.info( "testUnusualVersions1" );
        MapVersionsPhase mapReleasephase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );
        MapVersionsPhase mapDevelopmentphase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );
        
        List<MavenProject> reactorProjects =
            Collections.singletonList( createProject( "artifactId", "MYB_200909-SNAPSHOT" ) );
        
        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultReleaseVersion( "PPX" );
        releaseDescriptor.setDefaultDevelopmentVersion( "MYB_200909-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );
        
        // test
        mapReleasephase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        mapDevelopmentphase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        // verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "MYB_200909-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        assertEquals( "Check release versions", Collections.singletonMap( "groupId:artifactId", "PPX" ), 
                      releaseDescriptor.getReleaseVersions() );
    }

    // MRELEASE-269
    public void testContinuousSnapshotCheck() throws Exception
    {
        log.info( "testContinuousSnapshotCheck" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "bar", "1.11-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        when( mockPrompter.prompt( startsWith( "What is the next development version for component " ), eq( "1.12-SNAPSHOT" ) ) )
            .thenReturn( "2.0" ) // wrong, expected SNAPSHOT
            .thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the next development version for component " ), eq( "1.12-SNAPSHOT" ) );
    }
    
    //MRELEASE-734
    public void testEmptyDefaultDevelopmentVersion() throws Exception
    {
        log.info( "testEmptyDefaultDevelopmentVersion" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_DEVELOPMENT_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "bar", "1.11-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultDevelopmentVersion( "" );
        releaseDescriptor.setSnapshotDevelopmentVersion( true );

        when( mockPrompter.prompt( startsWith( "What is the next development version for component " ), eq( "1.12-SNAPSHOT" ) ) )
            .thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( mockPrompter ).prompt( startsWith( "What is the next development version for component " ), eq( "1.12-SNAPSHOT" ) );
    }
    
    public void testEmptyDefaultReleaseVersion() throws Exception
    {
        log.info( "testEmptyDefaultReleaseVersion" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_RELEASE_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "bar", "1.11-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultReleaseVersion( "" );

        when( mockPrompter.prompt( startsWith( "What is the release version for component " ), eq( "1.11" ) ) )
            .thenReturn( "2.0" );
        phase.setPrompter( mockPrompter );

        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( mockPrompter ).prompt( startsWith( "What is the release version for component " ), eq( "1.11" ) );
    }
    
    public void testSnapShotOfCurrentVersion_MapBranch() throws Exception
    {
        log.info( "testSnapShotOfCurrentVersion_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.11-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultReleaseVersion( "2.0" );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );
        releaseDescriptor.setUpdateBranchVersions( true );
        
        when( mockPrompter.prompt( startsWith( "What is the branch version for component " ), eq( "2.0-SNAPSHOT" ) ) )
            .thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );
        
        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        // verify
        assertEquals( "Check branch versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        verify( mockPrompter ).prompt( startsWith( "What is the branch version for component " ), eq( "2.0-SNAPSHOT" ) );
    }
    
    public void testSnapShotOfCurrentVersion_noInteraction_MapBranch() throws Exception
    {
        log.info( "testSnapShotOfCurrentVersion_noInteraction_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.11-SNAPSHOT" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultReleaseVersion( "2.0-SNAPSHOT" );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );
        releaseDescriptor.setUpdateBranchVersions( true );
        
        when( mockPrompter.prompt( startsWith( "What is the branch version for component " ), eq( "2.0-SNAPSHOT" ) ) )
            .thenReturn( "3.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );
        
        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        // verify
        assertEquals( "Check branch versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getReleaseVersions() );
        verify( mockPrompter, times( 0 ) ).prompt( startsWith( "What is the branch version for component " ), eq( "2.0-SNAPSHOT" ) );
    }
    
    public void testSnapShotOfCurrentVersion_noInteraction_noUpdate_MapBranch() throws Exception
    {
        log.info( "testSnapShotOfCurrentVersion_noInteraction_MapBranch" );
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( CxxReleasePhase.ROLE, TEST_MAP_BRANCH_VERSIONS );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.11" ) );

        CxxReleaseDescriptor releaseDescriptor = new CxxReleaseDescriptor();
        releaseDescriptor.setDefaultReleaseVersion( "2.0" );
        releaseDescriptor.setUpdateVersionsToSnapshot( true );
        releaseDescriptor.setUpdateBranchVersions( false );
        
        when( mockPrompter.prompt( startsWith( "What is the branch version for component " ), eq( "2.0-SNAPSHOT" ) ) )
            .thenReturn( "3.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );
        
        // test
        phase.run( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        // verify
        assertEquals( "Check branch versions", Collections.singletonMap( "groupId:artifactId", "1.11" ),
                      releaseDescriptor.getReleaseVersions() );
        verify( mockPrompter, times( 0 ) ).prompt( startsWith( "What is the branch version for component " ), eq( "2.0-SNAPSHOT" ) );
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
