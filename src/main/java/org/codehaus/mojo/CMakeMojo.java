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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
        result += addCMakeDefinition("EXECUTABLE_SUFFIX", executableSuffix);
        result += addCMakeDefinition("SHARED_LIBRARY_PREFIX", sharedLibraryPrefix);
        result += addCMakeDefinition("SHARED_LIBRARY_SUFFIX", sharedLibrarySuffix);
        result += addCMakeDefinition("SHARED_MODULE_PREFIX", sharedModulePrefix);
        result += addCMakeDefinition("SHARED_MODULE_SUFFIX", sharedModuleSuffix);
        result += addCMakeDefinition("STATIC_LIBRARY_PREFIX", staticLibraryPrefix);
        result += addCMakeDefinition("STATIC_LIBRARY_SUFFIX", staticLibrarySuffix);
        result += addCMakeDefinition("INJECT_MAVEN_DEPENDENCIES", injectMavenDependencies?"true":"false");
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
    private String cmakeDependenciesFile;
    
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
     * directories were bin debug dependencies are
     * default content is one location : ${project.build.directory}/dependency/${targetClassifier}/${buildConfig}
     * 
     * @parameter
     * @since 0.0.6
     */
    private List dependenciesRoots = new ArrayList();
    
    
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
            
            getLog().info( "Search for **/*.[so|dylib|dll|lib|a|] from " + afileSet.getDirectory() );
            afileSet.setIncludes( Arrays.asList( new String[]{"**/*.so", "**/*.dll", "**/*.dylib", "**/*.lib", "**/*.a"} ) );
            
            FileSetManager aFileSetManager = new FileSetManager();
            String[] found = aFileSetManager.getIncludedFiles( afileSet );
            
            for (int j = 0; null != found && j < found.length; j++)
            {
                getLog().info( "Found dependencies Lib : " + found[j] );
                getLog().info( "Found dependencies Lib full path : " + dependencyRoot + "/" + found[j] );
                ao_dependenciesLib.add(found[j]);
            }
        }
    }
    
    protected String baseNameAsStaticLibrary(String sName)
    {
        if (FilenameUtils.isExtension(sName, FilenameUtils.getExtension(staticLibrarySuffix)))
        {
            sName = FilenameUtils.removeExtension(sName)+"${STATIC_LIBRARY_SUFFIX}";
            if (! StringUtils.isEmpty(staticLibraryPrefix))
            {
                if (0 == sName.indexOf(staticLibraryPrefix))
                {
                    sName = sName.replaceFirst(Pattern.quote(staticLibraryPrefix), "");
                    sName = "${STATIC_LIBRARY_PREFIX}" + sName;
                }
                else
                {
                    sName = "";
                }
            }
            else
            {
                sName = "${STATIC_LIBRARY_PREFIX}" + sName;
            }
        }
        return sName;
    }
    
    protected String baseNameAsSharedModule(String sName)
    {
        if (FilenameUtils.isExtension(sName, FilenameUtils.getExtension(sharedModuleSuffix)))
        {
            sName = FilenameUtils.removeExtension(sName)+"${SHARED_MODULE_SUFFIX}";
            if (! StringUtils.isEmpty(sharedModulePrefix))
            {
                if (0 == sName.indexOf(sharedModulePrefix))
                {
                    sName = sName.replaceFirst(Pattern.quote(sharedModulePrefix), "");
                    sName = "${SHARED_MODULE_PREFIX}" + sName;
                }
                else
                {
                    sName = "";
                }
            }
            else
            {
                sName = "${SHARED_MODULE_PREFIX}" + sName;
            }
        }
        return sName;
    }
    
    protected String baseNameAsSharedLibrary(String sName)
    {
        if (FilenameUtils.isExtension(sName, FilenameUtils.getExtension(sharedLibrarySuffix)))
        {
            sName = FilenameUtils.removeExtension(sName)+"${SHARED_LIBRARY_SUFFIX}";
            if (! StringUtils.isEmpty(sharedLibraryPrefix))
            {
                if (0 == sName.indexOf(sharedLibraryPrefix))
                {
                    sName = sName.replaceFirst(Pattern.quote(sharedLibraryPrefix), "");
                    sName = "${SHARED_LIBRARY_PREFIX}" + sName;
                }
                else
                {
                    sName = "";
                }
            }
            else
            {
                sName = "${SHARED_LIBRARY_PREFIX}" + sName;
            }
        }
        return sName;
    }
    
    protected String generalizeDependencyFileName(String dependency)
    {
        String sName = FilenameUtils.getName(dependency);
        String fullPath = FilenameUtils.getFullPathNoEndSeparator(dependency);

        String sGeneralizedName = baseNameAsSharedLibrary(sName);
        if (StringUtils.isEmpty(sGeneralizedName))
        {
            sGeneralizedName = baseNameAsStaticLibrary(sName);
        }
        if (StringUtils.isEmpty(sGeneralizedName))
        {
            sGeneralizedName = baseNameAsSharedModule(sName);
        }
        return fullPath + "/" + (StringUtils.isEmpty(sGeneralizedName)? sName : sGeneralizedName);
    }
    
    protected boolean isDebugBuild()
    {
         return (buildConfig.equalsIgnoreCase("debug"));
    }
    
    protected void updateOrCreateCMakeDependenciesFile(List ai_dependenciesLib)
    {
        String fullCMakeDependenciesFile = cmakeDependenciesFile;
        File file = new File(cmakeDependenciesFile);
        if (!file.isAbsolute()) 
        {
            fullCMakeDependenciesFile = getProjectDir() + "/" + cmakeDependenciesFile;
        }
        file = new File(fullCMakeDependenciesFile);
      
        if (!file.exists())
        {  
            try 
            {
                file.createNewFile();
            }
            catch ( IOException e ) 
            {
                getLog().error( cmakeDependenciesFile + " script can't be created at " + file.getAbsolutePath());
                return;
            }
        }
        
        
        // check file content
        InputStream cmakeMavenDependenciesStream = null;
        String content = new String();
        try
        {  
            cmakeMavenDependenciesStream = new FileInputStream(file);
            content = IOUtils.toString(cmakeMavenDependenciesStream, "UTF8");
        }
        catch ( IOException e )
        {
            // shall not happen since file has been created
            getLog().error( cmakeDependenciesFile + " script can't be opened at " + file.getAbsolutePath());
        }
        finally
        {
            getLog().info( "close input stream at reading");
            IOUtils.closeQuietly(cmakeMavenDependenciesStream);
        }

        String beginPattern = (isDebugBuild() ?
            "# BEGIN MAVEN_DEBUG_DEPENDENCIES" : "# BEGIN MAVEN_OPTIMIZED_DEPENDENCIES");
        String endPattern = (isDebugBuild() ?
            "# END MAVEN_DEBUG_DEPENDENCIES" : "# END MAVEN_OPTIMIZED_DEPENDENCIES");
        
        
        // reset file content if needed
        if (content.isEmpty() || content.indexOf(beginPattern) == -1)
        {
            getLog().info( file.getAbsolutePath() + " content full update");
            try
            { 
                cmakeMavenDependenciesStream = getClass().getResourceAsStream( "/CMakeMavenDependencies.txt" );
                content = IOUtils.toString(cmakeMavenDependenciesStream, "UTF8");
            }
            catch ( IOException e )
            {
                getLog().error( cmakeDependenciesFile + " default content not found ");
            }
            finally
            {
                getLog().debug( "close input stream at full update");
                IOUtils.closeQuietly(cmakeMavenDependenciesStream);
            }
        }
        
        
        // update file content
        Iterator itDeps = ai_dependenciesLib.iterator();
        String indentation = "\n        ";
        String allDeps = new String(indentation);
        while( itDeps.hasNext() )
        {
            allDeps = allDeps + 
                "target_link_libraries(${target} " + 
                (isDebugBuild() ? "debug" : "optimized") +
                " ${DEPENDENCY_DIR}/${TARGET_CLASSIFIER}/"+
                (isDebugBuild() ? "debug" : "release") + "/" +
                generalizeDependencyFileName((String)itDeps.next()) + ")" + indentation;
        }
            
        getLog().debug( "cmake depfile was : " + content );
        allDeps = allDeps.replace("$", "\\$"); // Matcher replaceAll() is a bit rigid !
        getLog().info( "cmake injected dependency will be : " + allDeps );
        // regexp multi-line replace, see http://stackoverflow.com/questions/4154239/java-regex-replaceall-multiline
        Pattern p = Pattern.compile(beginPattern + ".*" + endPattern, Pattern.DOTALL);
        Matcher m = p.matcher(content);
        content = m.replaceAll(beginPattern + allDeps + endPattern);
                
        getLog().debug( "cmake depfile now is : " + content );
        OutputStream outStream = null;
        try
        { 
            outStream = new FileOutputStream( file );
            IOUtils.write(content, outStream, "UTF8");
        }
        catch ( IOException e )
        {
            getLog().error( cmakeDependenciesFile + " script can't be written at " + file.getAbsolutePath() + e.toString());
        }
        finally
        {
            getLog().debug( "close output stream at update");
            IOUtils.closeQuietly(outStream);
        }
    }
    
    protected void preExecute(Executor exec, CommandLine commandLine, Map enviro) throws MojoExecutionException
    {
        if (injectMavenDependencies)
        {
            if (dependenciesRoots.isEmpty())
            {
                dependenciesRoots.add(getProjectDependenciesTargetClassifierDirectory() + "/" + buildConfig);
                getLog().info( "use default Dependency Root: \"" + dependenciesRoots.get(0) + "\"" );  
            }
            
            ArrayList dependenciesLib = new ArrayList();
            findDependencies(dependenciesRoots, dependenciesLib);
            
            updateOrCreateCMakeDependenciesFile(dependenciesLib);
        }
    }
}
