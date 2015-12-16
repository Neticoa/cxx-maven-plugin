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
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;

import org.apache.maven.plugin.MojoExecutionException;

/* Use enhanced FileSet and FileManager (not the one provided in this project)*/
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

/**
 * Goal which cmakes workspace.
 *
 * @author Franck Bonin 
 * @goal cmake
 * @phase generate-sources
 * 
 */
public class CMakeMojo extends AbstractLaunchMojo
{

    protected List getArgsList()
    {
        return null;
    }

    /**
     * Directory location of main CMakeList.txt, argument for cmake command 
     * Defaut set to ${basedir}
     * 
     * @parameter expression="${cmake.projectdir}"
     * @since 0.0.4
     */
    private String projectDir;
    
    protected String getProjectDir()
    {
        if ( null == projectDir )
        {
            projectDir = new String( basedir.getAbsolutePath() );
        }
        return projectDir;
    }
    
    /**
     * Generator name, arguments for cmake command
     * 
     * @parameter expression="${cmake.generator}"
     * @required
     * @since 0.0.4
     */
    private String generator;
    
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${cmake.args}"
     * @since 0.0.4
     */
    private String commandArgs;
    
    protected String addCMakeDefinition(String sCMakeName, String sMavenValue)
    {
        String sResult = null;
        if (! StringUtils.isEmpty(sCMakeName) && ! StringUtils.isEmpty(sMavenValue))
        {
            sResult = " -D" + sCMakeName + "=\"" + sMavenValue + "\"";
        }
        else
        {
            sResult = new String();
        }
        return sResult;
    }
    
    protected String getCommandArgs()
    {
        String result = new String();
        if ( !StringUtils.isEmpty( commandArgs ) )
        {
            result = commandArgs + " ";
        }
        result += "\"" + getProjectDir() + "\" -G " + generator;
        result += addCMakeDefinition("CMAKE_BUILD_TYPE", buildConfig);
        result += addCMakeDefinition("TARGET_ID", project.getArtifactId());
        result += addCMakeDefinition("TARGET_NAME", project.getName());
        result += addCMakeDefinition("DEPENDENCY_DIR", getProjectDependenciesDirectory());
        result += addCMakeDefinition("TARGET_VERSION", project.getVersion());
        result += addCMakeDefinition("TARGET_CLASSIFIER", targetClassifier);
        result += addCMakeDefinition("TARGET_PLATFORM", targetPlatform);
        result += addCMakeDefinition("TARGET_ARCHITECTURE", targetArchitecture);
        result += addCMakeDefinition("executableSuffix", executableSuffix);
        result += addCMakeDefinition("sharedLibraryPrefix", sharedLibraryPrefix);
        result += addCMakeDefinition("sharedLibrarySuffix", sharedLibrarySuffix);
        result += addCMakeDefinition("sharedModulePrefix", sharedModulePrefix);
        result += addCMakeDefinition("sharedModuleSuffix", sharedModuleSuffix);
        result += addCMakeDefinition("staticLibraryPrefix", staticLibraryPrefix);
        result += addCMakeDefinition("staticLibrarySuffix", staticLibrarySuffix);
        return result;
    }
    


    protected String getExecutable()
    {
        return "cmake";
    }

    
    /**
     * Environment variables to pass to the cmake program.
     * 
     * @parameter
     * @since 0.0.4
     */
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
     * Defaut set to ${basedir}
     * 
     * @parameter expression="${cmake.outsourcedir}"
     * @since 0.0.4
     */
    private File outsourceDir;
    
    protected File getWorkingDir()
    {
        if (null == outsourceDir)
        {
            outsourceDir = new File( basedir.getPath() );
        }
        return outsourceDir;
    }

    public boolean isSkip()
    {
        return false;
    }
    
    protected String getProjectDependenciesDirectory()
    {
        // project.getProperties().setProperty("toto", "toto");
        //String projectBuildDirectory = project.getProperties().getProperty("project.build.directory");
        //String projectBuildDirectory = System.getProperty("project.build.directory");
        //String projectBuildDirectory = project.getModel().getBuild().getDirectory();
        String projectBuildDirectory = project.getBuild().getDirectory();
        if ( StringUtils.isEmpty( projectBuildDirectory ) )
        {
           projectBuildDirectory = basedir.getAbsolutePath() + "/target";
        }         
        return projectBuildDirectory + "/dependency";
    }
    
    protected String getProjectDependenciesTargetClassifierDirectory()
    {
        return getProjectDependenciesDirectory() + "/" + targetClassifier;
    }
    
    /**
     * generate a cmake external dependency file according to 
     * 
     * @parameter expression="${cmake.injectMavenDependencies}" default-value=true
     * @since 0.0.6
     */
    private boolean injectMavenDependencies;
    
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${cmake.mavenDependenciesFile}"  default-value="CMakeMavenDependencies.txt"
     * @since 0.0.6
     */
    private String mavenDependenciesFile;
    
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${buildConfig}"  default-value=""
     * @since 0.0.6
     */
    private String buildConfig;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${targetClassifier}"  default-value=""
     * @since 0.0.6
     */
    private String targetClassifier;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${targetPlatform}"  default-value=""
     * @since 0.0.6
     */
    private String targetPlatform;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${targetArchitecture}"  default-value=""
     * @since 0.0.6
     */
    private String targetArchitecture;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${executableSuffix}"  default-value=""
     * @since 0.0.6
     */
    private String executableSuffix;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${sharedLibraryPrefix}"  default-value=""
     * @since 0.0.6
     */
    private String sharedLibraryPrefix;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${sharedLibrarySuffix}"  default-value=""
     * @since 0.0.6
     */
    private String sharedLibrarySuffix;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${sharedModulePrefix}"  default-value=""
     * @since 0.0.6
     */
    private String sharedModulePrefix;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${sharedModuleSuffix}"  default-value=""
     * @since 0.0.6
     */
    private String sharedModuleSuffix;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${staticLibraryPrefix}"  default-value=""
     * @since 0.0.6
     */
    private String staticLibraryPrefix;
    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${staticLibrarySuffix}"  default-value=""
     * @since 0.0.6
     */
    private String staticLibrarySuffix;
    
    /**
     * directory were bin debug dependencies are
     * default location is ${project.build.directory}/dependency/${targetClassifier}/debug
     * 
     * @parameter
     * @since 0.0.6
     */
    private List debugDependenciesRoots = new ArrayList();
    
    /**
     * directory were bin release dependencies are
     * default location is ${project.build.directory}/dependency/${targetClassifier}/release
     * 
     * @parameter
     * @since 0.0.6
     */
    private List releaseDependenciesRoots = new ArrayList();
    
    protected void findDependencies(List ai_dependenciesRoots, List ao_dependenciesLib)
    {
        Iterator itRoots = ai_dependenciesRoots.iterator();
        while( itRoots.hasNext() )
        {
            String dependencyRoot = new String((String) itRoots.next());
            /*File dependencyRootDir = new File( dependencyRoot );
            String[] subDirectories = dependencyRootDir.list(new FilenameFilter()
            {
                @Override
                public boolean accept(File current, String name)
                {
                    return new File(current, name).isDirectory();
                }
            });
            
            for (int i = 0; null != subDirectories && i < subDirectories.length; i++)
            {
                String mod = subDirectories[i];
                getLog().info( "Found dependencies Mod : " + mod );
                ao_dependenciesMod.add(mod);
            }*/

            FileSet afileSet = new FileSet();
            afileSet.setDirectory( dependencyRoot );
            
            getLog().info( "Search for **/*.so from " + afileSet.getDirectory() );
            afileSet.setIncludes( Arrays.asList( new String[]{"**/*.so", "**/*.dll", "**/*.lib", "**/*.a"} ) );
            
            FileSetManager aFileSetManager = new FileSetManager();
            String[] found = aFileSetManager.getIncludedFiles( afileSet );
            
            for (int j = 0; null != found && j < found.length; j++)
            {
                String lib = dependencyRoot + "/" + found[j];
                getLog().info( "Found dependencies Lib : " + lib );
                ao_dependenciesLib.add(lib);
            }
        }
    }
    
    protected void preExecute(Executor exec, CommandLine commandLine, Map enviro) throws MojoExecutionException
    {
        if (injectMavenDependencies)
        {
            if (releaseDependenciesRoots.isEmpty())
            {
                releaseDependenciesRoots.add(getProjectDependenciesTargetClassifierDirectory() + "/release");
                getLog().info( "use default release Dependency Root: \"" + releaseDependenciesRoots.get(0) + "\"" );  
            }
            if (debugDependenciesRoots.isEmpty())
            {
                debugDependenciesRoots.add(getProjectDependenciesTargetClassifierDirectory() + "/debug");
                getLog().info( "use default debug Dependency Root: \"" + debugDependenciesRoots.get(0) + "\"" );  
            }
            
            ArrayList dependenciesLibRelease = new ArrayList();
            findDependencies(releaseDependenciesRoots, dependenciesLibRelease);
            
            ArrayList dependenciesLibDebug = new ArrayList();
            findDependencies(debugDependenciesRoots, dependenciesLibDebug);
        }
    }
}
