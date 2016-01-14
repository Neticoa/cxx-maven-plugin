package org.codehaus.mojo;
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
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which build VisualStudio solutions.
 *
 * @author Franck Bonin 
 */
@Mojo( name = "msbuild", defaultPhase = LifecyclePhase.COMPILE )
public class VisualStudioMojo extends AbstractLaunchMojo
{

    /**
     * Directory location of visual studio solution default set to baseDir
     * 
     * @since 0.0.5
     */
    @Parameter( property = "visualstudio.solutionDir" )
    private String solutionDir;
    
    protected String getSolutionDir()
    {
        if ( null == solutionDir )
        {
            solutionDir = new String( basedir.getAbsolutePath() );
        }
        return solutionDir;
    }
    
    /**
     * Visual studio solution file name.
     * 
     * @since 0.0.5
     */
    @Parameter( property = "visualstudio.solutionFileName", required = true )
    private String solutionFileName;
    
    /**
     * Build type [ rebuild | clean | build | ... ]
     * 
     * @since 0.0.5
     */
    @Parameter( property = "visualstudio.buildType", defaultValue = "build" )
    private String buildType;
    
    /**
     * Build config [ debug | release | ... ]
     * 
     * @since 0.0.5
     */
    @Parameter( property = "visualstudio.buildConfig", defaultValue = "release" )
    private String buildConfig;
    
    /**
     * target platform  [ win32 | ... ]
     * 
     * @since 0.0.5
     */
    @Parameter( property = "visualstudio.targetPlatform", defaultValue = "win32" )
    private String targetPlatform;
    
    /**
     * target architecture  [ x86 | amd64 | ia64 | x86_amd64 | x86_ia64 ]
     * 
     * @since 0.0.5
     */
    @Parameter( property = "visualstudio.targetArchitecture", defaultValue = "x86" )
    private String targetArchitecture;
    
    /**
     * Build version in visual studio format [ x.y.z.t ]
     * 
     * @since 0.0.5
     */
    @Parameter( property = "visualstudio.buildVersion", defaultValue = "0.0.0.1" )
    private String buildVersion;
    
    protected String visualStudioBuildVersion()
    {
        Pattern p = Pattern.compile( "(\\d+\\.)+\\d+" );
        Matcher m = p.matcher( buildVersion );
        if ( m.find() ) 
        {
            getLog().debug( "Visual Studio compatible buildVersion match is " + m.group() );
            return m.group();
        }
        else
        {
            getLog().debug( "Visual Studio 'as is' buildVersion is " + buildVersion );
            return buildVersion;
        }
    }
    
    protected String buildVersionExtension()
    {
        Pattern p = Pattern.compile( "(\\d+\\.)+\\d+((-[^-\\s]+)+)" );
        Matcher m = p.matcher( buildVersion );
        if ( m.matches() && m.groupCount() >= 3 ) 
        {
            String versionExtension = m.group( 2 );
            getLog().debug( "Visual Studio compatible Extension version is " + versionExtension );
            if ( versionExtension.contains( "SNAPSHOT" ) )
            {
                return versionExtension.replaceAll( "SNAPSHOT",
                    ( new SimpleDateFormat( "yyyyMMdd.HHmmss" ) ).format( new Date() ) );
            }
            else
            {
                return versionExtension;
            }
        }
        else
        {
            getLog().debug( "Visual Studio compatible Extension version is empty" );
            return "\"\\0\""; // this \0 will go through batch script, env var and C++/RCC preprocessor
        }
    }
    
    /**
     * Additional compiler options (without any global quotation)
     * 
     * @since 0.0.5
     */
    @Parameter( property = "visualstudio.compilerOptions", defaultValue = "" )
    private String compilerOptions;
    
    protected String getCommandArgs()
    {
        return null;
    }
    
    protected List getArgsList()
    {
        ArrayList args = new ArrayList();
        
        args.add( getSolutionDir() );
        args.add( solutionFileName );
        args.add( buildType );
        args.add( buildConfig );
        args.add( targetPlatform );
        args.add( targetArchitecture );
        args.add( visualStudioBuildVersion() );
        args.add( buildVersionExtension() );
        if ( StringUtils.isNotEmpty( compilerOptions ) )
        {
            args.add( "\"" + compilerOptions + "\"" );
        }
        
        return args;
    }
    
    protected String getExecutable()
    {
        InputStream batchScriptStream = getClass().getResourceAsStream( "/build.bat" );
        try
        {    
            File batchFile = File.createTempFile( "build", ".bat" );
            batchFile.deleteOnExit();
            OutputStream outStream = new FileOutputStream( batchFile );
            
            IOUtils.copy( batchScriptStream, outStream );
            
            getLog().debug( "msbuild batch script is located at :" + batchFile.getAbsolutePath() );
                
            return batchFile.getAbsolutePath();
        }
        catch ( IOException e )
        {
            getLog().error( "Temporary batch script can't be created" );
            return "echo";
        }
    }

    /**
     * Environment variables to pass to the msbuild program.
     * 
     * @since 0.0.5
     */
    @Parameter()
    private Map environmentVariables = new HashMap();
    protected Map getMoreEnvironmentVariables()
    {
        return environmentVariables;
    }

    protected List getSuccesCode()
    {
        return null;
    }
    
    protected File getWorkingDir()
    {
        return new File( solutionDir );
    }

    public boolean isSkip()
    {
        return false;
    }

}
