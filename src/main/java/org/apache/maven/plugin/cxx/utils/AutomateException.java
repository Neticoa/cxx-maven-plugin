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

/**
 * Thrown when a particular action cannot be found.
 * 
 * @author FBN
 */
@SuppressWarnings( "serial" )
public class AutomateException extends Exception
{
    /**
     * Constructs a <code>ActionNotFoundException</code> without a detail message.
     */
    public AutomateException()
    {
        super();
    }

    /**
     * Constructs a <code>ActionNotFoundException</code> with a detail message.
     * 
     * @param message the detail message.
     */
    public AutomateException( final String message )
    {
        super( message );
    }
}
