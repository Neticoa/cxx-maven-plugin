package org.codehaus.plexus.archiver.tar;

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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.codehaus.plexus.archiver.AbstractArchiveContentLister;
import org.codehaus.plexus.archiver.ArchiveContentEntry;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.util.IOUtil;
import org.xerial.snappy.SnappyInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.List;
import java.util.ArrayList;


/**
 * @author Franck Bonin
 * @version $Revision$ $Date$
 */
public class TarArchiveContentLister
    extends AbstractArchiveContentLister
{
    public TarArchiveContentLister()
    {
    }

    public TarArchiveContentLister( File sourceFile )
    {
        super( sourceFile );
    }

    /**
     * compression method
     */
    private UntarCompressionMethod compression = UntarCompressionMethod.NONE;

    /**
     * Set decompression algorithm to use; default=none.
     * <p/>
     * Allowable values are
     * <ul>
     * <li>none - no compression</li>
     * <li>gzip - Gzip compression</li>
     * <li>bzip2 - Bzip2 compression</li>
     * <li>snappy - Snappy compression</li>
     * </ul>
     *
     * @param method compression method
     */
    public void setCompression( UntarCompressionMethod method )
    {
        compression = method;
    }

    /**
     * No encoding support in Untar.
     */
    public void setEncoding( String encoding )
    {
        getLogger().warn( "The TarArchiveContentLister doesn't support the encoding attribute" );
    }
    
    private static class MyTarResource
        extends TarResource
    {   
        public MyTarResource( TarFile tarFile, TarArchiveEntry entry )
        {
            super(tarFile, entry);
        }
        
        public ArchiveContentEntry asArchiveContentEntry()
        {
            if ( isSymbolicLink() )
            {
                return ArchiveContentEntry.createSymlinkEntry( getName(), this, -1 );
            }
            else if ( isDirectory() )
            {
                return ArchiveContentEntry.createDirectoryEntry( getName(), this, -1 );
            }
            else
            {
                return ArchiveContentEntry.createFileEntry( getName(), this, -1 );
            }
        }
    };
      
    protected List<ArchiveContentEntry> execute()
        throws ArchiverException
    {
        ArrayList<ArchiveContentEntry> archiveContentList = new ArrayList<ArchiveContentEntry>();
        getLogger().debug( "listing: " + getSourceFile() );
        TarArchiveInputStream tis = null;
        try
        {
            TarFile tarFile = new TarFile( getSourceFile() );
            tis = new TarArchiveInputStream(
                decompress( compression, getSourceFile(), new BufferedInputStream( new FileInputStream( getSourceFile() ) ) ) );
            TarArchiveEntry te;
            while ( ( te = tis.getNextTarEntry() ) != null )
            {
                MyTarResource fileInfo = new MyTarResource( tarFile, te );
                if ( isSelected( te.getName(), fileInfo ) )
                {
                    ArchiveContentEntry ae = fileInfo.asArchiveContentEntry();
                    archiveContentList.add(ae);
                }
            }
            getLogger().debug( "listing complete" );
        }
        catch ( final IOException ioe )
        {
            throw new ArchiverException( "Error while listing " + getSourceFile().getAbsolutePath(), ioe );
        }
        finally
        {
        }
        return archiveContentList;
    }

/*
    protected void execute()
        throws ArchiverException
    {
        execute( getSourceFile(), getDestDirectory() );
    }

    protected void execute( String path, File outputDirectory )
    {
        execute( new File( path ), getDestDirectory() );
    }

    protected void execute( File sourceFile, File destDirectory )
        throws ArchiverException
    {
        TarArchiveInputStream tis = null;
        try
        {
            getLogger().info( "Expanding: " + sourceFile + " into " + destDirectory );
            TarFile tarFile = new TarFile( sourceFile );
            tis = new TarArchiveInputStream(
                decompress( compression, sourceFile, new BufferedInputStream( new FileInputStream( sourceFile ) ) ) );
            TarArchiveEntry te;
            while ( ( te = tis.getNextTarEntry() ) != null )
            {
                TarResource fileInfo = new TarResource( tarFile, te );
                if ( isSelected( te.getName(), fileInfo ) )
                {
                    final String symlinkDestination = te.isSymbolicLink() ? te.getLinkName() : null;
                    extractFile( sourceFile, destDirectory, tis, te.getName(), te.getModTime(), te.isDirectory(),
                                 te.getMode() != 0 ? te.getMode() : null, symlinkDestination );
                }

            }
            getLogger().debug( "expand complete" );

        }
        catch ( IOException ioe )
        {
            throw new ArchiverException( "Error while expanding " + sourceFile.getAbsolutePath(), ioe );
        }
        finally
        {
            IOUtil.close( tis );
        }
    }
*/
    /**
     * This method wraps the input stream with the
     * corresponding decompression method
     *
     * @param file    provides location information for BuildException
     * @param istream input stream
     * @return input stream with on-the-fly decompression
     * @throws IOException thrown by GZIPInputStream constructor
     */
    private InputStream decompress( UntarCompressionMethod compression, final File file, final InputStream istream )
        throws IOException, ArchiverException
    {
        if ( compression == UntarCompressionMethod.GZIP )
        {
            return new GZIPInputStream( istream );
        }
        else if ( compression == UntarCompressionMethod.BZIP2 )
        {
            return new BZip2CompressorInputStream( istream );
        }
        else if ( compression == UntarCompressionMethod.SNAPPY )
        {
            return new SnappyInputStream( istream );
        }
        return istream;
    }

    /**
       * Valid Modes for Compression attribute to Untar Task
       */
    public static enum UntarCompressionMethod
    {
        NONE("none"), GZIP("gzip"), BZIP2("bzip2"), SNAPPY("snappy");

        final String value;

        /**
         * Constructor
         */
        UntarCompressionMethod(String value)
        {
            this.value = value;
        }
    }
}
