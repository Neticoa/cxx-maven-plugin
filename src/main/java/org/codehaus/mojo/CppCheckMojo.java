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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
     * directory were cppcheck should search for source files
     * 
     * @parameter
     * @since 0.0.4
     */
    private List sourceDirs = new ArrayList();
    
    /**
     * directory were included file shall be found
     * 
     * @parameter
     * @since 0.0.4
     */
    private List includeDirs = new ArrayList();
    
   /**
     * The Report OutputFile Location.
     * 
     * @parameter expression="${cppcheck.reportsfilePath}" default-value="cppcheck-reports"
     * @since 0.0.4
     */
    private File reportsfileDir;
    
	/**
	 * The Report OutputFile Location.
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
		
		Iterator it = includeDirs.iterator();
		while(it.hasNext()) {
			params += "-I\"" + it.next() + "\" ";
		}
		it = sourceDirs.iterator();
		while(it.hasNext()) {
			params += "-I\"" + it.next() + "\" ";
		}
		
		it = sourceDirs.iterator();
		while(it.hasNext()) {
			params += "\"" + it.next() + "\" ";
		}
		
		return params;
	}
	
	//override
    protected OutputStream getOutputStreamErr(){
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
     * Environment variables to pass to the executed program.
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
