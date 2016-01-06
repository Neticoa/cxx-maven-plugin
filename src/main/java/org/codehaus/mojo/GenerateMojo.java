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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.util.regex.Pattern;


/**
 * Generates a new project from an archetype, or updates the actual project if using a partial archetype.
 * If the project is generated or updated in the current directory.
 *
 * @description Creates a project from an archetype. Use archetypeArtifactId property.
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
     * The archetype's artifactId {cmake-cpp-project | aggregator-pom | project-parent-pom | cpp-super-pom}
     * @parameter expression="${archetypeArtifactId}" default-value="cmake-cpp-project"
     * @since 0.0.6
     */
    //@Parameter( property = "archetypeArtifactId" )
    private String archetypeArtifactId;

    /**
     * The archetype's groupId (not used, reserved for futur usage).
     * @parameter expression="${archetypeGroupId}" default-value="org.codehaus.mojo.cxx-maven-plugin"
     * @since 0.0.6
     */
    //@Parameter( property = "archetypeGroupId" )
    private String archetypeGroupId;

    /**
     * The archetype's version (not used, reserved for futur usage).
     * @parameter expression="${archetypeVersion}" default-value="0.1"
     * @since 0.0.6
     */
    //@Parameter( property = "archetypeVersion" )
    private String archetypeVersion;
    
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
    
    /**
     * The generated pom parent groupId.
     * @parameter expression="${parentGroupId}" default-value="fr.neticoa"
     * @required
     * @since 0.0.6
     */
    private String parentGroupId;
    
    /**
     * The generated pom parent artifactId.
     * @parameter expression="${parentArtifactId}" default-value="cpp-super-pom"
     * @required
     * @since 0.0.6
     */
    private String parentArtifactId;
    
    /**
     * The generated pom parent version.
     * @parameter expression="${parentVersion}" default-value="1.0.0.0"
     * @required
     * @since 0.0.6
     */
    private String parentVersion;
    
    /**
     * The generated pom groupId .
     * @parameter expression="${groupId}" default-value="fr.neticoa"
     * @required
     * @since 0.0.6
     */
    private String groupId;
    
    /**
     * The generated pom artifact Name.
     * @parameter expression="${artifactName}"
     * @required
     * @since 0.0.6
     */
    private String artifactName;
     
    /**
     * The generated pom artifactId
     * @parameter expression="${artifactId}"
     * @required
     * @since 0.0.6
     */
    private String artifactId;
    
    /**
     * The generated pom version.
     * @parameter expression="${version}" default-value="0.0.0.1"
     * @since 0.0.6
     */
    private String version;
    
    protected Map<String, String> listResourceFolderContent(String path, Map valuesMap)
    {
        String location = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        final File jarFile = new File(location);
        
        Map resources = new HashMap<String, String>();
        StrSubstitutor substitutor = new StrSubstitutor(valuesMap);
        
        path = (StringUtils.isEmpty(path))? "" : path + "/";
        getLog().debug("listResourceFolderContent : " + location + ", sublocation : " + path);
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
                        String resourceFile = File.separator + name;
                        if (! (resourceFile.endsWith("/") || resourceFile.endsWith("\\")) )
                        {
                            getLog().debug("resource entry = " + resourceFile);
                            String destFile = substitutor.replace(resourceFile);
                            getLog().debug("become entry = " + destFile);
                            resources.put(resourceFile, destFile);
                        }
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
                        String resourceFile = name.getPath();
                        if (! (resourceFile.endsWith("/") || resourceFile.endsWith("\\")) )
                        {
                            getLog().debug("resource entry = " + resourceFile);
                            String destFile = substitutor.replace(resourceFile);
                            getLog().debug("become entry = " + destFile);
                            resources.put(resourceFile, destFile);
                        }
                    }
                }
                catch (URISyntaxException ex)
                {
                    // never happens
                }
            }
        }
        return resources;
    }


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        //Properties systemProperties = session.getSystemProperties();
        //Properties userProperties = session.getUserProperties();
        //Properties properties = session.getExecutionProperties();
        
        Map valuesMap = new HashMap();
        valuesMap.put("parentGroupId", parentGroupId);
        valuesMap.put("parentArtifactId", parentArtifactId);
        valuesMap.put("parentVersion", parentVersion);
        valuesMap.put("groupId", groupId);
        valuesMap.put("artifactId", artifactId);
        valuesMap.put("artifactName", artifactName);
        valuesMap.put("version", version);

        //1/ search for properties
        // -DgroupId=fr.neticoa -DartifactName=QtUtils -DartifactId=qtutils -Dversion=1.0-SNAPSHOT
        
        Map<String, String> resources = listResourceFolderContent(archetypeArtifactId, valuesMap);
        
        //2/ unpack resource to destdir 
        getLog().info("basdir = " + basedir);
        
        StrSubstitutor substitutor = new StrSubstitutor(valuesMap, "$(", ")");
        for (Map.Entry<String, String> entry : resources.entrySet())
        {
            String curRes = entry.getKey();
            String curDest = entry.getValue();
            InputStream resourceStream = null;
            resourceStream = getClass().getResourceAsStream(curRes);
            getLog().debug("resource stream to open : "+ curRes);
            getLog().debug("destfile to open : "+ curDest);
            if (null != resourceStream)
            {
                String sRelativePath = curDest.replaceFirst(Pattern.quote(archetypeArtifactId + File.separator), "");
                File newFile = new File(basedir + File.separator + sRelativePath);
                
                //3/ create empty dir struct; if needed using a descriptor 
                //create all non exists folders
                File newDirs = new File(newFile.getParent());
                if (Files.notExists(Paths.get(newDirs.getPath())))
                {
                    getLog().info("dirs to generate : "+ newDirs.getAbsoluteFile());
                    newDirs.mkdirs();
                }
                
                if (! newFile.getName().equals("empty.dir"))
                {
                    getLog().info("file to generate : "+ newFile.getAbsoluteFile());
                    try
                    { 
                        if (!newFile.createNewFile())
                        {
                            // duplicate existing file
                            FileInputStream inStream = new FileInputStream(newFile);
                            File backFile = File.createTempFile(newFile.getName(), ".back", newFile.getParentFile());
                            FileOutputStream outStream = new FileOutputStream(backFile);
                            
                            IOUtils.copy( inStream, outStream );
                            // manage file times
                            //backFile.setLastModified(newFile.lastModified());
                            BasicFileAttributes attributesFrom = 
                                Files.getFileAttributeView(Paths.get(newFile.getPath()),
                                    BasicFileAttributeView.class).readAttributes();
                            BasicFileAttributeView attributesToView =
                                Files.getFileAttributeView(Paths.get(backFile.getPath()),
                                    BasicFileAttributeView.class);
                            attributesToView.setTimes(
                                attributesFrom.lastModifiedTime(),
                                attributesFrom.lastAccessTime(),
                                attributesFrom.creationTime());

                            inStream.close();
                            outStream.close();
                        }
                        
                        FileOutputStream outStream = new FileOutputStream(newFile);
                        
                        //4/ variable substitution :
                        // change prefix and suffix to '$(' and ')'
                        // see https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/text/StrSubstitutor.html
                        String content = IOUtils.toString(resourceStream, "UTF8");
                        content = substitutor.replace(content);

                        //IOUtils.copy( resourceStream, outStream );
                        IOUtils.write(content, outStream, "UTF8");

                        outStream.close();
                        resourceStream.close();
                    }
                    catch ( IOException e )
                    {
                        getLog().error( "File " + newFile.getAbsoluteFile() + " can't be created : " + e);
                    }
                }
            }            
        }
    }
}
