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

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import java.util.regex.Pattern;

/**
 * svn info <uri> request result
 * 
 */
public class SvnInfo extends DefaultHandler
{
    private String url = null;
    private String root = null;
    private String relativeUrl = null;
    private long revision = -1;
    
    public void reset()
    {
        url = null;
        root = null;
        relativeUrl = null;
        revision = -1;
    }
    
    public boolean isValide()
    {
        return url != null && root != null && relativeUrl != null && revision != -1;
    }
    
    public String getSvnUrl()
    {
        return url;
    }
    
    public String getSvnRoot()
    {
        return root;
    }
    
    public String getSvnRelativeUrl()
    {
        return relativeUrl;
    }
     
    public long getRevision()
    {
        return revision;
    }
    
    public String toString()
    {
       return "r" + revision + " " + root + " " + relativeUrl;
    }
    
    private boolean bUrl = false;
    private boolean bRoot = false;
    private boolean bRelativeUrl = false;
    
    @Override
    public void startElement( String uri, String localName, String qName, Attributes attributes )
        throws SAXException
    {
        if ( qName.equalsIgnoreCase( "entry" ) )
        {
            String sRev = attributes.getValue( "revision" );
            try
            {
                revision = Long.parseLong( sRev );
            }
            catch ( Exception e )
            {
                throw new SAXException( "URI or folder svn revision not found", e );
            } 
        }
        else if ( qName.equalsIgnoreCase( "url" ) )
        {
            bUrl = true;
        }
        else if ( qName.equalsIgnoreCase( "root" ) )
        {
            bRoot = true;
        }
        else if ( qName.equalsIgnoreCase( "relative-url" ) )
        {
            bRelativeUrl = true;
        }
    }
    
    @Override
    public void characters( char ch[], int start, int length ) throws SAXException
    {
        if ( bUrl )
        {
            url = new String( ch, start, length );
            bUrl = false;
        }
        else if ( bRoot )
        {
            root = new String( ch, start, length );
            bRoot = false;
        }
        else if ( bRelativeUrl )
        {
            relativeUrl = new String( ch, start, length );
            bRelativeUrl = false;
        }
    }
    
    @Override
    public void endDocument() throws SAXException
    {
        if ( relativeUrl == null && url != null && root != null )
        {
            relativeUrl = url.replaceFirst( Pattern.quote( root ), "^" );
        }
    }
}
