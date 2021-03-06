package org.codehaus.plexus.archiver.gzip;

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

import java.util.zip.GZIPInputStream;
import org.codehaus.plexus.archiver.AbstractArchiveContentLister;
import org.codehaus.plexus.archiver.ArchiveContentEntry;
import org.codehaus.plexus.archiver.ArchiverException;

//import javax.annotation.Nonnull;

//import static org.codehaus.plexus.archiver.util.Streams.*;

import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;

import org.apache.commons.io.FilenameUtils;

/**
 * @author Franck Bonin
 * @version $Revision$ $Date$
 */
public class GZipArchiveContentLister
    extends AbstractArchiveContentLister
{
    private static final String OPERATION_GZIP = "gzip";

    public GZipArchiveContentLister()
    {
    }

    public GZipArchiveContentLister( File sourceFile )
    {
        super( sourceFile );
    }
    
    /**
     * GZip content resource
     */
    protected class GZipFileInfo
    extends AbstractPlexusIoResource
    {
        private final File sourceFile;
        
        public GZipFileInfo( File sourceFile )
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
            return new GZIPInputStream( new FileInputStream( sourceFile ) );
        }
    }
    
    protected List<ArchiveContentEntry> execute()
        throws ArchiverException
    {
        ArrayList<ArchiveContentEntry> archiveContentList = new ArrayList<ArchiveContentEntry>();
        getLogger().debug( "listing: " + getSourceFile() );

        ArchiveContentEntry ae = ArchiveContentEntry.createFileEntry( 
            FilenameUtils.removeExtension( getSourceFile().getName() ), new GZipFileInfo( getSourceFile() ), -1 );
        archiveContentList.add( ae );

        getLogger().debug( "listing complete" );
        
        return archiveContentList;
    }
}
