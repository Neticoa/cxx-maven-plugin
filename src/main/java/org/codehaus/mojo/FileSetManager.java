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

import java.io.File;
import java.util.List;

import org.apache.maven.model.FileSet;

import org.codehaus.plexus.util.DirectoryScanner;

/**
 * This FileSetManager always exclude default files (.svn, etc.) and scan symlinks.
 *
 * @author Franck Bonin 
 * 
 */
public class FileSetManager {

    private DirectoryScanner scan( FileSet fileSet )
    {
        File basedir = new File( fileSet.getDirectory() );

        if ( !basedir.exists() || !basedir.isDirectory() )
        {
             return null;
        }

        DirectoryScanner scanner = new DirectoryScanner();

        List includesList = fileSet.getIncludes();
        List excludesList = fileSet.getExcludes();

        if ( includesList.size() > 0 )
        {
             scanner.setIncludes( (String[]) includesList.toArray(new String[0]) );
        }

        if ( excludesList.size() > 0 )
        {
             scanner.setExcludes( (String[]) excludesList.toArray(new String[0]) );
        }

        if ( true )//fileSet.isUseDefaultExcludes() )
        {
             scanner.addDefaultExcludes();
        }

        scanner.setBasedir( basedir );
        scanner.setFollowSymlinks( true );//fileSet.isFollowSymlinks() );

        scanner.scan();

        return scanner;
    }
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public String[] getIncludedFiles( FileSet fileSet )
    {
        DirectoryScanner scanner = scan( fileSet );
     
        if ( scanner != null )
        {
            return scanner.getIncludedFiles();
        }
     
        return EMPTY_STRING_ARRAY;
    }
    
    public String[] getExcludedFiles( FileSet fileSet )
    {
         DirectoryScanner scanner = scan( fileSet );
   
         if ( scanner != null )
         {
           return scanner.getExcludedFiles();
         }
   
         return EMPTY_STRING_ARRAY;
     }

    public String[] getIncludedDirectories( FileSet fileSet )
    {
        DirectoryScanner scanner = scan( fileSet );

        if ( scanner != null )
        {
          return scanner.getIncludedDirectories();
        }

        return EMPTY_STRING_ARRAY;
    }
    
    public String[] getExcludedDirectories( FileSet fileSet )
    {
        DirectoryScanner scanner = scan( fileSet );

        if ( scanner != null )
        {
          return scanner.getExcludedDirectories();
        }

        return EMPTY_STRING_ARRAY;
    }
}
