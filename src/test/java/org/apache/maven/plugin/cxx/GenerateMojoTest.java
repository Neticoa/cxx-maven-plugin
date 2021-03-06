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
import org.apache.maven.execution.MavenSession;

public class GenerateMojoTest
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
        return "generate-test";
    }

    /**
     * @throws Exception if any
     */
    public void testSomething()
        throws Exception
    {
        File pom = getTestFile( "src/test/resources/unit/generate/cmake-cpp-project.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        GenerateMojo mojo = (GenerateMojo) lookupMojo( "generate", pom );
        assertNotNull( mojo );
        
        MavenSession mavenSession =
            new MavenSession( null, null, null, null, null, null, null, System.getProperties(), null );
        setVariableValueToObject( mojo, "session", mavenSession );
        
        mojo.execute();
        
        assertFileExists( testBasedir + "/pom.xml", true);

    }
}
