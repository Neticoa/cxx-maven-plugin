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

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.lang.StringUtils;

import org.apache.maven.plugin.MojoExecutionException;

/* Use FileSet and the FileManager provided in this project*/
import org.apache.maven.model.FileSet;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.maven.plugin.cxx.utils.ExecutorService;
import org.apache.maven.plugin.cxx.utils.FileSetManager;

/**
 * Goal which vera++ check sources.
 *
 * @author Franck Bonin 
 */
@Mojo( name = "veraxx", defaultPhase = LifecyclePhase.TEST )
public class VeraxxMojo extends AbstractLaunchMojo
{
    @Override
    protected List<String> getArgsList()
    {
        return null;
    }
    
    private int veraxxVersion = 0;
    
    @Override
    protected void preExecute( Executor exec, CommandLine commandLine, Properties enviro ) throws MojoExecutionException
    {
        OutputStream outStream = /*System.out;*/new ByteArrayOutputStream();
        OutputStream errStream = new ByteArrayOutputStream();

        CommandLine commandLineCheck = new CommandLine( getExecutable() );
        Executor execCheck = new DefaultExecutor();
        String[] args = parseCommandlineArgs( "--version" );
        commandLineCheck.addArguments( args, false );
        execCheck.setWorkingDirectory( exec.getWorkingDirectory() );
        
        getLog().info( "Executing command line: " + commandLineCheck );

        int res = 0;
        try
        {
            res = ExecutorService.executeCommandLine( execCheck, commandLineCheck, enviro,
                outStream/*getOutputStreamOut()*/, errStream/*getOutputStreamErr()*/, getInputStream() );
        }
        catch ( ExecuteException e )
        {
            getLog().info( "Exec Exception while detecting Vera++ version."
                + " Assume old Vera++ v1.1.x (and less) output parsing style" );
            getLog().info( "Vera++ err output is : " + errStream.toString() ) ;
            veraxxVersion = 0;
            /*throw new MojoExecutionException( "preExecute Command execution failed.", e );*/
            return;
        }
        catch ( IOException e )
        {
            getLog().info( "Vera++ detected version is : " + outStream.toString() ) ;
            getLog().info( "Vera++ err output is : " + errStream.toString() ) ;
            // due to jdk8 bug :: https://bugs.openjdk.java.net/browse/JDK-8054565
            // we use this dirty try/catch ...
            // because this quick command line call can close the output stream before jvm does
            getLog().info( "jvm " + System.getProperty( "java.version" )
                + " (8u11 - 9) workaround, ignoring a " + e.toString() + " during vera++ test command line." );
            //throw new MojoExecutionException( "preExecute Command execution failed.", e );
        }
        
        if ( isResultCodeAFailure( res ) )
        {
             getLog().info( "Vera++ returned a failure result code : " + res );
            //throw new MojoExecutionException( "preExecute Result of " + commandLineCheck 
            //    + " execution is: '" + res + "'." );
        }
        DefaultArtifactVersion newFormatMinVersion = new DefaultArtifactVersion( "1.2.0" );
        DefaultArtifactVersion currentVeraVersion = new DefaultArtifactVersion( outStream.toString() );
        
        getLog().debug( "Vera++ detected version is : " + outStream.toString() ) ;
        getLog().debug( "Vera++ version as ArtefactVersion is : " + currentVeraVersion.toString() );

        if ( currentVeraVersion.compareTo( newFormatMinVersion ) < 0 )
        {
            getLog().info( "Use old Vera++ v1.1.x (and less) output parsing style" );
            veraxxVersion = 0;
        }
        else
        {
            getLog().info( "Use Vera++ v1.2.0 (and more) output parsing style" );
            veraxxVersion = 1;
        }
    }

    /**
     * Arguments for vera++ program. Shall be -nodup -showrules 
     * 
     */
    @Parameter( property = "veraxx.args", defaultValue = "-nodup -showrules" )
    private String commandArgs;
    
    @Override
    protected String getCommandArgs()
    {
        String params = "- " + commandArgs + " ";
        return params;
    }
    
    /**
     * The Report OutputFile Location.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "veraxx.reportsfilePath", defaultValue = "vera++-reports" )
    private File reportsfileDir;

    /**
     * The Report OutputFile name identifier.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "veraxx.reportIdentifier", defaultValue = "" )
    private String reportIdentifier;
    
    private String getReportFileName()
    {
        return "vera++-result-" + reportIdentifier + ".xml";
    }
    
    @Override
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
        getLog().info( "Vera++ report location " + outputReportName );
         
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
            getLog().error( "Vera++ report redirected to stderr since " + outputReportName + " can't be opened" );
            return output;
        }

        final DataOutputStream out = new DataOutputStream( output );
        
        try
        {
            out.writeBytes( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
            out.writeBytes( "<checkstyle version=\"5.0\">\n" );
        }
        catch ( IOException e )
        {
            getLog().error( "Vera++ xml report write failure" );
        }
        
        OutputStream outErrFilter = new OutputStream()
        {
            StringBuffer sb = new StringBuffer();
            public void write( int b ) throws IOException
            {
                if ( ( b == '\n' ) || ( b == '\r' ) )
                {
                    transformCurrentLine();
                    // cleanup for next line
                    sb.delete( 0, sb.length() );
                }
                else 
                {
                    sb.append( (char) b );
                }
            }
            
            public void flush() throws IOException
            {
                transformCurrentLine();
                getLog().debug( "Vera++ xml flush() called" );
                if ( !StringUtils.isEmpty( lastfile ) )
                {
                    out.writeBytes( "\t</file>\n" );
                }
                out.writeBytes( "</checkstyle>\n" );
                out.flush();
            }
            
            String lastfile;
            private void transformCurrentLine()
            {
                if ( sb.length() > 0 )
                {
                    // parse current line
                    
                    // try to replace ' (RULENumber) ' with 'RULENumber:'
                    String p = "^(.+) \\((.+)\\) (.+)$";
                    Pattern pattern = Pattern.compile( p );
                    Matcher matcher = pattern.matcher( sb );
                    getLog().debug( "match " + sb + " on " + p );
                    
                    boolean bWinPath = false;
                    if ( sb.charAt( 1 ) == ':' )
                    {
                        bWinPath = true;
                        sb.setCharAt( 1, '_' );
                    }
                    
                    if ( matcher.matches() )
                    {
                        String sLine = matcher.group( 1 ) + matcher.group( 2 ) + ":" + matcher.group( 3 );
                        getLog().debug( "rebuild line = " + sLine );
                        
                        // extract informations
                        pattern = Pattern.compile( ":" );
                        String[] items = pattern.split( sLine );
                        
                        String file, line, rule, comment, severity;
                        file = items.length > 0 ? items[0] : "";
                        line = items.length > 1 ? items[1] : "";
                        rule = items.length > 2 ? items[2] : "";
                        comment = items.length > 3 ? items[3] : "";
                        severity = "warning";
                        
                        if ( bWinPath )
                        {
                            StringBuilder s = new StringBuilder( file );
                            s.setCharAt( 1, ':' );
                            file = s.toString();
                        }
                        
                        // output Xml errors
                        try
                        {
                            // handle <file/> tags
                            if ( !file.equals( lastfile ) )
                            {
                                if ( !StringUtils.isEmpty( lastfile ) )
                                {
                                    out.writeBytes( "\t</file>\n" );
                                }
                                out.writeBytes( "\t<file name=\"" + file + "\">\n" );
                                lastfile = file;
                            }
                            out.writeBytes( "\t\t<error line=\"" + line + "\" severity=\"" + severity 
                                + "\" message=\"" + comment + "\" source=\"" + rule + "\"/>\n" );
                        }
                        catch ( IOException e )
                        {
                            getLog().error( "Vera++ xml report write failure" );
                        }
                    }
                }
            }
        };
        return outErrFilter;
    }
    
    /**
     *  Excludes files/folder from analysis (comma separated list of filter)
     *
     *  @since 0.0.5
     */
    @Parameter( property = "veraxx.excludes", defaultValue = "" )
    private String excludes;
     
    /**
     * Directory where vera++ should search for source files
     * 
     * @since 0.0.4
     */
    @Parameter()
    private List sourceDirs = new ArrayList();
    
    @Override
    protected InputStream getInputStream()
    {
        StringBuilder sourceListString = new StringBuilder();
        Iterator it = sourceDirs.iterator();
        while ( it.hasNext() )
        {
            FileSet afileSet = new FileSet();
            String dir = it.next().toString();
            afileSet.setDirectory( new File( dir ).getAbsolutePath() );
            
            afileSet.setIncludes( Arrays.asList( new String[]{"**/*.cpp", "**/*.h", "**/*.cxx", "**/*.hxx"} ) );
            if ( StringUtils.isNotEmpty( excludes ) )
            {
                afileSet.setExcludes( Arrays.asList( excludes.split( "," ) ) );
            }
            getLog().debug( "vera++ excludes are :" + Arrays.toString( afileSet.getExcludes().toArray() ) );
            
            FileSetManager aFileSetManager = new FileSetManager();
            String[] found = aFileSetManager.getIncludedFiles( afileSet );
            
            for ( int i = 0; i < found.length; i++ )
            {
                sourceListString.append( dir + File.separator + found[i] + "\n" );
            }
        }
        InputStream is = System.in;
        try
        {
            getLog().debug( "vera++ sources are :" + sourceListString );
            is = new ByteArrayInputStream( sourceListString.toString().getBytes( "UTF-8" ) );
        }
        catch ( UnsupportedEncodingException e )
        {
            getLog().error( "vera++ source list from stdin failure" );
        }
        return is;
    }

    @Override
    protected String getExecutable()
    {
        return "vera++";
    }

    /**
     * Environment variables passed to vera++ program.
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

    @Override
    protected List<String> getSuccesCode()
    {
        return null;
    }

    /**
     * The current working directory. Optional. If not specified, basedir will be used.
     * 
     * @since 0.0.4
     */
    @Parameter( property = "veraxx.workingdir" )
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
    
    @Override
    protected boolean isSkip()
    {
        return skipTests || skip;
    }
}
