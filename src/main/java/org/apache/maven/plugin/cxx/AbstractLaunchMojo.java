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

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.OutputStream;

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.apache.maven.plugin.cxx.utils.ExecutorService;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;

/**
 * Common mojo stuff to launch external tools
 *
 * @author Franck Bonin
 */
public abstract class AbstractLaunchMojo extends AbstractCxxMojo
{
    protected abstract List<String> getArgsList();

    protected abstract String getCommandArgs();

    private List<String> getCommandLineArgs() throws MojoExecutionException
    {
        ArrayList<String> commandArguments = new ArrayList<String>();

        if ( getCommandArgs() != null )
        {
            String[] args = parseCommandlineArgs( getCommandArgs() );
            for ( String argument : args )
            {
                commandArguments.add( argument );
            }
        }
        else if ( getArgsList() != null )
        {
            List<String> args = getArgsList();
            for ( String argument : args )
            {
                if ( argument == null )
                {
                    throw new MojoExecutionException( "Misconfigured argument, value is null. "
                        + "Set the argument to an empty value if this is the required behaviour." );
                }
                else
                {
                    commandArguments.add( argument );
                }
            }
        }
        return commandArguments;
    }

    /**
     * 
     * @return Array of String representing the arguments
     * @throws MojoExecutionException for wrong formatted arguments
     */
    protected String[] parseCommandlineArgs( String commandLineArgs ) throws MojoExecutionException
    {
        if ( commandLineArgs == null )
        {
            return null;
        }
        else
        {
            try
            {
                return CommandLineUtils.translateCommandline( commandLineArgs );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( e.getMessage() );
            }
        }
    }

    protected abstract String getExecutable();

    protected abstract Map getMoreEnvironmentVariables();

    protected Map<String, String> getEnvs()
    {
        Map<String, String> enviro = new HashMap<String, String>();
        try
        {
            enviro = ExecutorService.getSystemEnvVars();
        }
        catch ( IOException e )
        {
            getLog().error( "Could not assign default system enviroment variables.", e );
        }

        if ( getMoreEnvironmentVariables() != null )
        {
            Iterator iter = getMoreEnvironmentVariables().keySet().iterator();
            while ( iter.hasNext() )
            {
                String key = (String) iter.next();
                String value = (String) getMoreEnvironmentVariables().get( key );
                enviro.put( key, value );
            }
        }
        return enviro;
    }

    protected abstract File getWorkingDir();

    private void ensureExistWorkingDirectory() throws MojoExecutionException
    {
        if ( !getWorkingDir().exists() )
        {
            getLog().info( "Making working directory '" + getWorkingDir().getAbsolutePath() + "'." );
            if ( !getWorkingDir().mkdirs() )
            {
                throw new MojoExecutionException(
                    "Could not make working directory: '" + getWorkingDir().getAbsolutePath() + "'" );
            }
        }
    }

    /**
     * The current build session instance.
     * 
     */
    // @Parameter( defaultValue = "${session}", readonly = true )
    // protected MavenSession session;

    protected abstract List<String> getSuccesCode();

    protected boolean isResultCodeAFailure( int result )
    {
        if ( getSuccesCode() == null || getSuccesCode().size() == 0 )
        {
            return result != 0;
        }
        for ( Iterator it = getSuccesCode().iterator(); it.hasNext(); )
        {
            int code = Integer.parseInt( (String) it.next() );
            if ( code == result )
            {
                return false;
            }
        }
        return true;
    }

    protected abstract boolean isSkip();

    protected OutputStream getOutputStreamOut()
    {
        return System.out;
    }

    protected OutputStream getOutputStreamErr()
    {
        return System.err;
    }

    protected InputStream getInputStream()
    {
        return System.in;
    }

    protected void preExecute( Executor exec, CommandLine commandLine, Map<String, String> enviro )
        throws MojoExecutionException
    {
    }

    protected void postExecute( int resultCode ) throws MojoExecutionException
    {
        if ( isResultCodeAFailure( resultCode ) )
        {
            throw new MojoExecutionException( "Result of command line execution is: '" + resultCode + "'." );
        }
    }

    @Override
    public void execute() throws MojoExecutionException
    {
        if ( isSkip() )
        {
            getLog().info( "skipping execute as per configuraion" );
            return;
        }

        if ( basedir == null )
        {
            throw new IllegalStateException( "basedir is null. Should not be possible." );
        }

        List<String> commandArguments = getCommandLineArgs();

        Map<String, String> enviro = getEnvs();

        ensureExistWorkingDirectory();

        CommandLine commandLine = ExecutorService.getExecutablePath( getExecutable(), enviro, getWorkingDir() );

        Executor exec = new DefaultExecutor();

        commandLine.addArguments( (String[]) commandArguments.toArray( new String[commandArguments.size()] ), false );

        exec.setWorkingDirectory( getWorkingDir() );

        try
        {
            getLog().info( "Executing command line: " + commandLine );

            preExecute( exec, commandLine, enviro );

            int resultCode = ExecutorService.executeCommandLine( exec, commandLine, enviro, getOutputStreamOut(),
                getOutputStreamErr(), getInputStream() );

            postExecute( resultCode );
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
