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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Goal which qmakes workspace.
 *
 * @author Franck Bonin 
 */
@Mojo( name = "qmake", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class QMakeMojo extends AbstractLaunchMojo
{

    protected List getArgsList()
    {
        return null;
    }
    
    /**
     * directory were sources are
     * 
     * @since 0.0.4
     */
    @Parameter()
    private List sourceDirs = new ArrayList();

    /**
     * qmake project file list
     * Can be empty, then sourceDirs will so be used.
     * If sourceDirs is empty to, then basedir will be used.
     * 
     * @since 0.0.5
     */
    @Parameter()
    private List pros = new ArrayList();
    
    protected List getProjectList()
    {
        if ( pros.size() == 0 )
        {
            Iterator it = sourceDirs.iterator();
            while( it.hasNext() )
            {
                pros.add( new File( (String) it.next() ).getAbsolutePath() );
            }
        }
        if ( pros.size() == 0 )
        {
            pros.add( new String( basedir.getAbsolutePath() ) );
        }
        return pros;
    }
    
    /**
     * Arguments for qmake program
     * 
     * @since 0.0.5
     */
    @Parameter( property = "qmake.args", defaultValue = "" )
    private String commandArgs;
    
    protected String getCommandArgs()
    {
        String result = new String( "-makefile " );
        if ( !StringUtils.isEmpty( commandArgs ) )
        {
            result += commandArgs + " ";
        }
        List proList = getProjectList();
        Iterator it = proList.iterator();
        while( it.hasNext() )
        {
            result += "\"" + (String) it.next() + "\" ";
        }
        return result;
    }
    
    /**
     * qmake commands names per OS name
     * os name match is java.lang.System.getProperty( "os.name" )
     * 
     * @since 0.0.5
     */
    @Parameter()
    private Map qmakecommandPerOS = new HashMap();
    
    protected String getExecutable()
    {
        String sOsName = System.getProperty( "os.name" );
        sOsName = sOsName.replace( " ", "" );
        getLog().info( "os.name is \"" + sOsName + "\"" );
        if ( qmakecommandPerOS == null )
        {
            return "qmake";
        }
        else
        {
            if ( qmakecommandPerOS.containsKey( sOsName ) )
            {
                return (String) qmakecommandPerOS.get( sOsName );
            }
            else
            {
                return "qmake";
            }
        }
    }
    
    /**
     * Environment variables passed to qmake program.
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

    /**
     * Out of source directory
     * 
     * @since 0.0.5
     */
    @Parameter( property = "qmake.outsourcedir")
    private File outsourceDir;
    
    protected File getWorkingDir()
    {
        if ( null == outsourceDir )
        {
            outsourceDir = new File( basedir.getPath() );
        }
        return outsourceDir;
    }

    public boolean isSkip()
    {
        return false;
    }

}
