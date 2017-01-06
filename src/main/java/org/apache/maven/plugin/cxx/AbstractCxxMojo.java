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

import org.apache.maven.plugin.AbstractMojo;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * Base class with shared configuration for cxx mojo.
 *
 * @author 
 * @version
 */
public abstract class AbstractCxxMojo
    extends AbstractMojo
{
    /**
     */
    @Parameter( defaultValue = "${basedir}", readonly = true, required = true )
    protected File basedir;

    /**
     */
    @Parameter( defaultValue = "${settings}", readonly = true, required = true )
    protected Settings settings;

    /**
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;
    
    /**
     */
    @Parameter( defaultValue = "${reactorProjects}", readonly = true, required = true )
    protected List<MavenProject> reactorProjects;
    
    /**
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;
  
}
