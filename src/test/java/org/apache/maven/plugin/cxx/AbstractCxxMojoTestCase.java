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

public abstract class AbstractCxxMojoTestCase
    extends AbstractMojoTestCase
{
    protected File testBasedir = null;
    
    protected String platformEol = "\n";
    
    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        // required
        super.setUp();
        
        platformEol = System.getProperty("line.separator");
        
        testBasedir = new File( getBasedir(), "target/test/unit/" + testName() );
        testBasedir.mkdirs();
    }
    
    protected abstract String testName();

    /** {@inheritDoc} */
    protected void tearDown()
        throws Exception
    {
        // required
        super.tearDown();
    }
    
    public void assertFileExists( String filename, boolean exist )
    {
        File file = new File( filename );
        assertEquals( exist, file.exists() );
    }
}
