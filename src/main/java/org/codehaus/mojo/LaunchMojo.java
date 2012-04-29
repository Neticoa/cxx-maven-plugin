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
 * Goal which Launch an external executable.
 *
 * @author Franck Bonin 
 * @goal launch
 * 
 */
public class LaunchMojo extends AbstractLaunchMojo {

    /**
     * Can be of type <code>&lt;argument&gt;</code> or <code>&lt;classpath&gt;</code> Can be overriden using "cxx.args"
     * env. variable
     * 
     * @parameter
     * @since 0.0.4
     */
	private List arguments;
	protected List getArgsList() {
		return arguments;
	}

    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${launch.args}"
     */
	private String commandArgs;
	protected String getCommandArgs() {
		return commandArgs;
	}

    /**
     * The executable. Can be a full path or a the name executable. In the latter case, the executable must be in the
     * PATH for the execution to work.
     * 
     * @parameter expression="${launch.executable}"
     * @required
     * @since 0.0.4
     */
    private String executable;
	protected String getExecutable() {
		return executable;
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

    /**
     * Exit codes to be resolved as successful execution for non-compliant applications (applications not returning 0
     * for success).
     * 
     * @parameter
     * @since 0.0.4
     */
    private List successCodes;
	protected List getSuccesCode() {
		return successCodes;
	}

    /**
     * The current working directory. Optional. If not specified, basedir will be used.
     * 
     * @parameter expression="${launch.workingdir}"
     * @since 0.0.4
     */
    private File workingDir;
	protected File getWorkingDir() {
		if (null == workingDir) {
			workingDir = new File(basedir.getPath());
		}
		return workingDir;
	}

    /**
     * Os for which command shall be executed
     * os name match is java.lang.System.getProperty("os.name")
     * 
     * @parameter
     * @since 0.0.4
     */
    private List includeOS;
    
    /**
     * Os for which command shall not be executed
     * os name match is java.lang.System.getProperty("os.name")
     *  
     * @parameter
     * @since 0.0.4
     */
    private List excludeOS;
	
	protected boolean isSkip() {
		String sOsName = System.getProperty("os.name");
		if (null == excludeOS && null == includeOS)	{
			return false;
		} else if (null == includeOS) {
			return excludeOS.contains(sOsName);
		} else if (null == excludeOS) {
			return !includeOS.contains(sOsName);
		} else {
			return (excludeOS.contains(sOsName) || !includeOS.contains(sOsName));
		}
	}

}
