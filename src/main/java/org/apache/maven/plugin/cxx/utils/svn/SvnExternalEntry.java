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

import org.codehaus.plexus.util.StringUtils;

/**
 * svn
 * 
 */
public class SvnExternalEntry
{
    private String revision = null;
    private String origin = null;
    private String targetDir = null;
    private String comment = null;

    public boolean isValide()
    {
        return !StringUtils.isEmpty( getComment() )
            || ( !StringUtils.isEmpty( getOrigin() ) && !StringUtils.isEmpty( getTargetDir() ) );
    }

    public String toString()
    {
        return "" + ( ( null != getComment() ) ? getComment()
            : ( ( ( null != getRevision() ) ? getRevision() + " " : "" )
                + ( ( null != getOrigin() ) ? getOrigin() : "" ) + " "
                + ( ( null != getTargetDir() ) ? getTargetDir() : "" ) ) );
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
        if ( null != getComment() || null != otherExternalEntry.getComment() )
        {
            return false;
        }
        // otherwise, entries are equals if their origin or tagetDir are equals
        return StringUtils.equals( getOrigin(), otherExternalEntry.getOrigin() )
            || StringUtils.equals( getTargetDir(), otherExternalEntry.getTargetDir() );
    }

    @Override
    public int hashCode()
    {
        return null != getComment() ? super.hashCode() // comments are unique, hashCode shall be different for them
            : null != getTargetDir()
                ? null != getOrigin() ? ( getOrigin() + getTargetDir() ).hashCode() : getTargetDir().hashCode()
                : null != getOrigin() ? getOrigin().hashCode() : 0;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    public String getTargetDir()
    {
        return targetDir;
    }

    public void setTargetDir( String targetDir )
    {
        this.targetDir = targetDir;
    }

    public String getOrigin()
    {
        return origin;
    }

    public void setOrigin( String origin )
    {
        this.origin = origin;
    }

    public String getRevision()
    {
        return revision;
    }

    public void setRevision( String revision )
    {
        this.revision = revision;
    }
}
