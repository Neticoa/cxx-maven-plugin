package org.codehaus.mojo;

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

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Add more source directories to the POM.
 *
 * @author Franck Bonin
 * @version $Id$
 * @since 0.0.5
 */
@Mojo( name = "addsource", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true )
public class AddSourceMojo extends AbstractMojo
{
    /**
     * directory were sources are
     * 
     * @since 0.0.4
     */
    @Parameter()
    private List sourceDirs = new ArrayList();

    /**
     * @since 0.0.5
     */
    @Parameter( property = "project", readonly = true, required = true )
    private MavenProject project;

    public void execute()
    {   
        getLog().info( "Cxx Maven Plugin AddSource " );
        Iterator it = sourceDirs.iterator();
        while ( it.hasNext() )
        {
            File source = new File( (String) it.next() );
            this.project.addCompileSourceRoot( source.getAbsolutePath() );
            getLog().info( "Source directory: \"" + source.getAbsolutePath()
                 + "\" added to Maven Project." );              
        }
    }
}

