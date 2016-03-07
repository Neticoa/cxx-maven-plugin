package org.apache.maven.plugin.cxx;

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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.codehaus.plexus.util.FileUtils;
import org.apache.commons.lang.StringUtils;

public class LaunchMojoTest
    extends AbstractCxxMojoTestCase
{
    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        // required
        super.setUp();
        
    }

    /** {@inheritDoc} */
    protected void tearDown()
        throws Exception
    {
        // required
        super.tearDown();
    }
    
    @Override    
    protected String testName()
    {
        return "launch-test";
    }

    /**
     * @throws Exception if any
     */
    public void testHelloWorld()
        throws Exception
    {
        File pom = getTestFile( "src/test/resources/unit/launch/HelloWorld.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        LaunchMojo mojo = (LaunchMojo) lookupMojo( "launch", pom );
        assertNotNull( mojo );
        
        mojo.execute();
        assertFileExists( testBasedir + "/launch-test.txt", true);
        
        String sExecutionDate = new SimpleDateFormat( "yyyy-MM-dd-HH:mm:ss.SSS" ).format( new Date() );
        FileUtils.fileAppend( testBasedir + "/launch-test.txt", sExecutionDate );
        
        assertEquals( "hello world!" + platformEol + sExecutionDate, 
            FileUtils.fileRead( testBasedir + "/launch-test.txt", "UTF8" ) );
    }
    
    /**
     * @throws Exception if any
     */
    public void testConfigList()
        throws Exception
    {
        String sOsName = System.getProperty( "os.name" );
        sOsName = sOsName.replace( " ", "" );
        
        File pom = getTestFile( StringUtils.contains( "Windows", sOsName ) 
            ? "src/test/resources/unit/launch/ConfigList-Windows.xml"
            : "src/test/resources/unit/launch/ConfigList.xml");
        assertNotNull( pom );
        assertTrue( pom.exists() );

        LaunchMojo mojo = (LaunchMojo) lookupMojo( "launch", pom );
        assertNotNull( mojo );
        
        mojo.execute();
        assertFileExists( testBasedir + "/launch-test-2.txt", true);
        
        String sExecutionDate = new SimpleDateFormat( "yyyy-MM-dd-HH:mm:ss.SSS" ).format( new Date() );
        FileUtils.fileAppend( testBasedir + "/launch-test-2.txt", sExecutionDate );
        
        assertEquals( "configlist hello world!" + platformEol + sExecutionDate, 
            FileUtils.fileRead( testBasedir + "/launch-test-2.txt", "UTF8" ) );
    }
}
