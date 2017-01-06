package org.apache.maven.plugin.cxx.utils.release;

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
 
import org.apache.maven.shared.release.config.ReleaseDescriptor;

/*
 * 
 * 
 * ReleaseDescriptor :

MAP releaseVersions = MAP des versions release à tagger ou brancher, une par artefact
                  clé = ProjecId
                  valeur = string (une version)
MAP developmentVersions = MAP des versions dev auquelles revenir apres le tag ou la branche, une par artefact
                  clé = ProjecId
                  valeur = string (une version)
MAP resolvedSnapshotDependencies = MAP qui transforme les versions des dépendence encore en version SNAPSHOT vers
*                                  des versions release. voir CheckDependencySnapshotsPhase, 
                  clé = ProjecId 
                  valeur = une map à deux entrées 
                             Cle1 "ReleaseDescriptor.RELEASE_KEY"
                             valeur1 = string (la version release à tagger ou brancher)
                             Cle2 "ReleaseDescriptor.DEVELOPMENT_KEY"
                             valeur2 = string (la version dev à laquelle revenir apres le tag ou la branche)
 */

public class CxxReleaseDescriptor extends ReleaseDescriptor
{
    /**
     * run cycle in simulation mode
     */
    private boolean dryRun = true;
    
    public void setDryRun( boolean dryRun )
    {
        this.dryRun = dryRun;
    }
    
    public boolean getDryRun( )
    {
        return dryRun;
    }
      
    /**
     * new main artifactId if any
     */
    private String artifactId = null;
    
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }
    
    public String getArtifactId( )
    {
        return artifactId;
    }
    
    /**
     * 
     */
    private boolean snapshotDevelopmentVersion = false;
    
    public void setSnapshotDevelopmentVersion( boolean snapshotDevelopmentVersion )
    {
        this.snapshotDevelopmentVersion = snapshotDevelopmentVersion;
    }
    
    public boolean isSnapshotDevelopmentVersion( )
    {
        return snapshotDevelopmentVersion;
    }
    
}
