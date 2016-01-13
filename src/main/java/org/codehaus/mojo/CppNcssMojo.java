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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Goal which cppncss sources.
 *
 * @author Franck Bonin 
 */
@Mojo( name = "cppncss", defaultPhase = LifecyclePhase.TEST )
public class CppNcssMojo extends AbstractLaunchMojo
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
     * The Report OutputFile Location.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "cppncss.reportsfilePath", defaultValue = "cppncss-reports" )
    private File reportsfileDir;

    /**
     * The Report OutputFile name identifier.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "cppncss.reportIdentifier", defaultValue = "" )
    private String reportIdentifier;
    
    private String getReportFileName()
    {
        return "cppncss-result-" + reportIdentifier + ".xml";
    }

    /**
     * define substitution values (value can be empty).
     * 
     * @since 0.0.4
     */
    @Parameter()
    private Map definitions = new HashMap();
    
    /**
     * Macro definition substitution values (value can be empty).
     * 
     * @since 0.0.4
     */
    @Parameter()
    private Map macros = new HashMap();
    

    /**
     * Arguments for cppncss program. Shall be -r -v -x -k
     * 
     */
    @Parameter( property = "cppncss.args", defaultValue = "-r -v -x -k" )
    private String commandArgs;
    
    protected String getCommandArgs()
    {
        String params = commandArgs + " ";
        
        String OutputReportName = new String();
        if ( reportsfileDir.isAbsolute() )
        {
            OutputReportName = reportsfileDir.getAbsolutePath() + File.separator + getReportFileName();
        }
        else
        {
            OutputReportName = basedir.getAbsolutePath() + File.separator + reportsfileDir.getPath() + File.separator + getReportFileName();
        }
        File file = new File( OutputReportName );
        new File( file.getParent() ).mkdirs();
        
        params += "-f=\"" + OutputReportName + "\" ";
        
        Iterator it = macros.keySet().iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();
            String value = (String) macros.get( key );
            if ( StringUtils.isEmpty(value) )
            {
                params += "-M" + key + " ";
            }
            else
            {
                params += "-M" + key + "=\"" + value + "\" ";
            }
        }
        
        it = definitions.keySet().iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();
            String value = (String) definitions.get( key );
            if ( StringUtils.isEmpty( value ) )
            {
                params += "-D" + key + " ";
            }
            else
            {
                params += "-D" + key + "=\"" + value + "\" ";
            }
        }
        
        it = sourceDirs.iterator();
        while( it.hasNext() )
        {
            params += "\"" + it.next() + "\" ";
        }
        
        return params;
    }

    protected String getExecutable()
    {
        return "cppncss";
    }

    /**
     * Environment variables to pass to cppncss program.
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
    @Parameter( property = "cppncss.workingdir" )
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
     * Set this to "true" to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable it using
     * the "maven.test.skip" property, because maven.test.skip shall disables both running the tests and compiling the tests.
     * Consider using the <code>skipTests</code> parameter instead.
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
