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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;

import org.apache.maven.plugin.MojoExecutionException;

/* Use FileSet and the FileManager provided in this project*/
import org.apache.maven.model.FileSet;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.maven.plugin.cxx.utils.ExecutorService;
import org.apache.maven.plugin.cxx.utils.FileSetManager;

/**
 * Goal which gcovr execution.
 *
 * @author Franck Bonin 
 */
@Mojo( name = "coverage", defaultPhase = LifecyclePhase.TEST )
public class CoverageMojo extends LaunchMojo
{  
    /**
     * The Report OutputFile Location.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "coverage.reportsfilePath", defaultValue = "gcovr-reports" )
    private File reportsfileDir;
    
    /**
     * The Report OutputFile name identifier.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "coverage.reportIdentifier", defaultValue = "" )
    private String reportIdentifier;
    
    private String getReportFileName()
    {
        return "gcovr-result-" + reportIdentifier + ".xml";
    }
    
    /**
     * Arguments to clean preexisting gcda report under workingDir
     * 
     * @since 0.0.4
     */
    @Parameter( property = "coverage.preclean", defaultValue = "true" )
    private boolean preclean;
    
    @Override
    protected void preExecute( Executor exec, CommandLine commandLine, Properties enviro ) throws MojoExecutionException
    {
        if ( preclean )
        {
            FileSet afileSet = new FileSet();
            afileSet.setDirectory( getWorkingDir().getAbsolutePath() );
            
            getLog().debug( "Search for **/*.gcda from " + afileSet.getDirectory() );
            afileSet.setIncludes( Arrays.asList( new String[]{"**/*.gcda"} ) );
            //afileSet.setExcludes( Arrays.asList(excludes) );
            
            FileSetManager aFileSetManager = new FileSetManager();
            String[] found = aFileSetManager.getIncludedFiles( afileSet );

            for ( int i = 0; i < found.length; i++ )
            {
                File target = new File( getWorkingDir() + "/" + found[i] );
                getLog().debug( "Found file " + target.getAbsolutePath() );
                if ( target.exists() )
                {
                    try
                    {
                        if ( target.delete() )
                        {
                            getLog().debug( "Succesfully delete " + target.getAbsolutePath() );
                        }
                        else
                        {
                            getLog().warn( "Failed to delete " + target.getAbsolutePath() );
                        }
                    }
                    catch ( SecurityException e )
                    {
                        getLog().warn( "SecurityException, unable to delete " + target.getAbsolutePath() );
                    } 
                }
                else
                {
                    getLog().debug( "But file " + target.getAbsolutePath() + " not exist" );
                }
            }
        }
    }
    
    /**
     * Arguments for the gcovr program. Shall be -x -d
     * ex: -x -d to produce Xml reports and clean gcda execution reports after reading
     * 
     */
    @Parameter( property = "coverage.args", defaultValue = "-x -d" )
    private String gcovrArgs;
    
    @Override
    protected void postExecute( int resultCode ) throws MojoExecutionException
    {
        String outputReportName = new String();
        if ( reportsfileDir.isAbsolute() )
        {
            outputReportName = reportsfileDir.getAbsolutePath() + "/" + getReportFileName();
        }
        else
        {
            outputReportName = basedir.getAbsolutePath() + "/" + reportsfileDir.getPath() + "/" + getReportFileName();
        }
        getLog().info( "Coverage report location " + outputReportName );
         
        OutputStream outStream = System.out;
        File file = new File( outputReportName );
        try
        {
            new File( file.getParent() ).mkdirs();
            file.createNewFile();
            outStream = new FileOutputStream( file );
        }
        catch ( IOException e )
        {
            getLog().error( "Coverage report redirected to stdout since " + outputReportName + " can't be opened" );
        }
        
        InputStream pyScript = getClass().getResourceAsStream( "/gcovr.py" );
        
        CommandLine commandLine = new CommandLine( "python" );
        Executor exec = new DefaultExecutor();
        String[] args = parseCommandlineArgs( "-" );
        commandLine.addArguments( args, false );
        args = parseCommandlineArgs( gcovrArgs );
        commandLine.addArguments( args, false );
        exec.setWorkingDirectory( getWorkingDir() );
        try
        {
            getLog().info( "Executing command line: " + commandLine );

            int res = ExecutorService.executeCommandLine( exec, commandLine, getEnvs(),
                outStream/*getOutputStreamOut()*/, getOutputStreamErr(), pyScript/*getInputStream()*/ );
            // this is a hugly workaround against a random bugs from hudson cobertura plugin.
            // hudson cobertura plugin randomly truncat coverage reports file to a 1024 size multiple
            // while it copy reports from slave to master node
            for ( int j = 0 ; j < 200 ; j++ )
            {
                for ( int i = 0 ; i < 80 ; i++ )
                {
                    outStream.write( ' ' );
                }
                outStream.write( '\n' );
            }
            outStream.flush();
            
            if ( isResultCodeAFailure( res ) )
            {
                throw new MojoExecutionException( "Result of command line execution is: '" + res + "'." );
            }
        }
        catch ( ExecuteException e )
        {
            throw new MojoExecutionException( "Command execution failed.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Command execution failed.", e );
        }
    }
    
    /**
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @since 0.0.5
     */
    @Parameter( property = "skipTests", defaultValue = "false" )
    protected boolean skipTests;
    
    /**
     * Set this to "true" to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable
     * it using the "maven.test.skip" property, because maven.test.skip shall disables both running the tests
     * and compiling the tests. Consider using the <code>skipTests</code> parameter instead.
     *
     * @since 0.0.5
     */
    @Parameter( property = "maven.test.skip", defaultValue = "false" )
    protected boolean skip;
    
    @Override
    protected boolean isSkip()
    {
        return super.isSkip() || skipTests || skip;
    }
}
