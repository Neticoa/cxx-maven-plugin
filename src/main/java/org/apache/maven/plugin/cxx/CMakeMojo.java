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
import java.util.Set;
import org.codehaus.plexus.util.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.Executor;

import org.apache.maven.plugin.MojoExecutionException;

/* Use enhanced FileSet and FileManager (not the one provided in this project)*/
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.apache.maven.artifact.Artifact;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which cmakes workspace.
 *
 * @author Franck Bonin
 * 
 */
@Mojo( name = "cmake", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class CMakeMojo extends AbstractLaunchMojo
{
    @Override
    protected List<String> getArgsList()
    {
        return null;
    }

    /**
     * Directory location of main CMakeList.txt, argument for cmake command Defaut
     * set to ${basedir}
     * 
     * @since 0.0.4
     */
    @Parameter( property = "cmake.projectdir" )
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
     * @since 0.0.4
     */
    @Parameter( property = "cmake.generator", required = true )
    private String generator;

    /**
     * Arguments for the executed program
     * 
     * @since 0.0.4
     */
    @Parameter( property = "cmake.args" )
    private String commandArgs;

    protected String addCMakeDefinition( String sCMakeName, String sMavenValue )
    {
        String sResult = null;
        if ( !StringUtils.isEmpty( sCMakeName ) && !StringUtils.isEmpty( sMavenValue ) )
        {
            sResult = " -D" + sCMakeName + "=\"" + sMavenValue + "\"";
        }
        else
        {
            sResult = new String();
        }
        return sResult;
    }

    @Override
    protected String getCommandArgs()
    {
        String result = new String();
        if ( !StringUtils.isEmpty( commandArgs ) )
        {
            result = commandArgs + " ";
        }
        result += "\"" + getProjectDir() + "\" -G " + generator;
        result += addCMakeDefinition( "CMAKE_BUILD_TYPE", buildConfig );
        result += addCMakeDefinition( "TARGET_ID", project.getArtifactId() );
        result += addCMakeDefinition( "TARGET_NAME", project.getName() );
        result += addCMakeDefinition( "DEPENDENCY_DIR", getProjectDependenciesDirectory() );
        result += addCMakeDefinition( "TARGET_VERSION", project.getVersion() );
        result += addCMakeDefinition( "TARGET_CLASSIFIER", targetClassifier );
        result += addCMakeDefinition( "TARGET_PLATFORM", targetPlatform );
        result += addCMakeDefinition( "TARGET_ARCHITECTURE", targetArchitecture );
        result += addCMakeDefinition( "EXECUTABLE_SUFFIX", executableSuffix );
        result += addCMakeDefinition( "SHARED_LIBRARY_PREFIX", sharedLibraryPrefix );
        result += addCMakeDefinition( "SHARED_LIBRARY_SUFFIX", sharedLibrarySuffix );
        result += addCMakeDefinition( "SHARED_MODULE_PREFIX", sharedModulePrefix );
        result += addCMakeDefinition( "SHARED_MODULE_SUFFIX", sharedModuleSuffix );
        result += addCMakeDefinition( "STATIC_LIBRARY_PREFIX", staticLibraryPrefix );
        result += addCMakeDefinition( "STATIC_LIBRARY_SUFFIX", staticLibrarySuffix );
        result += addCMakeDefinition( "INJECT_MAVEN_DEPENDENCIES", injectMavenDependencies ? "true" : "false" );
        return result;
    }

    @Override
    protected String getExecutable()
    {
        return "cmake";
    }

    /**
     * Environment variables to pass to the cmake program.
     * 
     * @since 0.0.4
     */
    @Parameter( )
    private Map<?, ?> environmentVariables = new HashMap<Object, Object>();

    @Override
    protected Map<?, ?> getMoreEnvironmentVariables()
    {
        return environmentVariables;
    }

    @Override
    protected List<String> getSuccesCode()
    {
        return null;
    }

    /**
     * Out of source directory Defaut set to ${basedir}
     * 
     * @since 0.0.4
     */
    @Parameter( property = "cmake.outsourcedir" )
    private File outsourceDir;

    @Override
    protected File getWorkingDir()
    {
        if ( null == outsourceDir )
        {
            outsourceDir = new File( basedir.getPath() );
        }
        return outsourceDir;
    }

    @Override
    public boolean isSkip()
    {
        return false;
    }

    protected String getProjectDependenciesDirectory()
    {
        //@formatter:off
        // project.getProperties().setProperty("toto", "toto");
        // String projectBuildDirectory = project.getProperties().getProperty("project.build.directory");
        // String projectBuildDirectory = System.getProperty("project.build.directory");
        // String projectBuildDirectory = project.getModel().getBuild().getDirectory();
        //@formatter:on
        String projectBuildDirectory = project.getBuild().getDirectory();
        if ( StringUtils.isEmpty( projectBuildDirectory ) )
        {
            projectBuildDirectory = basedir.getAbsolutePath() + "/target";
        }
        return projectBuildDirectory + "/dependency";
    }

    /**
     * generate a cmake maven dependency file according
     * 
     * @since 0.0.6
     */
    @Parameter( property = "cmake.injectMavenDependencies", defaultValue = "true" )
    private boolean injectMavenDependencies;

    /**
     * cmake maven dependency file name. If not full qualified name file location is
     * ${basedir}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "cmake.mavenDependenciesFile", defaultValue = "CMakeMavenDependencies.txt" )
    private String cmakeMavenDependenciesFile;

    /**
     * cmake dependency file name. If not full qualified name file location is
     * ${basedir}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "cmake.dependenciesFile", defaultValue = "CMakeDependencies.txt" )
    private String cmakeDependenciesFile;

    /**
     * build configuration {debug, release, debcov, relcov, relinfo}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "buildConfig", defaultValue = "" )
    private String buildConfig;

    /**
     * maven artifact sub-classifier {win32, win64, linux-x64_64, etc.}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "targetClassifier", defaultValue = "" )
    private String targetClassifier;

    /**
     * build platform = "native API" {win32, linux, mac}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "targetPlatform", defaultValue = "" )
    private String targetPlatform;

    /**
     * build architecture {i386, x86_64, etc.}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "targetArchitecture", defaultValue = "" )
    private String targetArchitecture;

    /**
     * executables suffix of current platform {".exe", etc.}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "executableSuffix", defaultValue = "" )
    private String executableSuffix;

    /**
     * libraries prefixe of current platform {"lib", etc.}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "sharedLibraryPrefix", defaultValue = "" )
    private String sharedLibraryPrefix;

    /**
     * libraries suffixe of current platform {".so", ".dylib", etc.}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "sharedLibrarySuffix", defaultValue = "" )
    private String sharedLibrarySuffix;

    /**
     * module prefixe of current platform {"lib", etc.}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "sharedModulePrefix", defaultValue = "" )
    private String sharedModulePrefix;

    /**
     * module suffixe of current platform {".so", ".dylib", etc.}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "sharedModuleSuffix", defaultValue = "" )
    private String sharedModuleSuffix;

    /**
     * static libraries prefixe of current platform {"lib", etc.}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "staticLibraryPrefix", defaultValue = "" )
    private String staticLibraryPrefix;

    /**
     * static libraries suffixe of current platform {".a", ".lib", etc.}
     * 
     * @since 0.0.6
     */
    @Parameter( property = "staticLibrarySuffix", defaultValue = "" )
    private String staticLibrarySuffix;

    /**
     * directories were additional bin dependencies are default dependencies
     * location is :
     * ${project.build.directory}/dependency/${targetClassifier}/${buildConfig}
     * "${targetClassifier}/${buildConfig}" will be automaticaly added to provided
     * path to search for additionnal dependencies
     * 
     * @since 0.0.6
     */
    @Parameter( )
    private String additionalDependenciesRoots[] = null;

    /**
     * Directory where additional include dependencies are
     * 
     * @since 0.0.6
     */
    @Parameter( )
    protected String additionalIncludeRoots[] = null;

    protected void findDependencies( Map<String, String> aiDependenciesRoots, List<String> aoDependenciesLib )
    {
        for ( Map.Entry<String, String> entry : aiDependenciesRoots.entrySet() )
        {
            String dependencyRoot = new String( entry.getKey() );

            FileSet afileSet = new FileSet();
            afileSet.setDirectory( dependencyRoot );

            getLog().info( "Search for **/*.[so|dylib|dll|lib|a|] from " + afileSet.getDirectory() );
            afileSet.setIncludes(
                Arrays.asList( new String[] { "**/*.so", "**/*.dll", "**/*.dylib", "**/*.lib", "**/*.a" } ) );

            FileSetManager aFileSetManager = new FileSetManager();
            String[] found = aFileSetManager.getIncludedFiles( afileSet );

            for ( int j = 0; null != found && j < found.length; j++ )
            {
                // $FB always use unix path separator with cmake even under windows !
                getLog().info( "Found dependencies Lib : " + found[j] );
                getLog().info( "Found dependencies Lib full path : " + dependencyRoot + "/" + found[j] );
                getLog().info( "Found dependencies Lib generalized path : " + entry.getValue() + "/" + found[j] );
                aoDependenciesLib.add( entry.getValue() + "/" + found[j] );
            }
        }
    }

    protected String baseNameAsStaticLibrary( String sName, boolean bMavenDependency )
    {
        if ( FilenameUtils.isExtension( sName, FilenameUtils.getExtension( staticLibrarySuffix ) ) )
        {
            sName = FilenameUtils.removeExtension( sName ) + ( bMavenDependency ? "${STATIC_LIBRARY_SUFFIX}" : "" );
            if ( !StringUtils.isEmpty( staticLibraryPrefix ) )
            {
                if ( 0 == sName.indexOf( staticLibraryPrefix ) )
                {
                    sName = sName.replaceFirst( Pattern.quote( staticLibraryPrefix ), "" );
                    sName = ( bMavenDependency ? "${STATIC_LIBRARY_PREFIX}" : "" ) + sName;
                }
                else
                {
                    sName = "";
                }
            }
            else
            {
                sName = ( bMavenDependency ? "${STATIC_LIBRARY_PREFIX}" : "" ) + sName;
            }
        }
        return sName;
    }

    protected String baseNameAsSharedModule( String sName, boolean bMavenDependency )
    {
        if ( FilenameUtils.isExtension( sName, FilenameUtils.getExtension( sharedModuleSuffix ) ) )
        {
            sName = FilenameUtils.removeExtension( sName ) + ( bMavenDependency ? "${SHARED_MODULE_SUFFIX}" : "" );
            if ( !StringUtils.isEmpty( sharedModulePrefix ) )
            {
                if ( 0 == sName.indexOf( sharedModulePrefix ) )
                {
                    sName = sName.replaceFirst( Pattern.quote( sharedModulePrefix ), "" );
                    sName = ( bMavenDependency ? "${SHARED_MODULE_PREFIX}" : "" ) + sName;
                }
                else
                {
                    sName = "";
                }
            }
            else
            {
                sName = ( bMavenDependency ? "${SHARED_MODULE_PREFIX}" : "" ) + sName;
            }
        }
        return sName;
    }

    protected String baseNameAsSharedLibrary( String sName, boolean bMavenDependency )
    {
        if ( FilenameUtils.isExtension( sName, FilenameUtils.getExtension( sharedLibrarySuffix ) ) )
        {
            sName = FilenameUtils.removeExtension( sName ) + ( bMavenDependency ? "${SHARED_LIBRARY_SUFFIX}" : "" );
            if ( !StringUtils.isEmpty( sharedLibraryPrefix ) )
            {
                if ( 0 == sName.indexOf( sharedLibraryPrefix ) )
                {
                    sName = sName.replaceFirst( Pattern.quote( sharedLibraryPrefix ), "" );
                    sName = ( bMavenDependency ? "${SHARED_LIBRARY_PREFIX}" : "" ) + sName;
                }
                else
                {
                    sName = "";
                }
            }
            else
            {
                sName = ( bMavenDependency ? "${SHARED_LIBRARY_PREFIX}" : "" ) + sName;
            }
        }
        return sName;
    }

    protected String generalizeDependencyFileName( String dependency, boolean bMavenDependency )
    {
        String sName = FilenameUtils.getName( dependency );
        String fullPath = FilenameUtils.getFullPathNoEndSeparator( dependency );

        String sGeneralizedName = baseNameAsSharedLibrary( sName, bMavenDependency );
        if ( StringUtils.isEmpty( sGeneralizedName ) )
        {
            sGeneralizedName = baseNameAsStaticLibrary( sName, bMavenDependency );
        }
        if ( StringUtils.isEmpty( sGeneralizedName ) )
        {
            sGeneralizedName = baseNameAsSharedModule( sName, bMavenDependency );
        }
        // $FB always use unix path separator with cmake even under windows !
        return ( bMavenDependency ? fullPath + "/" : "" )
            + ( StringUtils.isEmpty( sGeneralizedName ) ? sName : sGeneralizedName );
    }

    protected boolean isDebugBuild()
    {
        return ( !StringUtils.isEmpty( buildConfig ) && 0 == buildConfig.indexOf( "deb" ) );
    }

    protected void updateOrCreateCMakeDependenciesFile( List<String> aiDependenciesLib, boolean bMavenDependencies )
    {
        String dependencieFile = ( bMavenDependencies ? cmakeMavenDependenciesFile : cmakeDependenciesFile );
        String fullDependenciesFile = dependencieFile;
        File file = new File( dependencieFile );
        if ( !file.isAbsolute() )
        {
            // $FB always use unix path separator with cmake even under windows !
            fullDependenciesFile = getProjectDir() + "/" + dependencieFile;
        }
        file = new File( fullDependenciesFile );

        if ( !file.exists() )
        {
            try
            {
                file.createNewFile();
            }
            catch ( IOException e )
            {
                getLog().error( dependencieFile + " script can't be created at " + file.getAbsolutePath() );
                return;
            }
        }

        // check file content
        InputStream dependenciesStream = null;
        String content = new String();
        try
        {
            dependenciesStream = new FileInputStream( file );
            content = IOUtils.toString( dependenciesStream, "UTF8" );
        }
        catch ( IOException e )
        {
            // shall not happen since file has been created
            getLog().error( dependencieFile + " script can't be opened at " + file.getAbsolutePath() );
        }
        finally
        {
            getLog().debug( "close input stream at reading" );
            IOUtils.closeQuietly( dependenciesStream );
        }

        String beginDepsPattern = ( bMavenDependencies
            ? ( isDebugBuild() ? "# BEGIN MAVEN_DEBUG_DEPENDENCIES" : "# BEGIN MAVEN_OPTIMIZED_DEPENDENCIES" )
            : "# BEGIN CMAKE_DEPENDENCIES" );
        String endDepsPattern = ( bMavenDependencies
            ? ( isDebugBuild() ? "# END MAVEN_DEBUG_DEPENDENCIES" : "# END MAVEN_OPTIMIZED_DEPENDENCIES" )
            : "# END CMAKE_DEPENDENCIES" );

        String beginIncPattern = "# BEGIN MAVEN_INCLUDE_ROOTS";
        String endIncPattern = "# END MAVEN_INCLUDE_ROOTS";

        // reset file content if needed
        if ( StringUtils.isEmpty( content ) || content.indexOf( beginDepsPattern ) == -1 )
        {
            getLog().info( file.getAbsolutePath() + " content full update" );
            try
            {
                dependenciesStream = getClass()
                    .getResourceAsStream( ( bMavenDependencies ? "/cmake-cpp-project/CMakeMavenDependencies.txt"
                        : "/cmake-cpp-project/CMakeDependencies.txt" ) );
                content = IOUtils.toString( dependenciesStream, "UTF8" );
            }
            catch ( IOException e )
            {
                getLog().error( dependencieFile + " default content not found " );
            }
            finally
            {
                getLog().debug( "close input stream at full update" );
                IOUtils.closeQuietly( dependenciesStream );
            }
        }

        // update file content
        String simpleIndentation = "\n    ";
        String doubleIndentation = "\n        ";
        Iterator<String> itDeps = aiDependenciesLib.iterator();
        StringBuilder allDepsBuilder = new StringBuilder(
            ( bMavenDependencies ? doubleIndentation : simpleIndentation ) );
        while ( itDeps.hasNext() )
        {
            String dep = (String) itDeps.next();
            if ( bMavenDependencies )
            {
                String externalDep = generalizeDependencyFileName( dep, true );
                allDepsBuilder.append( "target_link_libraries(${target} " + ( isDebugBuild() ? "debug " : "optimized " )
                    + externalDep + ")" + doubleIndentation );
            }
            else
            {
                String cmakeDep = generalizeDependencyFileName( dep, false );
                allDepsBuilder.append( "# If a \"" + cmakeDep + "\" target has been define, this means we are building "
                    + "an amalgamed cmake project" + simpleIndentation + "# but maven dependencies can be used too"
                    + simpleIndentation + "if(TARGET " + cmakeDep + ")" + doubleIndentation + "message(\"Adding direct "
                    + cmakeDep + " cmake dependencies to target '${target}'\")" + doubleIndentation
                    + "target_link_libraries(${target} " + cmakeDep + ")" + simpleIndentation + "endif()"
                    + simpleIndentation );
            }
        }

        // adding additionalIncludeRoots in cmake maven dependencies file
        StringBuilder addIncsBuilder = new StringBuilder( doubleIndentation );
        if ( bMavenDependencies && null != additionalIncludeRoots )
        {
            addIncsBuilder.append( "include_directories( " + doubleIndentation );
            for ( String includeRoot : additionalIncludeRoots )
            {
                addIncsBuilder.append( "\"" + includeRoot + "\"" + doubleIndentation );
            }
            addIncsBuilder.append( ")" + doubleIndentation );
            for ( String includeRoot : additionalIncludeRoots )
            {
                addIncsBuilder
                    .append( "message(\"Adding '" + includeRoot + "' additional include root.\")" + doubleIndentation );
            }
        }

        getLog().debug( dependencieFile + " depfile was : " + content );
        String allDeps = Matcher.quoteReplacement( allDepsBuilder.toString() );
        // .replace( "$", "\\$" ); // Matcher replaceAll() is a bit rigid !
        getLog().debug( dependencieFile + " injected dependency will be : " + allDeps );
        // regexp multi-line replace, see
        // http://stackoverflow.com/questions/4154239/java-regex-replaceall-multiline
        Pattern p1 = Pattern.compile( beginDepsPattern + ".*" + endDepsPattern, Pattern.DOTALL );
        Matcher m1 = p1.matcher( content );
        content = m1.replaceAll( beginDepsPattern + allDeps + endDepsPattern );

        if ( bMavenDependencies && null != additionalIncludeRoots )
        {
            String addIncs = Matcher.quoteReplacement( addIncsBuilder.toString() );
            // .replace( "$", "\\$" ); // Matcher replaceAll() is a bit rigid !
            getLog().debug( dependencieFile + " injected includes Roots will be : " + addIncs );
            Pattern p2 = Pattern.compile( beginIncPattern + ".*" + endIncPattern, Pattern.DOTALL );
            Matcher m2 = p2.matcher( content );
            content = m2.replaceAll( beginIncPattern + addIncs + endIncPattern );
        }

        getLog().debug( dependencieFile + " depfile now is : " + content );
        OutputStream outStream = null;
        try
        {
            outStream = new FileOutputStream( file );
            IOUtils.write( content, outStream, "UTF8" );
        }
        catch ( IOException e )
        {
            getLog().error( dependencieFile + " script can't be written at " + file.getAbsolutePath() + e.toString() );
        }
        finally
        {
            getLog().debug( "close output stream at update" );
            IOUtils.closeQuietly( outStream );
        }
    }

    protected String extractBuildConfig( String classifier )
    {
        // bin-${targetClassifier}-${buildConfig}
        String[] parts = classifier.split( "-" );
        return ( parts.length >= 3 ) ? parts[parts.length - 1] : null;
    }

    protected String extractSubClassifier( String classifier )
    {
        // bin-${targetClassifier}-${buildConfig}
        String[] parts = classifier.split( "-" );
        if ( parts.length >= 3 )
        {
            // parts[1] .. parts[length-2]
            StringBuilder builder = new StringBuilder();
            builder.append( parts[1] );
            for ( int i = 2; i <= parts.length - 2; i++ )
            {
                builder.append( "-" );
                builder.append( parts[i] );
            }
            return builder.toString();
        }
        return null;
    }

    @Override
    protected void preExecute( Executor exec, CommandLine commandLine, Map<String, String> enviro )
        throws MojoExecutionException
    {
        HashMap<String, String> dependenciesRoots = new HashMap<String, String>();

        // Iterator<String> itAdditionnalDeps = additionalDependenciesRoots.iterator();
        // while ( itAdditionnalDeps.hasNext() )
        if ( null != additionalDependenciesRoots )
        {
            for ( String dependencyRoot : additionalDependenciesRoots )
            {
                // $FB always use unix path separator with cmake even under windows !
                String cur = dependencyRoot + "/" + targetClassifier + "/" + buildConfig;
                dependenciesRoots.put( cur, cur );
                getLog().info( "add additional Dependency Root: \"" + cur + "\"" );
            }
        }

        // enhanced auto-detection of dependency root dir with artifacts classifier :
        // maven 2.0.11 api doesn't provide generics containers
        @SuppressWarnings( "unchecked" )
        Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();
        Iterator<Artifact> itDeps = dependencyArtifacts.iterator();
        while ( itDeps.hasNext() )
        {
            Artifact cur = itDeps.next();
            String artifactId = cur.getArtifactId();
            String classifer = cur.getClassifier();
            if ( !StringUtils.isEmpty( classifer ) && 0 == classifer.indexOf( "bin" ) )
            {
                String artifactBuildConfig = extractBuildConfig( classifer );
                String artifactBuildConfigGeneralized = artifactBuildConfig;
                if ( StringUtils.isEmpty( artifactBuildConfig ) || artifactBuildConfig.equals( buildConfig ) )
                {
                    artifactBuildConfig = buildConfig;
                    artifactBuildConfigGeneralized = buildConfig; // "${CMAKE_BUILD_TYPE}";
                }

                String artifactSubClassifier = extractSubClassifier( classifer );
                String artifactSubClassifierGeneralized = artifactSubClassifier;
                if ( StringUtils.isEmpty( artifactSubClassifier ) || artifactSubClassifier.equals( targetClassifier ) )
                {
                    artifactSubClassifier = targetClassifier;
                    artifactSubClassifierGeneralized = "${TARGET_CLASSIFIER}";
                }
                // $FB always use unix path separator with cmake even under windows !
                String newDepRoot = getProjectDependenciesDirectory() + "/" + artifactSubClassifier + "/"
                    + artifactBuildConfig + "/" + artifactId;

                String newDepRootGeneralized = "${DEPENDENCY_DIR}" + "/" + artifactSubClassifierGeneralized + "/"
                    + artifactBuildConfigGeneralized + "/" + artifactId;

                if ( !dependenciesRoots.containsKey( newDepRoot ) )
                {
                    dependenciesRoots.put( newDepRoot, newDepRootGeneralized );
                    getLog()
                        .info( "add Dependency Root: \"" + newDepRoot + "\" <=> \"" + newDepRootGeneralized + "\"" );
                }
            }
        }

        ArrayList<String> dependenciesLib = new ArrayList<String>();
        findDependencies( dependenciesRoots, dependenciesLib );

        updateOrCreateCMakeDependenciesFile( dependenciesLib, true ); // maven dependencies
        updateOrCreateCMakeDependenciesFile( dependenciesLib, false ); // cmake project dependencies
    }
}
