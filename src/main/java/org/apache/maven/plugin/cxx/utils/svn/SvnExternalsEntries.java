package org.apache.maven.plugin.cxx.utils.svn;

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

import java.util.ArrayList;
import java.util.Collection;

/**
 * svn 
 * 
 */
public class SvnExternalsEntries
{
    //private HashMap<String, ExternalEntry> fromOrigin = new HashMap<String, ExternalEntry>();
    //private HashMap<String, ExternalEntry> fromTargetDir = new HashMap<String, ExternalEntry>();
    private ArrayList<SvnExternalEntry> ordered = new ArrayList<SvnExternalEntry>();
    
    public void put( SvnExternalEntry ee )
    {
        /*if ( fromOrigin.containsKey( ee.origin ) )
        {
            ExternalEntry eeo = fromOrigin.get( ee.origin );
            fromTargetDir.remove( eeo.targetDir );
            ordered.remove( ee );
        }
        if ( fromTargetDir.containsKey( ee.targetDir ) )
        {
            ExternalEntry eetd = fromTargetDir.get( ee.targetDir );
            fromOrigin.remove( eetd.origin );
            ordered.remove( ee );
        }*/
        
        //fromOrigin.put( ee.origin, ee );
        //fromTargetDir.put( ee.targetDir, ee );
        int place = ordered.indexOf( ee );
        if ( place != -1 )
        {
            ordered.set( place, ee );
        }
        else
        {
            ordered.add( ee );
        }
    }
    
    public Collection<SvnExternalEntry> values()
    {
        return ordered;
    }
}
