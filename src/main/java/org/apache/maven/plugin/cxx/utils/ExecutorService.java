package org.apache.maven.plugin.cxx.utils;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Locale;
import java.util.Properties;

import org.codehaus.plexus.util.cli.CommandLineUtils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang.StringUtils;

/**
 * Executor service
 */
public class ExecutorService
{
    public static Properties getSystemEnvVars() throws IOException
    {
        return CommandLineUtils.getSystemEnvVars();
    }
  
    public static CommandLine getExecutablePath( String executableName, Properties enviro, File dir )
    {
        File execFile = new File( executableName );
        String exec = null;
        if ( execFile.exists() )
        {
            //getLog().debug( "Toolchains are ignored, 'executable' parameter is set to " + executableName );
            exec = execFile.getAbsolutePath();
        }
        else
        {
            if ( OS.isFamilyWindows() )
            {
                String ex = executableName.indexOf( "." ) < 0 ? executableName + ".bat" : executableName;
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
            exec = executableName;
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
        
    public static int executeCommandLine( Executor exec, CommandLine commandLine, Properties enviro, OutputStream out,
            OutputStream err,  InputStream in ) throws ExecuteException, IOException
    {
        exec.setExitValues( null ); // let us decide of exit value
        exec.setStreamHandler( new PumpStreamHandler( out, err, in ) );
        return exec.execute( commandLine, enviro );
    }
    
}
