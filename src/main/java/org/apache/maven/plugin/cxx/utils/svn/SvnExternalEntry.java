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
 
import org.apache.commons.lang.StringUtils;

/**
 * svn 
 * 
 */
public class SvnExternalEntry
{
    public String revision = null;
    public String origin = null;
    public String targetDir = null;
    public String comment = null;
    
    public boolean isValide()
    {
        return ! StringUtils.isEmpty( comment )
            || ( ! StringUtils.isEmpty( origin ) && ! StringUtils.isEmpty( targetDir ) );
    }
    
    public String toString()
    {
        return "" + ( ( null != comment ) ? comment + " "
            : ( ( ( null != revision ) ? revision + " " : "" ) + ( ( null != origin ) ? origin : "" )
            + " " + ( ( null != targetDir ) ? targetDir : "" ) ) );
    }
    
    @Override
    public boolean equals( Object other )
    {
        if ( other == null )
        {
            return false;
        }
        if ( other == this )
        {
            return true;
        }
        if ( !( other instanceof SvnExternalEntry ) )
        {
            return false;
        }
        SvnExternalEntry otherExternalEntry = (SvnExternalEntry) other;
        // comments are unique, equals shall return false on them
        if ( null != comment || null != otherExternalEntry.comment )
        {
            return false;
        }
        else
        {
            return StringUtils.equals( targetDir, otherExternalEntry.targetDir );
        }
    }
    
    @Override        
    public int hashCode()
    {
        // comments are unique, hashCode shall be different for them
        return null != comment ? super.hashCode() : null != targetDir ? targetDir.hashCode() : 0;
    }
}
