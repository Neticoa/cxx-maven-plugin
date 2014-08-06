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

import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.apache.commons.lang3.StringUtils;

/**
 * Goal which cppcheck sources.
 *
 * @author Franck Bonin 
 * @goal cppcheck
 * @phase test
 * 
 */
public class CppCheckMojo extends AbstractLaunchMojo {

    protected List getArgsList() {
        return null;
    }
    
    /**
     * Directory where cppcheck should search for source files
     * 
     * @parameter
     * @since 0.0.4
     */
    private List sourceDirs = new ArrayList();
    
    /**
     * Directory where included file shall be found
     * 
     * @parameter
     * @since 0.0.4
     */
    private List includeDirs = new ArrayList();
    
    /**
     *  Excludes files/folder from analysis (comma separated list of filter)
     *
     *  @parameter expression="${cppcheck.excludes}" default-value=""
     *  @since 0.0.5
     */
    private String excludes;
    
   /**
     * The Report OutputFile Location.
     * 
     * @parameter expression="${cppcheck.reportsfilePath}" default-value="cppcheck-reports"
     * @since 0.0.4
     */
    private File reportsfileDir;
    
    /**
     * The Report OutputFile name identifier.
     * 
     * @parameter expression="${cppcheck.reportIdentifier}" default-value=""
     * @since 0.0.4
     */
    private String reportIdentifier;
    private String getReportFileName() {
        return "cppcheck-result-" + reportIdentifier + ".xml";
    }
    
    /**
     * Arguments for the cppncss program. Shall be -v --enable=style --force --xml 
     * 
     * @parameter expression="${cppcheck.args}" default-value="-v --enable=style --force --xml"
     */
    private String commandArgs;

    protected String getCommandArgs() {
        String params = commandArgs + " ";
        HashSet<String> excudedSet = new HashSet<String>();
        
        Iterator it = includeDirs.iterator();
        while(it.hasNext()) {
            FileSet afileSet = new FileSet();
            
            String dir = it.next().toString();
            params += "-I\"" + dir + "\" ";
            
            if (StringUtils.isNotEmpty(excludes)) {
                afileSet.setDirectory(new File(dir).getAbsolutePath());
                afileSet.setUseDefaultExcludes(false); // $FB pour éviter d'avoir TROP de fichier excludes (inutiles) dans la boucle for ci-après
                afileSet.setExcludes(Arrays.asList(excludes.split(",")));
                getLog().debug("cppcheck excludes are :" + Arrays.toString(afileSet.getExcludes().toArray()) );
                
                FileSetManager aFileSetManager = new FileSetManager();
                String[] found = aFileSetManager.getExcludedFiles(afileSet);
                excudedSet.addAll(new HashSet<String>(Arrays.asList(found)));
            }
        }
        it = sourceDirs.iterator();
        while(it.hasNext()) {
            FileSet afileSet = new FileSet();
            
            String dir = it.next().toString();
            params += "-I\"" + dir + "\" ";
            
            if (StringUtils.isNotEmpty(excludes)) {
                afileSet.setDirectory(new File(dir).getAbsolutePath());
                afileSet.setUseDefaultExcludes(false); // $FB pour éviter d'avoir TROP de fichiers exclude (inutile) dans la boucle for ci-après
                afileSet.setExcludes(Arrays.asList(excludes.split(",")));
                getLog().debug("cppcheck excludes are :" + Arrays.toString(afileSet.getExcludes().toArray()) );
                
                FileSetManager aFileSetManager = new FileSetManager();
                String[] found = aFileSetManager.getExcludedFiles(afileSet);
                excudedSet.addAll(new HashSet<String>(Arrays.asList(found)));
            }
        }
        for (Iterator<String> iter = excudedSet.iterator(); iter.hasNext(); ) {
            String s = iter.next();
            //cppcheck only check *.cpp, *.cxx, *.cc, *.c++, *.c, *.tpp, and *.txx files
            // so remove unneeded exclusions
            if (s.matches("(.+\\.cpp)|(.+\\.cxx)|(.+\\.cc)|(.+\\.c\\+\\+)|(.+\\.c)|(.+\\.tpp)|(.+\\.txx)")) {
                params += "-i\"" + s + "\" ";
            }
        }
        
        it = sourceDirs.iterator();
        while(it.hasNext()) {
            params += "\"" + it.next() + "\" ";
        }
        
        return params;
    }
    
    //override
    protected OutputStream getOutputStreamErr() {
        String OutputReportName = new String();
        if (reportsfileDir.isAbsolute()) {
            OutputReportName = reportsfileDir.getAbsolutePath() + "/" + getReportFileName();
        } else {
            OutputReportName = basedir.getAbsolutePath() + "/" + reportsfileDir.getPath() + "/" + getReportFileName();
        }
        
        getLog().info( "Cppcheck report location " + OutputReportName );
         
        OutputStream output = System.err;
        File file = new File(OutputReportName);
        try {
            new File(file.getParent()).mkdirs();
            file.createNewFile();
            output = new FileOutputStream(file);
        } catch (IOException e) {
            getLog().error( "Cppcheck report redirected to stderr since " + OutputReportName + " can't be opened" );
        }

        return output;
    }

    protected String getExecutable() {
        return "cppcheck";
    }

    /**
     * Environment variables to pass to cppcheck program.
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
     * @parameter expression="${cppcheck.workingdir}"
     * @since 0.0.4
     */
    private File workingDir;
    protected File getWorkingDir() {
        if (null == workingDir) {
            workingDir = new File(basedir.getPath());
        }
        return workingDir;
    }
    
    public boolean isSkip() {
        return false;
    }

}
