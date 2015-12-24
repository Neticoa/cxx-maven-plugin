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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
/*import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;*/
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;

/**
 * Generates a new project from an archetype, or updates the actual project if using a partial archetype.
 * If the project is fully generated, it is generated in a directory corresponding to its artifactId.
 * If the project is updated with a partial archetype, it is done in the current directory.
 *
 * @description Creates a project from an archetype.
 * @requiresProject false
 * @goal generate
 * @author Franck Bonin 
 * 
 */
//@Mojo( name = "generate", requiresProject = false )
//@Execute( phase = LifecyclePhase.GENERATE_SOURCES )
public class GenerateMojo
    extends AbstractMojo
    implements ContextEnabled
{

    /**
     * The archetype's artifactId.
     * @parameter expression="${archetypeArtifactId}" default-value=""
     * @since 0.0.6
     */
    //@Parameter( property = "archetypeArtifactId" )
    private String archetypeArtifactId;

    /**
     * The archetype's groupId.
     * @parameter expression="${archetypeGroupId}" default-value=""
     * @since 0.0.6
     */
    //@Parameter( property = "archetypeGroupId" )
    private String archetypeGroupId;

    /**
     * The archetype's version.
     * @parameter expression="${archetypeVersion}" default-value=""
     * @since 0.0.6
     */
    //@Parameter( property = "archetypeVersion" )
    private String archetypeVersion;

    /**
     * The archetype's repository.
     * @parameter expression="${archetypeRepository}" default-value=""
     * @since 0.0.6
     */
    //@Parameter( property = "archetypeRepository" )
    private String archetypeRepository;


    /**
     * Local Maven repository.
     * @parameter default-value="${localRepository}" 
     * @required
     * @readonly
     * @since 0.0.6
     */
    //@Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository localRepository;

    /**
     * List of remote repositories used by the resolver.
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     * @since 0.0.6
     */
    //@Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true )
    private List<ArtifactRepository> remoteArtifactRepositories;
    
    /**
     * Pom directory location
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     * @since 0.0.6
     */
    //@Parameter( defaultValue = "${basedir}" )
    private File basedir;

    /**
     * Pom directory location
     * @parameter expression="${session}"
     * @required
     * @readonly
     * @since 0.0.6
     */
    //@Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession session;
    
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     * @since 0.0.5
     */
    //@Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;
    
    protected String[] listResourceFolderContent(String path)
    {
        String location = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        final File jarFile = new File(location);
        
        path = (StringUtils.isEmpty(path))? "" : path + "/";
        getLog().info("listResourceFolderContent : " + location + ", sublocation : " + path);
        if(jarFile.isFile())
        {
            getLog().info("jar case");
            try
            {
                final JarFile jar = new JarFile(jarFile);
                final Enumeration<JarEntry> entries = jar.entries();
                while(entries.hasMoreElements())
                {
                    final String name = entries.nextElement().getName();
                    if (name.startsWith(path))
                    { 
                        getLog().info("resource entry = " + name);
                    }
                }
                jar.close();
            }
            catch (IOException ex)
            {
            }
        }
        else
        {
            getLog().info("file case");
            //final URL url = Launcher.class.getResource("/" + path);
            final URL url = getClass().getResource("/" + path);
            if (url != null)
            {
                try
                {
                    final File names = new File(url.toURI());
                    for (File name : names.listFiles())
                    {
                        getLog().info("resource entry = " + name);
                    }
                }
                catch (URISyntaxException ex)
                {
                    // never happens
                }
            }
        }
        return null;
    }


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        //Properties systemProperties = session.getSystemProperties();
        //Properties userProperties = session.getUserProperties();
        Properties properties = session.getExecutionProperties();
        
        listResourceFolderContent("cxx-maven-project");

    }
}
