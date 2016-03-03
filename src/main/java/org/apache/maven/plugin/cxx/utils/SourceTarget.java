package org.apache.maven.plugin.cxx.utils;

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

import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.maven.artifact.Artifact;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author fbonin
 * 
 */
public class SourceTarget
{
    /**
     * Option 3 (highest priority). Provide explicit target path for each source dependency
     * 
     * @since 0.0.6
     */
    @Parameter( )
    public String targetDir;

    /**
     * Option 3 (highest priority). Provide explicit target path for each source dependency
     * 
     * @since 0.0.6
     */
    @Parameter( )
    public Dependency dependency;
    
    /**
     * test if provides artifact match this external dependency
     * 
     * @since 0.0.6
     */
    public boolean dependencyMatch( Artifact artifact )
    {
        return dependency != null && artifact != null
            && StringUtils.equals( dependency.getGroupId(), artifact.getGroupId() )
            && StringUtils.equals( dependency.getArtifactId(), artifact.getArtifactId() )
            && ( StringUtils.isEmpty( dependency.getVersion() )
                || StringUtils.equals( dependency.getVersion(), artifact.getVersion() ) )
            // ignore type since if not provided, Dependency class force it to "jar"
            // but for source dependencies it is always "pom"
            /*&& ( StringUtils.isEmpty( dependency.getType() )
                || StringUtils.equals( dependency.getType(), artifact.getType() ) )*/;
    }
}
