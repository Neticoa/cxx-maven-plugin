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
 * Goal which makes workspace.
 *
 * @author Franck Bonin 
 * @goal make
 * @phase compile
 * 
 */
public class MakeMojo extends AbstractLaunchMojo {

	protected List getArgsList() {
		return null;
	}

    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${make.args}"
     * @since 0.0.4
     */
	private String commandArgs;
	protected String getCommandArgs() {
		return commandArgs;
	}

    /**
     * make commands names per OS name
     * os name match is java.lang.System.getProperty("os.name")
     * 
     * @parameter
     * @since 0.0.4
     */
    private Map makecommandPerOS = new HashMap();
	
	protected String getExecutable() {
		String sOsName = System.getProperty("os.name");
		sOsName = sOsName.replace(" ", "");
		getLog().info( "os.name is \"" + sOsName + "\"" );
		if (makecommandPerOS == null)
		{
			return "make";
		}
		else
		{
			if (makecommandPerOS.containsKey(sOsName)) {
				return (String) makecommandPerOS.get(sOsName);
			} else {
				return "make";
			}
		}
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
     * directory location where make will execute
     * 
     * @parameter expression="${make.projectdir}"
     * @since 0.0.4
     */
    private File projectDir;
    
	protected File getWorkingDir() {
		if (null == projectDir) {
			projectDir = new File(basedir.getPath());
		}
		return projectDir;
	}
	
	public boolean isSkip() {
		return false;
	}

}
