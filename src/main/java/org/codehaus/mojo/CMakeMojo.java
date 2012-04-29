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

import org.apache.commons.lang.StringUtils;

/**
 * Goal which cmakes workspace.
 *
 * @author Franck Bonin 
 * @goal cmake
 * @phase initialize
 * 
 */
public class CMakeMojo extends AbstractLaunchMojo {

	protected List getArgsList() {
		return null;
	}

    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${cmake.projectdir}"
     * @since 0.0.4
     */
	private String projectDir;
	
	protected String getProjectDir() {
		if (null == projectDir) {
			projectDir = new String(basedir.getAbsolutePath());
		}
		return projectDir;
	}
	
    /**
     * Arguments for the executed program
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
	
	protected String getCommandArgs() {
		String result = new String();
		if (!StringUtils.isEmpty(commandArgs)) {
			result = commandArgs + " ";
		}
		result += "\"" + getProjectDir() + "\" -G " + generator;
		return result;
	}
	


	protected String getExecutable() {
		return "cmake";
	}

	
    /**
     * Environment variables to pass to the cmake program.
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
     * Out of source directory
     * 
     * @parameter expression="${cmake.outsourcedir}"
     * @since 0.0.4
     */
    private File outsourceDir;
    
	protected File getWorkingDir() {
		if (null == outsourceDir) {
			outsourceDir = new File(basedir.getPath());
		}
		return outsourceDir;
	}

	public boolean isSkip() {
		return false;
	}

}
