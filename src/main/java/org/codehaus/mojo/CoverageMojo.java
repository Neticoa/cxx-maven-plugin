/*
 * Copyright (C) 2011, Neticoa SAS France - Tous droits réservés.
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
package org.codehaus.mojo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;

import org.apache.maven.plugin.MojoExecutionException;

/* Use FileSet and the FileManager provided in this project*/
import org.apache.maven.model.FileSet;

/**
 * Goal which gcovr execution.
 *
 * @author Franck Bonin 
 * @goal coverage
 * @phase test
 * 
 */
public class CoverageMojo extends LaunchMojo
{  
    /**
     * The Report OutputFile Location.
     * 
     * @parameter expression="${coverage.reportsfilePath}" default-value="gcovr-reports"
     * @since 0.0.4
     */
    private File reportsfileDir;
    
    /**
     * The Report OutputFile name identifier.
     * 
     * @parameter expression="${gcovr.reportIdentifier}" default-value=""
     * @since 0.0.4
     */
    private String reportIdentifier;
    
    private String getReportFileName()
    {
        return "gcovr-result-" + reportIdentifier + ".xml";
    }
    
    /**
     * Arguments to clean preexisting gcda report under workingDir
     * 
     * @parameter expression="${gcovr.preclean}" default-value=true
     */
    private boolean preclean;
    
    protected void preExecute(Executor exec, CommandLine commandLine, Map enviro) throws MojoExecutionException
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

            for (int i = 0; i < found.length; i++)
            {
                File target = new File( getWorkingDir() + "/" + found[i]);
                getLog().debug( "Found file " + target.getAbsolutePath() );
                if (target.exists() )
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
     * @parameter expression="${gcovr.args}" default-value="-x -d"
     */
    private String gcovrArgs;
    
    protected void postExecute(int resultCode) throws MojoExecutionException
    {
        String OutputReportName = new String();
        if ( reportsfileDir.isAbsolute() )
        {
            OutputReportName = reportsfileDir.getAbsolutePath() + "/" + getReportFileName();
        }
        else
        {
            OutputReportName = basedir.getAbsolutePath() + "/" + reportsfileDir.getPath() + "/" + getReportFileName();
        }
        getLog().info( "Coverage report location " + OutputReportName );
         
        OutputStream outStream = System.out;
        File file = new File( OutputReportName );
        try
        {
            new File( file.getParent() ).mkdirs();
            file.createNewFile();
            outStream = new FileOutputStream( file );
        }
        catch ( IOException e )
        {
            getLog().error( "Coverage report redirected to stdout since " + OutputReportName + " can't be opened" );
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

            int res = executeCommandLine( exec, commandLine, getEnvs(), outStream/*getOutputStreamOut()*/, getOutputStreamErr(), pyScript/*getInputStream()*/ );
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
}
