package org.apache.maven.plugin.cxx.utils.release;

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

import java.util.List;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.plugin.logging.Log;

/*
import java.util.List;*/
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * Base class for all phases.
 *
 * @author 
 */
public abstract class CxxAbstractReleasePhase
    implements CxxReleasePhase
{
    protected ResourceBundle getResourceBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "cxx-release-messages", locale,
            CxxAbstractReleasePhase.class.getClassLoader() );
    }
    
    private Log log = null;
    
    protected Log getLog()
    {
        return log;
    }
    
    public void setLog( Log log )
    {
        this.log = log;
    }
    
    public void setLogger( String name )
    {
        this.log = new org.apache.maven.monitor.logging.DefaultLog( 
            new org.codehaus.plexus.logging.console.ConsoleLogger( 
                org.codehaus.plexus.logging.console.ConsoleLogger.LEVEL_INFO, name ) );
    }
    
    public ReleaseResult clean( List<MavenProject> reactorProjects )
    {
        // nothing to do by default

        return getReleaseResultSuccess();
    }

    protected void logInfo( ReleaseResult result, String message )
    {
        result.appendInfo( message );
        log.info( "  " + message );
    }

    protected void logWarn( ReleaseResult result, String message )
    {
        result.appendWarn( message );
        log.warn( "  " + message );
    }

    protected void logError( ReleaseResult result, String message )
    {
        result.appendWarn( message );
        log.error( "  " + message );
    }

    protected void logDebug( ReleaseResult result, String message )
    {
        result.appendDebug( message );
        log.debug( "  " + message );
    }

    protected void logDebug( ReleaseResult result, String message, Exception e )
    {
        result.appendDebug( message, e );
        log.debug( "  " + message, e );
    }
    
    protected ReleaseResult getReleaseResultSuccess()
    {
        ReleaseResult result = new ReleaseResult();

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }
}

