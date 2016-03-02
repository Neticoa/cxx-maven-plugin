package org.codehaus.plexus.archiver;

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

import org.codehaus.plexus.components.io.fileselectors.FileSelector;

import java.io.File;
import java.util.List;

/**
 * @author Franck Bonin
 * @version $Revision$ $Date$
 */
public interface ArchiveContentLister
{
    String ROLE = ArchiveContentLister.class.getName();

    /**
     * list the archive content.
     * 
     * @throws ArchiverException
     */
    List<ArchiveContentEntry> list()
        throws ArchiverException;
        
    /*
     * Take a path into the archive and list it (futur).
     * 
     * @param path
     *            Path inside the archive to be listed.
     * @throws ArchiverException
     */
    /*void extract( String path )
        throws ArchiverException;*/

    File getSourceFile();

    void setSourceFile( File sourceFile );

    /**
     * Sets a set of {@link FileSelector} instances, which may be used to select the files to extract from the archive.
     * If file selectors are present, then a file is only extracted, if it is confirmed by all file selectors.
     */
    void setFileSelectors( FileSelector[] selectors );

    /**
     * Returns a set of {@link FileSelector} instances, which may be used to select the files to extract from the
     * archive. If file selectors are present, then a file is only extracted, if it is confirmed by all file selectors.
     */
    FileSelector[] getFileSelectors();
}
