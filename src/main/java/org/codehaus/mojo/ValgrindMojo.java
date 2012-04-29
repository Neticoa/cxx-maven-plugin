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
import java.util.List;
import java.util.Map;

/**
 * Goal which valgrind executables.
 *
 * @author Franck Bonin 
 * @goal valgrind
 * @phase test
 * 
 */
public class ValgrindMojo extends AbstractLaunchMojo {

	protected List getArgsList() {
		return null;
	}

	
	/**
     * The Report OutputFile Location.
     * 
     * @parameter expression="${valgrind.reportsfilePath}" default-value="valgrind-reports"
     * @since 0.0.4
     */
    private File reportsfileDir;


	/**
	 * The Report OutputFile Location.
	 * 
	 * @parameter expression="${valgrind.reportIdentifier}" default-value=""
	 * @since 0.0.4
	 */
	private String reportIdentifier;
	private String getReportFileName() {
		return "valgrind-result-" + reportIdentifier + ".xml";
	}
    /**
     * Arguments for the valgrind program. Shall be 
     * 
     * @parameter expression="${valgrind.args}" default-value="--leak-check=yes --demangle=yes --xml=yes"
     */
	private String commandArgs;
	
	protected String getCommandArgs() {
		String params = commandArgs + " ";
    	String OutputReportName = new String();
		if (reportsfileDir.isAbsolute()) {
			OutputReportName = reportsfileDir.getAbsolutePath() + "/" + getReportFileName();
		} else {
			OutputReportName = basedir.getAbsolutePath() + "/" + reportsfileDir.getPath() + "/" + getReportFileName();
		}
	    File file = new File(OutputReportName);
	    new File(file.getParent()).mkdirs();
		
		params += "--xml-file=\"" + OutputReportName + "\" ";
		
		params += "\"" + instrumentedExecutablePath + "\" " + instrumentedExecutableArgs;
				
		return params;
	}
	
    /**
     * Arguments 
     * 
     * @parameter expression="${valgrind.instrumented}"
     * @required
     */
	private String instrumentedExecutablePath;
	
    /**
     * Arguments 
     * 
     * @parameter expression="${valgrind.instrumentedArgs}" default-value=" "
     */
	private String instrumentedExecutableArgs;
	
	
	protected String getExecutable() {
		return "valgrind";
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
     * @parameter expression="${valgrind.workingdir}"
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
		String sOsName = System.getProperty("os.name");
		// vilgrind will work on linux, macos, everything but windows
		return sOsName.contains("windows");
	}

}
