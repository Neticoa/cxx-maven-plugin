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
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.codehaus.plexus.util.StringUtils;

/**
 * Executor service
 */
public class ExecutorService
{
    private ExecutorService()
    {
    }

    /**
     * Gets the shell environment variables for this process. Note that the returned
     * mapping from variable names to values will always be case-sensitive
     * regardless of the platform, i.e. <code>getSystemEnvVars().get("path")</code>
     * and <code>getSystemEnvVars().get("PATH")</code> will in general return
     * different values. However, on platforms with case-insensitive environment
     * variables like Windows, all variable names will be normalized to upper case.
     *
     * @return The shell environment variables, can be empty but never
     *         <code>null</code>.
     * @throws IOException If the environment variables could not be queried from
     *                     the shell.
     * @see System#getenv() System.getenv() API, new in JDK 5.0, to get the same
     *      result <b>since 2.0.2 System#getenv() will be used if available in the
     *      current running jvm.</b>
     */
    public static Map<String, String> getSystemEnvVars() throws IOException
    {
        return getSystemEnvVars( !OS.isFamilyWindows() );
    }

    /**
     * Return the shell environment variables. If
     * <code>caseSensitive == true</code>, then envar keys will all be upper-case.
     *
     * @param caseSensitive Whether environment variable keys should be treated
     *                      case-sensitively.
     * @return Properties object of (possibly modified) envar keys mapped to their
     *         values.
     * @throws IOException .
     * @see System#getenv() System.getenv() API, new in JDK 5.0, to get the same
     *      result <b>since 2.0.2 System#getenv() will be used if available in the
     *      current running jvm.</b>
     */
    public static Map<String, String> getSystemEnvVars( boolean caseSensitive )
    {
        Map<String, String> envVars = new HashMap<String, String>();
        Map<String, String> envs = System.getenv();
        for ( Map.Entry<String, String> entry : envs.entrySet() )
        {
            String key = entry.getKey();
            String value = entry.getValue();
            if ( !caseSensitive )
            {
                key = key.toUpperCase( Locale.ENGLISH );
            }
            envVars.put( key, value );
        }
        return envVars;
    }

    public static String findExecutablePath( String executableName, Map<String, String> enviro, File dir )
    {
        File execFile = new File( executableName );
        String exec = null;
        if ( execFile.exists() && execFile.isFile() && execFile.canExecute() )
        {
            // getLog().debug( "Toolchains are ignored, 'executable' parameter is set to " +
            // execFile.getAbsolutePath() );
            exec = execFile.getAbsolutePath();
        }
        else
        {
            if ( OS.isFamilyWindows() )
            {
                String ex = executableName.indexOf( '.' ) < 0 ? executableName + ".bat" : executableName;
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
        return exec;
    }

    public static CommandLine getExecutablePath( String executableName, Map<String, String> enviro, File dir )
    {
        String exec = findExecutablePath( executableName, enviro, dir );

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

    public static int executeCommandLine( Executor exec, CommandLine commandLine, Map<String, String> enviro,
        OutputStream out, OutputStream err, InputStream in ) throws ExecuteException, IOException
    {
        final Log log = new SystemStreamLog();
        exec.setExitValues( null ); // let us decide of exit value
        // exec.setStreamHandler(new PumpStreamHandler(out, err, in));
        if ( in != System.in )
        {
            log.debug( "executeCommandLine : full stream handler for InputStream" );
            exec.setStreamHandler( new PumpStreamHandler( out, err, in ) );
        }
        else if ( out != System.out || err != System.err )
        {
            log.debug( "executeCommandLine : minimal stream handler for outputstream" );
            exec.setStreamHandler( new PumpStreamHandler( out, err ) );
        }

        log.debug( "executeCommandLine in" );
        int res = exec.execute( commandLine, enviro );
        log.debug( "executeCommandLine out" );
        return res;
    }

    public static String[] translateCommandline( String toProcess ) throws ParseException
    {
        if ( ( toProcess == null ) || ( toProcess.length() == 0 ) )
        {
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer( toProcess, "\"\' ", true );
        Vector<String> v = new Vector<String>();
        StringBuilder current = new StringBuilder();

        while ( tok.hasMoreTokens() )
        {
            String nextTok = tok.nextToken();
            switch ( state )
            {
            case inQuote:
                if ( "\'".equals( nextTok ) )
                {
                    state = normal;
                }
                else
                {
                    current.append( nextTok );
                }
                break;
            case inDoubleQuote:
                if ( "\"".equals( nextTok ) )
                {
                    state = normal;
                }
                else
                {
                    current.append( nextTok );
                }
                break;
            default:
                if ( "\'".equals( nextTok ) )
                {
                    state = inQuote;
                }
                else if ( "\"".equals( nextTok ) )
                {
                    state = inDoubleQuote;
                }
                else if ( " ".equals( nextTok ) )
                {
                    if ( current.length() != 0 )
                    {
                        v.addElement( current.toString() );
                        current.setLength( 0 );
                    }
                }
                else
                {
                    current.append( nextTok );
                }
                break;
            }
        }

        if ( current.length() != 0 )
        {
            v.addElement( current.toString() );
        }

        if ( ( state == inQuote ) || ( state == inDoubleQuote ) )
        {
            throw new ParseException( "unbalanced quotes in " + toProcess, current.length() );
        }

        String[] args = new String[v.size()];
        v.copyInto( args );
        return args;
    }

}
