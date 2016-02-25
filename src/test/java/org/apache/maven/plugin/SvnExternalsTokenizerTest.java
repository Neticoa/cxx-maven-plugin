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
import org.codehaus.mojo.SvnExternalsTokenizer;
import java.text.StringCharacterIterator;

public class SvnExternalsTokenizerTest
    extends AbstractMojoTestCase
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

    /**
     * @throws Exception if any
     */
    public void testSomething()
        throws Exception
    {
        String s = "r455 dfg sdfgsd ";
        
        StringCharacterIterator iter = new StringCharacterIterator(s);
        SvnExternalsTokenizer m = new SvnExternalsTokenizer(iter);

        SvnExternalsTokenizer.Token t1 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t1.tokenType );
        assertEquals( "r455", t1.value.toString() );
        SvnExternalsTokenizer.Token t2 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t2.tokenType );
        assertEquals( "dfg", t2.value.toString() );
        SvnExternalsTokenizer.Token t3 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t3.tokenType );
        assertEquals( "sdfgsd", t3.value.toString() );
        SvnExternalsTokenizer.Token t4 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t4.tokenType );
        assertEquals( null, t4.value );
        SvnExternalsTokenizer.Token t5 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t5.tokenType );
        assertEquals( null, t5.value );
        
    }
}

