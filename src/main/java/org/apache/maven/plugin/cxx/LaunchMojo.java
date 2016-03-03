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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which Launch an external executable.
 *
 * @author Franck Bonin 
 */
@Mojo( name = "launch" )
public class LaunchMojo extends AbstractLaunchMojo
{

    /**
     * Can be of type <code>&lt;argument&gt;</code> or <code>&lt;classpath&gt;</code> Can be overriden using "cxx.args"
     * env. variable
     * 
     * @since 0.0.4
     */
    @Parameter()
    private List<String> arguments;
    
    
    @Override
    protected List<String> getArgsList()
    {
        return arguments;
    }

    /**
     * Arguments for the executed program
     * 
     */
    @Parameter( property = "launch.args" )
    private String commandArgs;
    
    @Override
    protected String getCommandArgs()
    {
        return commandArgs;
    }

    /**
     * The executable. Can be a full path or a the name executable. In the latter case, the executable must be in the
     * PATH for the execution to work.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "launch.executable", required = true )
    private String executable;
    
    @Override
    protected String getExecutable()
    {
        return executable;
    }

    /**
     * Environment variables to pass to the executed program.
     * 
     * @since 0.0.4
     */
    @Parameter()
    private Map environmentVariables = new HashMap();
    
    @Override
    protected Map getMoreEnvironmentVariables()
    {
        return environmentVariables;
    }

    /**
     * Exit codes to be resolved as successful execution for non-compliant applications (applications not returning 0
     * for success).
     * 
     * @since 0.0.4
     */
    @Parameter()
    private List<String> successCodes;
    
    @Override
    protected List<String> getSuccesCode()
    {
        return successCodes;
    }

    /**
     * The current working directory. Optional. If not specified, basedir will be used.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "launch.workingdir" )
    private File workingDir;
    
    @Override
    protected File getWorkingDir()
    {
        if ( null == workingDir )
        {
            workingDir = new File( basedir.getPath() );
        }
        return workingDir;
    }

    /**
     * Os for which command shall be executed
     * os name match is java.lang.System.getProperty( "os.name" )
     * 
     * @since 0.0.4
     */
    @Parameter()
    private List includeOS;
    
    /**
     * Os for which command shall not be executed
     * os name match is java.lang.System.getProperty( "os.name" )
     *  
     * @since 0.0.4
     */
    @Parameter()
    private List excludeOS;
    
    @Override
    protected boolean isSkip()
    {
        String sOsName = System.getProperty( "os.name" );
        sOsName = sOsName.replace( " ", "" );
        getLog().info( "os.name is \"" + sOsName + "\"" );
        if ( null == excludeOS && null == includeOS )
        {
            return false;
        }
        else if ( null == includeOS )
        {
            return excludeOS.contains( sOsName );
        }
        else if ( null == excludeOS )
        {
            return !includeOS.contains( sOsName );
        }
        else
        {
            return ( excludeOS.contains( sOsName ) || !includeOS.contains( sOsName ) );
        }
    }
    
    /**
     * The OutputStreamOut Location.
     * 
     * @since 0.0.5
     */
    @Parameter( property = "launch.outputStreamOut", defaultValue = "" )
    private File outputStreamOut;
    
    @Override
    protected OutputStream getOutputStreamOut()
    {
        String sOutputStreamOut = new String();
        if ( null != outputStreamOut && !outputStreamOut.toString().isEmpty() )
        {
            if ( outputStreamOut.isAbsolute() )
            {
                sOutputStreamOut = outputStreamOut.getPath();
            }
            else
            {
                sOutputStreamOut = basedir.getAbsolutePath() + File.separator + outputStreamOut.getPath();
            }
            
            getLog().info( "Launch output location " + sOutputStreamOut );
             
            OutputStream output = super.getOutputStreamOut();
            File file = new File( sOutputStreamOut );
            try
            {
                new File( file.getParent() ).mkdirs();
                file.createNewFile();
                output = new FileOutputStream( file );
            }
            catch ( IOException e )
            {
                getLog().error( "Launch report redirected to stout since " + sOutputStreamOut + " can't be opened" );
            }
            return output;
        }
        else
        {
            return super.getOutputStreamOut();
        }
    }
    
    /**
     * The OutputStreamErr Location.
     * 
     * @since 0.0.5
     */
    @Parameter( property = "launch.outputStreamErr", defaultValue = "" )
    private File outputStreamErr;
    
    @Override
    protected OutputStream getOutputStreamErr()
    {
        String sOutputStreamErr = new String();
        if ( null != outputStreamErr && !outputStreamErr.toString().isEmpty() )
        {
            if ( outputStreamErr.isAbsolute() )
            {
                sOutputStreamErr = outputStreamErr.getPath();
            }
            else
            {
                sOutputStreamErr = basedir.getAbsolutePath() + File.separator + outputStreamErr.getPath();
            }
            
            getLog().info( "Launch erroutput location " + sOutputStreamErr );
             
            OutputStream output = super.getOutputStreamErr();
            File file = new File( sOutputStreamErr );
            try
            {
                new File( file.getParent() ).mkdirs();
                file.createNewFile();
                output = new FileOutputStream( file );
            }
            catch ( IOException e )
            {
                getLog().error( "Launch report redirected to stout since " + sOutputStreamErr + " can't be opened" );
            }
            return output;
        }
        else
        {
            return super.getOutputStreamErr();
        }
    }
    
    /**
     * The InputStream Location.
     * 
     * @since 0.0.5
     */
    @Parameter( property = "launch.inputStream", defaultValue = "" )
    private File inputStream;
    
    @Override
    protected InputStream getInputStream()
    {
        String sInputStream = new String();
        if ( null != inputStream && !inputStream.toString().isEmpty() )
        {
            if ( inputStream.isAbsolute() )
            {
                sInputStream = inputStream.getPath();
            }
            else
            {
                sInputStream = basedir.getAbsolutePath() + File.separator + inputStream.getPath();
            }
            
            getLog().info( "Launch input location " + sInputStream );
             
            InputStream input = super.getInputStream();
            File file = new File( sInputStream );
            try
            {
                new File( file.getParent() ).mkdirs();
                file.createNewFile();
                input = new FileInputStream( file );
            }
            catch ( IOException e )
            {
                getLog().error( "Launch report redirected to stout since " + sInputStream + " can't be opened" );
            }
            return input;
        }
        else
        {
            return super.getInputStream();
        }
    }
}
