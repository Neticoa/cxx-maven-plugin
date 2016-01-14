package org.codehaus.plexus.archiver.snappy;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

import java.util.List;
import java.util.ArrayList;
import java.net.URL;

/* SNAPPY
      [INFO] Restricted to JDK 1.5 yet org.xerial.snappy:snappy-java:jar:1.1.1.6:compile
             contains org/xerial/snappy/OSInfo.class targeted to JDK 1.6
      [WARNING] Rule 0: org.apache.maven.plugins.enforcer.EnforceBytecodeVersion failed with message:
      Found Banned Dependency: org.xerial.snappy:snappy-java:jar:1.1.1.6
*/
//import org.xerial.snappy.SnappyInputStream;
import org.codehaus.plexus.archiver.AbstractArchiveContentLister;
import org.codehaus.plexus.archiver.ArchiveContentEntry;
import org.codehaus.plexus.archiver.ArchiverException;

import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;

import org.apache.commons.io.FilenameUtils;

/**
 * @author Franck Bonin
 * @version $Revision$ $Date$
 */
public class SnappyArchiveContentLister
    extends AbstractArchiveContentLister
{
    private static final String OPERATION_SNAPPY = "snappy";

    public SnappyArchiveContentLister()
    {
    }

    public SnappyArchiveContentLister( File sourceFile )
    {
        super( sourceFile );
    }
    
    /**
     * Snappy content resource
     */
    protected class SnappyFileInfo
    extends AbstractPlexusIoResource
    {
        private final File sourceFile;
        
        public SnappyFileInfo( File sourceFile )
        {
            super( sourceFile.getName(), sourceFile.lastModified(),
                 PlexusIoResource.UNKNOWN_RESOURCE_SIZE, true, false, true );
            this.sourceFile = sourceFile;
        }

        public URL getURL()
            throws IOException
        {
            return null;
        }

        public InputStream getContents()
            throws IOException
        {
            // SNAPPY
            return /*new SnappyInputStream*/( new FileInputStream( sourceFile ) );
        }
    }
    
    protected List<ArchiveContentEntry> execute()
        throws ArchiverException
    {
        ArrayList<ArchiveContentEntry> archiveContentList = new ArrayList<ArchiveContentEntry>();
        getLogger().debug( "listing: " + getSourceFile() );

        ArchiveContentEntry ae = ArchiveContentEntry.createFileEntry( 
            FilenameUtils.removeExtension( getSourceFile().getName() ), new SnappyFileInfo( getSourceFile() ), -1 );
        archiveContentList.add( ae );

        getLogger().debug( "listing complete" );
        
        return archiveContentList;
    }
}
