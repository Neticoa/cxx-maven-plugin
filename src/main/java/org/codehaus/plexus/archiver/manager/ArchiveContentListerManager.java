package org.codehaus.plexus.archiver.manager;

/*
 * Copyright  2001,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


import java.io.File;

//import javax.annotation.Nonnull;

import org.codehaus.plexus.archiver.ArchiveContentLister;

/**
 * @author Franck Bonin
 * @version $Revision$ $Date$
 */
public interface ArchiveContentListerManager
{
    String ROLE = ArchiveContentListerManager.class.getName();
    
    ArchiveContentLister getArchiveContentLister( String archiveContentListerName )
        throws NoSuchArchiverException;
        
    ArchiveContentLister getArchiveContentLister( File file )
        throws NoSuchArchiverException;

	/*@Nonnull Archiver getArchiver( @Nonnull String archiverName )
        throws NoSuchArchiverException;

	@Nonnull Archiver getArchiver( @Nonnull File file )
      throws NoSuchArchiverException;
    
    @Nonnull UnArchiver getUnArchiver( @Nonnull String unArchiverName )
        throws NoSuchArchiverException;

	@Nonnull UnArchiver getUnArchiver(  @Nonnull File file )
        throws NoSuchArchiverException;    

    @Nonnull PlexusIoResourceCollection getResourceCollection( @Nonnull File file )
        throws NoSuchArchiverException;

	@Nonnull
    PlexusIoResourceCollection getResourceCollection( String unArchiverName )
        throws NoSuchArchiverException;*/
}
