package org.codehaus.plexus.archiver;

/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

//import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
//import org.codehaus.plexus.components.io.attributes.SymlinkUtils;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
//import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.logging.AbstractLogEnabled;
//import org.codehaus.plexus.util.FileUtils;
//import org.codehaus.plexus.util.IOUtil;
//import org.codehaus.plexus.util.StringUtils;


import java.io.*;
import java.util.ArrayList;
//import java.util.Date;
import java.util.List;
//import java.util.Iterator;

/**
 * @author Franck Bonin
 * @version $Revision$ $Date$
 */
public abstract class AbstractArchiveContentLister
    extends AbstractLogEnabled
    implements ArchiveContentLister
{
    private File sourceFile;

    private FileSelector[] fileSelectors;

    public AbstractArchiveContentLister()
    {
    }

    public AbstractArchiveContentLister( final File sourceFile )
    {
        this.sourceFile = sourceFile;
    }

    public File getSourceFile()
    {
        return sourceFile;
    }

    public void setSourceFile( final File sourceFile )
    {
        this.sourceFile = sourceFile;
    }

    public final List<ArchiveContentEntry> list()
        throws ArchiverException
    {
        validate();
        return execute();
        //runArchiveFinalizers();
    }
/*
    public final void list( final String path )
        throws ArchiverException
    {
        validate( path );
        execute( path );
        //runArchiveFinalizers();
    }
*/
/*
    public void addArchiveFinalizer( final ArchiveFinalizer finalizer )
    {
        if ( finalizers == null )
        {
            finalizers = new ArrayList();
        }

        finalizers.add( finalizer );
    }

    public void setArchiveFinalizers( final List archiveFinalizers )
    {
        finalizers = archiveFinalizers;
    }

    private void runArchiveFinalizers()
        throws ArchiverException
    {
        if ( finalizers != null )
        {
			for (Object finalizer1 : finalizers) {
				final ArchiveFinalizer finalizer = (ArchiveFinalizer) finalizer1;

				finalizer.finalizeArchiveExtraction(this);
			}
        }
    }
*/
/*
    protected void validate( final String path, final File outputDirectory )
    {
    }
*/

    protected void validate()
        throws ArchiverException
    {
        if ( sourceFile == null )
        {
            throw new ArchiverException( "The source file isn't defined." );
        }

        if ( sourceFile.isDirectory() )
        {
            throw new ArchiverException( "The source must not be a directory." );
        }

        if ( !sourceFile.exists() )
        {
            throw new ArchiverException( "The source file " + sourceFile + " doesn't exist." );
        }
/*
        if ( destDirectory == null && destFile == null )
        {
            throw new ArchiverException( "The destination isn't defined." );
        }

        if ( destDirectory != null && destFile != null )
        {
            throw new ArchiverException( "You must choose between a destination directory and a destination file." );
        }

        if ( destDirectory != null && !destDirectory.isDirectory() )
        {
            destFile = destDirectory;
            destDirectory = null;
        }

        if ( destFile != null && destFile.isDirectory() )
        {
            destDirectory = destFile;
            destFile = null;
        }
*/
    }

    public void setFileSelectors( final FileSelector[] fileSelectors )
    {
        this.fileSelectors = fileSelectors;
    }

    public FileSelector[] getFileSelectors()
    {
        return fileSelectors;
    }

    protected boolean isSelected( final String fileName, final /*PlexusIoResource*/ FileInfo fileInfo )
        throws ArchiverException
    {
        if ( fileSelectors != null )
        {
            for (FileSelector fileSelector : fileSelectors)
            {
                try {
                    if (!fileSelector.isSelected(fileInfo))
                    {
                        return false;
                    }
                }
                catch (final IOException e)
                {
                    throw new ArchiverException("Failed to check, whether " + fileInfo.getName() + " is selected: "
                        + e.getMessage(), e);
                }
            }
        }
        return true;
    }

    protected abstract List<ArchiveContentEntry> execute()
        throws ArchiverException;

/*    
    protected abstract void execute( String path)
        throws ArchiverException;
*/

/*
    protected void extractFile( final File srcF, final File dir, final InputStream compressedInputStream,
                                final String entryName, final Date entryDate, final boolean isDirectory,
                                final Integer mode, String symlinkDestination )
        throws IOException, ArchiverException
    {
        // Hmm. Symlinks re-evaluate back to the original file here. Unsure if this is a good thing...
        final File f = FileUtils.resolveFile( dir, entryName );

        try
        {
            if ( !isOverwrite() && f.exists() && ( f.lastModified() >= entryDate.getTime() ) )
            {
                return;
            }

            // create intermediary directories - sometimes zip don't add them
            final File dirF = f.getParentFile();
            if ( dirF != null )
            {
                dirF.mkdirs();
            }

            if ( !StringUtils.isEmpty( symlinkDestination )){
                SymlinkUtils.createSymbolicLink( f, new File( symlinkDestination) );
            }
            else if ( isDirectory )
            {
                f.mkdirs();
            }
            else
            {
                OutputStream out = null;
                try
                {
                    out = new FileOutputStream( f );

                    IOUtil.copy( compressedInputStream, out );
                }
                finally
                {
                    IOUtil.close( out );
                }
            }

            f.setLastModified( entryDate.getTime() );

            if ( !isIgnorePermissions() && mode != null && !isDirectory)
            {
                ArchiveEntryUtils.chmod( f, mode, getLogger(), isUseJvmChmod() );
            }
        }
        catch ( final FileNotFoundException ex )
        {
            getLogger().warn( "Unable to expand to file " + f.getPath() );
        }
    }
*/
}
