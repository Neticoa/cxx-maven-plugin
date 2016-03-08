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
 
import org.apache.maven.plugin.cxx.utils.Credential;
import org.apache.maven.plugin.cxx.utils.ExecutorService;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.FileOutputStream;

import java.util.Properties;

import java.text.StringCharacterIterator;
 
import org.apache.commons.exec.CommandLine;

import org.apache.commons.lang.StringUtils;
 
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import org.codehaus.plexus.util.IOUtil;

/**
 * TODO
 * 
 * @version 8 mars 2016
 * @author fbonin
 */
public class SvnService
{
    public static void execSvnCommand( File basedir, Credential cred, String[] specificParams,
        OutputStream out, Log log ) throws MojoExecutionException
    {
        Properties enviro = null;
        try
        {
            enviro = ExecutorService.getSystemEnvVars();
            enviro.put( "LC_MESSAGES", "C" );
        }
        catch ( IOException e )
        {
            log.error( "Could not assign default system environment variables.", e );
        }
        
        CommandLine commandLine = ExecutorService.getExecutablePath( "svn", enviro, basedir );
        
        if ( cred != null && ! StringUtils.isEmpty( cred.getUsername() ) )
        {
            commandLine.addArguments( new String[] { "--username", cred.getUsername() } );
        }
        if ( cred != null && ! StringUtils.isEmpty( cred.getPassword() ) )
        {
            commandLine.addArguments( new String[] { "--password", cred.getPassword() } );
        }
        /* TODO later :
        commandLine.addArguments( new String[] {"--config-dir", "todo" } );
        commandLine.addArgument( "--no-auth-cache" ); 
        commandLine.addArgument( "--non-interactive" );
        commandLine.addArgument( "--trust-server-cert" );
        */

        commandLine.addArguments( specificParams );
        
        Executor exec = new DefaultExecutor();
        
        try
        {
            log.debug( "Execute command '" + commandLine + "'" );
            int resultCode = ExecutorService.executeCommandLine( exec, commandLine, enviro, out,
                System.err, System.in );
        }
        catch ( ExecuteException e )
        {
            throw new MojoExecutionException( "Command '" + commandLine + "' execution failed.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Command '" + commandLine + "' execution failed.", e );
        }
    }
    
    public static SvnInfo getSvnInfo( File basedir, Credential cred, String uri, Log log ) throws MojoExecutionException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        execSvnCommand( basedir, cred, new String[] {"info", uri, "--xml"}, out, log );
        
        SvnInfo svnInfo = new SvnInfo();
        try
        {
            SAXParserFactory sfactory = SAXParserFactory.newInstance();
            SAXParser parser = sfactory.newSAXParser();
            XMLReader xmlparser = parser.getXMLReader();
            xmlparser.setContentHandler( svnInfo );
            xmlparser.parse( new InputSource( new ByteArrayInputStream( out.toByteArray( ) ) ) );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "svn info xml parsing failed.", e );
        }        
        return svnInfo;
    }
    
    public static SvnExternalsEntries loadSvnExternals( File basedir, Credential cred, String uri, Log log )
        throws MojoExecutionException
    {
        SvnExternalsEntries properties = new SvnExternalsEntries();
      
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        SvnService.execSvnCommand( basedir, cred,
            new String[] {"propget", "svn:externals", uri}, out, log );
        
        BufferedReader in = new BufferedReader( new StringReader( out.toString( ) ) );
        
        String line = null;
        
        try
        {
            /* old svn 1.4 :
              third-party/sounds             http://svn.example.com/repos/sounds
              third-party/skins -r148        http://svn.example.com/skinproj
              third-party/skins/toolkit -r21 http://svn.example.com/skin-maker
            */
            /* svn 1.5 and above (operative rev):
              http://svn.example.com/repos/sounds third-party/sounds
              -r148 http://svn.example.com/skinproj third-party/skins
              -r21  http://svn.example.com/skin-maker third-party/skins/toolkit
              svn 1.5 and above (PEG rev):
              http://svn.example.com/repos/sounds third-party/sounds
              http://svn.example.com/skinproj@148 third-party/skins
              http://svn.example.com/skin-maker@21 third-party/skins/toolkit
            */
            
            // this parser only support svn:externals syntaxe 1.5 and above
            // [revision] origin target_dir
            while ( null != ( line = in.readLine() ) )
            {
                StringCharacterIterator iter = new StringCharacterIterator( line );
                SvnExternalsTokenizer m = new SvnExternalsTokenizer( iter );
                SvnExternalEntry external = new SvnExternalEntry();

                SvnExternalsTokenizer.Token tok = m.nextToken();
                if ( SvnExternalsTokenizer.TokenType.revision == tok.tokenType )
                {
                    external.revision = tok.value.toString();
                    
                    tok = m.nextToken();
                    external.origin = SvnExternalsTokenizer.TokenType.libelle == tok.tokenType
                        ? tok.value.toString() : null; 
                    
                    tok = m.nextToken();
                    external.targetDir = SvnExternalsTokenizer.TokenType.libelle == tok.tokenType
                        ? tok.value.toString() : null; 
                }
                else if ( SvnExternalsTokenizer.TokenType.libelle == tok.tokenType )
                {
                    external.origin = tok.value.toString();
                    
                    tok = m.nextToken();
                    external.targetDir = SvnExternalsTokenizer.TokenType.libelle == tok.tokenType
                        ? tok.value.toString() : null; 
                }
                else if ( SvnExternalsTokenizer.TokenType.comment == tok.tokenType )
                {
                    external.comment = tok.value.toString();
                }
                
                if ( external.isValide() )
                {
                    properties.put( external );
                }
                else
                {
                    log.warn( "unrecognized svn:externals entry of " + uri + " line content : '" + line + "'" );
                }
            }
        }
        catch ( Exception e )
        {
            log.warn( "Failed to parse svn:externals of " + uri + " : " + e );
        }
        
        return properties;
    }
    
    public static void writeSvnExternals( File basedir, Credential cred, String uri,
        SvnExternalsEntries properties, String tempDir, Log log ) throws MojoExecutionException
    {   
        String targetDirectoryPath = tempDir;
        new File( targetDirectoryPath ).mkdirs();
        File file = new File( targetDirectoryPath, "svn.externals" );

        OutputStream outputStream = null;
        try
        {
            outputStream = new FileOutputStream( file );

            for ( SvnExternalEntry entry : properties.values() )
            {
                if ( entry.isValide() )
                {
                    outputStream.write( ( entry.toString() + "\n" ).getBytes() );
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error writing intermediate properties file "
                + file.toString() + " : " + e );
        }
        finally
        {
            IOUtil.close( outputStream );
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SvnService.execSvnCommand( basedir, cred,
            new String[] {"propset", "svn:externals", "--file", file.getPath(), uri}, out, log );
    }
    
}
