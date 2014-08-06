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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.Executor;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which launch unit test  suing custum command.
 * This mojo just tell programmer where to produce xUnit reports
 *
 * @author Franck Bonin 
 * @goal xunit
 * @phase test
 * 
 */
public class XUnitMojo extends LaunchMojo {

    /**
     * The Report OutputFile Location.
     * 
     * @parameter expression="${xunit.reportsfilePath}" default-value="xunit-reports"
     * @since 0.0.4
     */
    private File reportsfileDir;
    
    protected void preExecute(Executor exec, CommandLine commandLine, Map enviro) throws MojoExecutionException
    {
        // this
        String OutputReportDir = new String();
        if (reportsfileDir.isAbsolute()) {
            OutputReportDir = reportsfileDir.getAbsolutePath();
        } else {
            OutputReportDir = basedir.getAbsolutePath() + "/" + reportsfileDir.getPath();
        }
        new File(OutputReportDir).mkdirs();
        getLog().info("You shall produce a xUnit report called \""+ OutputReportDir + "/xunit-result-*.xml\" within this xunit goal" );
        File file = new File(OutputReportDir + "/Readme.txt");
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
            out.writeBytes("You shall produce xUnit reports called \"xunit-result-*.xml\" within this directory.\n");
        } catch (IOException e) {
        }
    }
    
    /**
     * The Xunit Skip feature.
     * 
     * @parameter expression="${xunit.skiptests}" default-value="false"
     * @since 0.0.5
     */
    private boolean skiptests;
    
    protected boolean isSkip() {
        return super.isSkip() || skiptests;
    }
}
