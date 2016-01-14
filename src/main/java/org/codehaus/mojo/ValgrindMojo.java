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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which valgrind executables.
 *
 * @author Franck Bonin 
 */
@Mojo( name = "valgrind", defaultPhase = LifecyclePhase.TEST )
public class ValgrindMojo extends AbstractLaunchMojo
{
    protected List getArgsList()
    {
        return null;
    }

    /**
     * The Report OutputFile Location.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "valgrind.reportsfilePath", defaultValue = "valgrind-reports" )
    private File reportsfileDir;

    /**
     * The Report OutputFile name identifier.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "valgrind.reportIdentifier", defaultValue = "" )
    private String reportIdentifier;
    
    private String getReportFileName()
    {
        return "valgrind-result-" + reportIdentifier + ".xml";
    }
    /**
     * Arguments for valgrind program. Shall be --leak-check=yes --demangle=yes --xml=yes
     * 
     */
    @Parameter( property = "valgrind.args", defaultValue = "--leak-check=yes --demangle=yes --xml=yes" )
    private String commandArgs;
    
    protected String getCommandArgs()
    {
        String params = commandArgs + " ";
        String OutputReportName = new String();
        if ( reportsfileDir.isAbsolute() )
        {
            OutputReportName = reportsfileDir.getAbsolutePath() + "/" + getReportFileName();
        }
        else
        {
            OutputReportName = basedir.getAbsolutePath() + "/" + reportsfileDir.getPath() + "/" + getReportFileName();
        }
        File file = new File( OutputReportName );
        new File( file.getParent() ).mkdirs();
        
        params += "--xml-file=\"" + OutputReportName + "\" ";
        
        params += "\"" + instrumentedExecutablePath + "\" " + instrumentedExecutableArgs;
                
        return params;
    }
    
    /**
     * Path to executed (tested) program 
     * 
     */
    @Parameter( property = "valgrind.instrumented", required = true )
    private String instrumentedExecutablePath;
    
    /**
     * Arguments of executed program 
     * 
     */
    @Parameter( property = "valgrind.instrumentedArgs", defaultValue = " " )
    private String instrumentedExecutableArgs;
    
    protected String getExecutable()
    {
        return "valgrind";
    }

    /**
     * Environment variables passed to valgrind program.
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
    @Parameter( property = "valgrind.workingdir" )
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
    * Set this to "true" to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable it
    * using the "maven.test.skip" property, because maven.test.skip shall disables both running the tests and
    * compiling the tests. Consider using the <code>skipTests</code> parameter instead.
    *
    * @since 0.0.5
    */
    @Parameter( property = "maven.test.skip", defaultValue = "false" )
    protected boolean skip;
    
    protected boolean isSkip()
    {
        return _isSkip() || skipTests || skip;
    }

    protected boolean _isSkip()
    {
        String sOsName = System.getProperty( "os.name" );
        // vilgrind will work on linux, macos, everything but windows
        return sOsName.contains( "windows" );
    }

}
