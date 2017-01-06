package org.apache.maven.plugin.cxx.utils;

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
import java.net.URL;
import java.net.MalformedURLException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

/**
 * TODO
 * 
 * @version 8 mars 2016
 * @author fbonin
 */
public final class Credential
{
    private String username;
    private String password;

    public Credential( String username, String password )
    {
        this.username = username;
        this.password = password;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }
    
    public void setUsername( String username )
    {
        this.username = username;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }
    
    /**
     * Load username password from settings if user has not set them in JVM properties
     * 
     * @see http://blog.sonatype.com/2009/10/maven-tips-and-tricks-encrypting-passwords
     * 
     * origin : derived from org.apache.maven.scm.plugin.AbstractScmMojo::loadInfosFromSettings()
     *
     * @param url not null
     */
    public static Credential createCredential( String url, /*ScmProviderRepositoryWithHost repo*/
        String username, String password, String settingsServerId, 
        Settings settings, SecDispatcher secDispatcher, Log log )
    {
        Credential ret = new Credential( username, password );
        if ( username == null || password == null )
        {
            String host = settingsServerId;
            if ( StringUtils.isEmpty( host ) )
            {
                URL aURL = null;
                try
                {
                    aURL = new URL( url );
                }
                catch ( MalformedURLException e )
                {
                    log.warn( "Failed to parse url '" + url
                        + "' while trying to find associated maven credentials info from maven settings" );
                    return ret;
                }

                host = /*repo*/aURL.getHost();

                int port = /*repo*/aURL.getPort();

                if ( port > 0 )
                {
                    host += ":" + port;
                }
            }

            Server server = settings.getServer( host );

            if ( server != null )
            {
                if ( username == null )
                {
                    //username = server.getUsername();
                    ret.setUsername( server.getUsername() );
                }

                if ( password == null )
                {
                    //password = decrypt( server.getPassword(), host );
                    ret.setPassword( decrypt( server.getPassword(), host, secDispatcher, log ) );
                }

                /*if ( privateKey == null )
                {
                    privateKey = server.getPrivateKey();
                }

                if ( passphrase == null )
                {
                    passphrase = decrypt( server.getPassphrase(), host );
                }*/
            }
        }
        return ret;
    }

    private static String decrypt( String str, String server, SecDispatcher secDispatcher, Log log )
    {
        try
        {
            return secDispatcher.decrypt( str );
        }
        catch ( SecDispatcherException e )
        {
            log.warn( "Failed to decrypt password/passphrase for server " + server + ", using auth token as is" );
            return str;
        }
    }
}
