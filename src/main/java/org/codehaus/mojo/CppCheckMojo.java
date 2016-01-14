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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

/* Use enhanced FileSet and FileManager (not the one provided in this project)*/
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which cppcheck sources.
 *
 * @author Franck Bonin 
 */
@Mojo( name = "cppcheck", defaultPhase = LifecyclePhase.TEST )
public class CppCheckMojo extends AbstractLaunchMojo
{
    protected List getArgsList()
    {
        return null;
    }
    
    /**
     * Directory where cppcheck should search for source files
     * 
     * @since 0.0.4
     */
    @Parameter()
    private List sourceDirs = new ArrayList();
    
    /**
     * Directory where included file shall be found
     * 
     * @since 0.0.4
     */
    @Parameter( )
    private List includeDirs = new ArrayList();
    
    /**
     *  Excludes files/folder from analysis (comma separated list of filter)
     *
     *  @since 0.0.5
     */
    @Parameter( property = "cppcheck.excludes", defaultValue = "" )
    private String excludes;
    
   /**
     * The Report OutputFile Location.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "cppcheck.reportsfilePath", defaultValue = "cppcheck-reports" )
    private File reportsfileDir;
    
    /**
     * The Report OutputFile name identifier.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "cppcheck.reportIdentifier", defaultValue = "" )
    private String reportIdentifier;
    
    private String getReportFileName()
    {
        return "cppcheck-result-" + reportIdentifier + ".xml";
    }
    
    /**
     * Arguments for the cppcheck program. Shall be -v --enable=style --force --xml 
     * 
     */
    @Parameter( property = "cppcheck.args", defaultValue = "-v --enable=style --force --xml" )
    private String commandArgs;

    protected String getCommandArgs()
    {
        String params = commandArgs + " ";
        HashSet<String> excudedSet = new HashSet<String>();
        
        Iterator it = includeDirs.iterator();
        while ( it.hasNext() )
        {
            FileSet afileSet = new FileSet();
            
            String dir = it.next().toString();
            params += "-I\"" + dir + "\" ";
            
            if ( StringUtils.isNotEmpty( excludes ) )
            {
                afileSet.setDirectory( new File( dir ).getAbsolutePath() );
                // $FB pour éviter d'avoir TROP de fichier excludes (inutiles) dans la boucle for ci-après
                afileSet.setUseDefaultExcludes( false ); 
                afileSet.setExcludes( Arrays.asList( excludes.split( "," ) ) );
                getLog().debug( "cppcheck excludes are :" + Arrays.toString( afileSet.getExcludes().toArray() ) );
                
                FileSetManager aFileSetManager = new FileSetManager();
                String[] found = aFileSetManager.getExcludedFiles( afileSet );
                excudedSet.addAll( new HashSet<String>( Arrays.asList( found ) ) );
            }
        }
        it = sourceDirs.iterator();
        while ( it.hasNext() )
        {
            FileSet afileSet = new FileSet();
            
            String dir = it.next().toString();
            params += "-I\"" + dir + "\" ";
            
            if ( StringUtils.isNotEmpty( excludes ) )
            {
                afileSet.setDirectory( new File( dir ).getAbsolutePath() );
                // $FB pour éviter d'avoir TROP de fichiers exclude (inutile) dans la boucle for ci-après
                afileSet.setUseDefaultExcludes( false ); 
                afileSet.setExcludes( Arrays.asList( excludes.split( "," ) ) );
                getLog().debug( "cppcheck excludes are :" + Arrays.toString( afileSet.getExcludes().toArray() ) );
                
                FileSetManager aFileSetManager = new FileSetManager();
                String[] found = aFileSetManager.getExcludedFiles( afileSet );
                excudedSet.addAll( new HashSet<String>( Arrays.asList( found ) ) );
            }
        }
        for ( Iterator<String> iter = excudedSet.iterator(); iter.hasNext(); )
        {
            String s = iter.next();
            //cppcheck only check *.cpp, *.cxx, *.cc, *.c++, *.c, *.tpp, and *.txx files
            // so remove unneeded exclusions
            if ( s.matches( "(.+\\.cpp)|(.+\\.cxx)|(.+\\.cc)|(.+\\.c\\+\\+)|(.+\\.c)|(.+\\.tpp)|(.+\\.txx)" ) )
            {
                params += "-i\"" + s + "\" ";
            }
        }
        
        it = sourceDirs.iterator();
        while ( it.hasNext() )
        {
            params += "\"" + it.next() + "\" ";
        }
        
        return params;
    }
    
    //override
    protected OutputStream getOutputStreamErr()
    {
        String outputReportName = new String();
        if ( reportsfileDir.isAbsolute() )
        {
            outputReportName = reportsfileDir.getAbsolutePath() + File.separator + getReportFileName();
        }
        else
        {
            outputReportName = basedir.getAbsolutePath() + File.separator + reportsfileDir.getPath()
            + File.separator + getReportFileName();
        }
        
        getLog().info( "Cppcheck report location " + outputReportName );
         
        OutputStream output = System.err;
        File file = new File( outputReportName );
        try
        {
            new File( file.getParent() ).mkdirs();
            file.createNewFile();
            output = new FileOutputStream( file );
        }
        catch ( IOException e )
        {
            getLog().error( "Cppcheck report redirected to stderr since " + outputReportName + " can't be opened" );
        }

        return output;
    }

    protected String getExecutable()
    {
        return "cppcheck";
    }

    /**
     * Environment variables to pass to cppcheck program.
     * 
     * @since 0.0.4
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

    /**
     * The current working directory. Optional. If not specified, basedir will be used.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "cppcheck.workingdir" )
    private File workingDir;
    
    protected File getWorkingDir()
    {
        if ( null == workingDir )
        {
            workingDir = new File( basedir.getPath() );
        }
        return workingDir;
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
    
    protected boolean isSkip()
    {
        return skipTests || skip;
    }
}
