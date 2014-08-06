/*
 * Copyright (C) 2011, Neticoa SAS France - Tous droits réservés.
 * Author(s) : Franck Bonin, Neticoa SAS France
 *
 * Copyright (C) 2005-2006, The Codehaus
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Iterator;
import java.io.OutputStream;
import java.util.Locale;

import org.codehaus.plexus.util.cli.CommandLineUtils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang.StringUtils;


public abstract class AbstractLaunchMojo extends AbstractMojo
{
    /**
    * Current Project
    * @parameter expression="${project}"
    */
    protected org.apache.maven.project.MavenProject project;

     /**
     * Pom directory location
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     * @since 0.0.4
     */
    protected File basedir;
    
    protected abstract List getArgsList();
    protected abstract String getCommandArgs();
    private List getCommandLineArgs() throws MojoExecutionException
    {
        List commandArguments = new ArrayList();
        
        if ( getCommandArgs() != null )
        {
            String[] args = parseCommandlineArgs( getCommandArgs() );
            for ( int i = 0; i < args.length; i++ )
            {
                 commandArguments.add( args[i] );
            }
        }
        else if ( getArgsList() != null )
        {
            for ( int i = 0; i < getArgsList().size(); i++ )
            {
                Object argument = getArgsList().get( i );
                String arg;
                if ( argument == null )
                {
                    throw new MojoExecutionException( "Misconfigured argument, value is null. "
                        + "Set the argument to an empty value if this is the required behaviour." );
                }
                else
                {
                    arg = argument.toString();
                    commandArguments.add( arg );
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
    protected String[] parseCommandlineArgs( String CommandLineArgs ) throws MojoExecutionException
    {
        if ( CommandLineArgs == null )
        {
            return null;
        }
        else
        {
            try
            {
                return CommandLineUtils.translateCommandline( CommandLineArgs );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( e.getMessage() );
            }
        }
    }
    
    protected abstract String getExecutable();

    protected abstract Map getMoreEnvironmentVariables();

    protected Map getEnvs()
    {
        Map enviro = new HashMap();
        try
        {
            Properties systemEnvVars = CommandLineUtils.getSystemEnvVars();
            enviro.putAll( systemEnvVars );
        }
        catch ( IOException x )
        {
            getLog().error( "Could not assign default system enviroment variables.", x );
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
    
    private void EnsureExistWorkingDirectory() throws MojoExecutionException
    {
        if ( !getWorkingDir().exists() )
        {
            getLog().info( "Making working directory '" + getWorkingDir().getAbsolutePath() + "'." );
            if ( !getWorkingDir().mkdirs() )
            {
                throw new MojoExecutionException( "Could not make working directory: '"
                    + getWorkingDir().getAbsolutePath() + "'" );
            }
        }
    }
    
    /**
     * The current build session instance.
     * 
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    //private MavenSession session;

    CommandLine getExecutablePath( Map enviro, File dir )
    {
        File execFile = new File( getExecutable() );
        String exec = null;
        if ( execFile.exists() )
        {
            getLog().debug( "Toolchains are ignored, 'executable' parameter is set to " + getExecutable() );
            exec = execFile.getAbsolutePath();
        }
        else
        {
            if ( OS.isFamilyWindows() )
            {
                String ex = getExecutable().indexOf( "." ) < 0 ? getExecutable() + ".bat" : getExecutable();
                File f = new File( dir, ex );
                if ( f.exists() )
                {
                    exec = ex;
                }
                else
                {
                    // now try to figure the path from PATH, PATHEXT env vars
                    // if bat file, wrap in cmd /c
                    String path = (String) enviro.get( "PATH" );
                    if ( path != null )
                    {
                        String[] elems = StringUtils.split( path, File.pathSeparator );
                        for ( int i = 0; i < elems.length; i++ )
                        {
                            f = new File( new File( elems[i] ), ex );
                            if ( f.exists() )
                            {
                                exec = ex;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if ( exec == null )
        {
            exec = getExecutable();
        }

        CommandLine toRet;
        if ( OS.isFamilyWindows() && exec.toLowerCase( Locale.getDefault() ).endsWith( ".bat" ) )
        {
            toRet = new CommandLine( "cmd" );
            toRet.addArgument( "/c" );
            toRet.addArgument( exec );
        }
        else
        {
            toRet = new CommandLine( exec );
        }

        return toRet;
    }
        
    protected int executeCommandLine( Executor exec, CommandLine commandLine, Map enviro, OutputStream out,
            OutputStream err,  InputStream in ) throws ExecuteException, IOException
    {
        exec.setStreamHandler( new PumpStreamHandler( out, err, in ) );
        return exec.execute( commandLine, enviro );
    }
    
    protected abstract List getSuccesCode();
    
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
    
    
    protected void preExecute( Executor exec, CommandLine commandLine, Map enviro ) throws MojoExecutionException
    {
    }
    
    protected void postExecute( int resultCode ) throws MojoExecutionException
    {
    }
    
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

        List commandArguments = getCommandLineArgs();
        
        Map enviro = getEnvs();
        
        EnsureExistWorkingDirectory();

        CommandLine commandLine = getExecutablePath( enviro, getWorkingDir() );

        Executor exec = new DefaultExecutor();
       
        commandLine.addArguments( (String[])commandArguments.toArray( new String[commandArguments.size()] ), false );

        exec.setWorkingDirectory( getWorkingDir() );

        try
        {
            getLog().info( "Executing command line: " + commandLine );
            
            preExecute(exec, commandLine, enviro);

            int resultCode = executeCommandLine( exec, commandLine, enviro, getOutputStreamOut(), getOutputStreamErr(), getInputStream() );
   
            if ( isResultCodeAFailure( resultCode ) )
            {
                throw new MojoExecutionException( "Result of " + commandLine + " execution is: '" + resultCode + "'." );
            }
            
            postExecute(resultCode);
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
