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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which vera++ check sources.
 *
 * @author Franck Bonin 
 * @goal veraxx
 * @phase test
 * 
 */
public class VeraxxMojo extends AbstractLaunchMojo {

    protected List getArgsList() {
        return null;
    }
    
    private int veraxx_version = 0;
    protected void preExecute(Executor exec, CommandLine commandLine, Map enviro) throws MojoExecutionException{
        OutputStream outStream = System.out;
        outStream = new ByteArrayOutputStream();

        CommandLine commandLineCheck = new CommandLine( getExecutable() );
        Executor execCheck = new DefaultExecutor();
        String[] args = parseCommandlineArgs("--version");
        commandLineCheck.addArguments(args, false );
        execCheck.setWorkingDirectory( exec.getWorkingDirectory() );
        try
        {
            getLog().info( "Executing command line: " + commandLineCheck );
            int res = executeCommandLine( execCheck, commandLineCheck, enviro, outStream/*getOutputStreamOut()*/, getOutputStreamErr(), getInputStream());

            /*if ( isResultCodeAFailure( res ) )
            {
                throw new MojoExecutionException( "preExecute Result of " + commandLineCheck + " execution is: '" + res + "'." );
            }
            else*/
            {
                DefaultArtifactVersion newFormatMinVersion = new DefaultArtifactVersion("1.2.0");
                DefaultArtifactVersion currentVeraVersion = new DefaultArtifactVersion(outStream.toString());
                
                getLog().debug( "Vera++ detected version is : " + outStream.toString());
                getLog().debug( "Vera++ version as ArtefactVersion is : " + currentVeraVersion.toString());

                if (currentVeraVersion.compareTo(newFormatMinVersion) < 0 ) {
                    getLog().info("Use old Vera++ output parsing");
                    veraxx_version = 0;
                }
                else
                {
                    getLog().info("Use new Vera++ output parsing");
                    veraxx_version = 1;
                }

            }
        }
        catch ( ExecuteException e )
        {
            /*throw new MojoExecutionException( "preExecute Command execution failed.", e );*/
            getLog().info( "Exec Exception while detecting Vera++ version. Assum old version");
            veraxx_version = 0;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "preExecute Command execution failed.", e );
        }
 
    }

    /**
     * Arguments for vera++ program. Shall be -nodup -showrules 
     * 
     * @parameter expression="${veraxx.args}" default-value="-nodup -showrules"
     */
    private String commandArgs;
    protected String getCommandArgs() {
        String params = "- " + commandArgs + " ";
        return params;
    }
    
    /**
     * The Report OutputFile Location.
     * 
     * @parameter expression="${veraxx.reportsfilePath}" default-value="vera++-reports"
     * @since 0.0.4
     */
    private File reportsfileDir;


    /**
     * The Report OutputFile name identifier.
     * 
     * @parameter expression="${veraxx.reportIdentifier}" default-value=""
     * @since 0.0.4
     */
    private String reportIdentifier;
    private String getReportFileName() {
        return "vera++-result-" + reportIdentifier + ".xml";
    }
    
    protected OutputStream getOutputStreamErr() {
        
        String OutputReportName = new String();
        if (reportsfileDir.isAbsolute()) {
            OutputReportName = reportsfileDir.getAbsolutePath() + "/" + getReportFileName();
        } else {
            OutputReportName = basedir.getAbsolutePath() + "/" + reportsfileDir.getPath() + "/" + getReportFileName();
        }
        getLog().info("Vera++ report location " + OutputReportName );
         
        OutputStream output = System.err;
        File file = new File(OutputReportName);
        try {
            new File(file.getParent()).mkdirs();
            file.createNewFile();
            output = new FileOutputStream(file);
        } catch (IOException e) {
            getLog().error( "Vera++ report redirected to stderr since " + OutputReportName + " can't be opened" );
            return output;
        }

        final DataOutputStream out = new DataOutputStream(output);
        
        try {
            out.writeBytes("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.writeBytes("<checkstyle version=\"5.0\">\n");
        } catch (IOException e) {
            getLog().error( "Vera++ xml report write failure");
        }
        
        OutputStream outErrFilter = new OutputStream() {
            StringBuffer sb = new StringBuffer();
            public void write(int b) throws IOException {
                if ((b == '\n') || (b == '\r')) {
                    transformCurrentLine();
                    // cleanup for next line
                    sb.delete(0, sb.length());
                } else {
                    sb.append((char) b);
                }
            }
            
            public void flush() throws IOException {
                transformCurrentLine();
                getLog().debug( "Vera++ xml flush() called");
                if (!StringUtils.isEmpty(lastfile))    {
                    out.writeBytes("\t</file>\n");
                }
                out.writeBytes("</checkstyle>\n");
                out.flush();
            }
            
            String lastfile;
            private void transformCurrentLine()
            {
                if (sb.length() > 0) {
                    // parse current line
                    
                    // try to replace ' (RULENumber) ' with 'RULENumber:'
                    String p = "^(.+) \\((.+)\\) (.+)$";
                    Pattern pattern = Pattern.compile(p);
                    Matcher matcher = pattern.matcher(sb);
                    getLog().debug( "match "+ sb + " on " + p);
                    
                    boolean bWinPath = false;
                    if (sb.charAt(1) == ':') {
                        bWinPath = true;
                        sb.setCharAt(1, '_');
                    }
                    
                    if (matcher.matches())
                    {
                        String sLine = matcher.group(1) + matcher.group(2) + ":" + matcher.group(3);
                        getLog().debug( "rebuild line = " + sLine);
                        
                        // extract informations
                        pattern = Pattern.compile(":");
                        String[] items = pattern.split(sLine);
                        
                        String file, line, rule, comment, severity;
                        file = items.length > 0 ? items[0] : "";
                        line = items.length > 1 ? items[1] : "";
                        rule = items.length > 2 ? items[2] : "";
                        comment = items.length > 3 ? items[3] : "";
                        severity = "warning";
                        
                        if (bWinPath) {
                            StringBuilder s = new StringBuilder(file);
                            s.setCharAt(1, ':');
                            file = s.toString();
                        }
                        
                        // output Xml errors
                        try {
                            // handle <file/> tags
                            if (!file.equals(lastfile))    {
                                if (!StringUtils.isEmpty(lastfile))    {
                                    out.writeBytes("\t</file>\n");
                                }
                                out.writeBytes("\t<file name=\"" + file + "\">\n");
                                lastfile = file;
                            }
                            out.writeBytes("\t\t<error line=\"" + line + "\" severity=\"" + severity + "\" message=\"" + comment + "\" source=\"" + rule + "\"/>\n");
                        } catch (IOException e) {
                            getLog().error( "Vera++ xml report write failure");
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
     *  @parameter expression="${veraxx.excludes}" default-value=""
     *  @since 0.0.5
     */
    private String excludes;
     
    /**
     * Directory where vera++ should search for source files
     * 
     * @parameter
     * @since 0.0.4
     */
    private List sourceDirs = new ArrayList();
    
    protected InputStream getInputStream() {
    StringBuilder sourceListString = new StringBuilder();
        Iterator it = sourceDirs.iterator();
    while(it.hasNext()) {
            FileSet afileSet = new FileSet();
            String dir = it.next().toString();
            afileSet.setDirectory(new File(dir).getAbsolutePath());
            
        afileSet.setIncludes(Arrays.asList(new String[]{"**/*.cpp", "**/*.h", "**/*.cxx", "**/*.hxx"}));
        if (StringUtils.isNotEmpty(excludes)) {
            afileSet.setExcludes(Arrays.asList(excludes.split(",")));
        }
        getLog().debug("vera++ excludes are :" + Arrays.toString(afileSet.getExcludes().toArray()) );
            
        FileSetManager aFileSetManager = new FileSetManager();
        String[] found = aFileSetManager.getIncludedFiles(afileSet);
            
        for (int i = 0; i < found.length; i++) {
            sourceListString.append(dir + "/" + found[i] + "\n");
        }
    }
        InputStream is = System.in;
    try {
        getLog().debug("vera++ sources are :" + sourceListString );
        is = new ByteArrayInputStream(sourceListString.toString().getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
        getLog().error("vera++ source list from stdin failure" );
    }
        return is;
    }

    protected String getExecutable() {
        return "vera++";
    }

    /**
     * Environment variables passed to vera++ program.
     * 
     * @parameter
     * @since 0.0.4
     */
    private Map environmentVariables = new HashMap();
    protected Map getMoreEnvironmentVariables() {
        return environmentVariables;
    }

    protected List getSuccesCode() {
        return null;
    }

    /**
     * The current working directory. Optional. If not specified, basedir will be used.
     * 
     * @parameter expression="${veraxx.workingdir}"
     * @since 0.0.4
     */
    private File workingDir;
    protected File getWorkingDir() {
        if (null == workingDir) {
            workingDir = new File(basedir.getPath());
        }
        return workingDir;
    }

    protected boolean isSkip() {
        return false;
    }

}
