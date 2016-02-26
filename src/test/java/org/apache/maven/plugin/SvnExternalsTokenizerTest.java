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
    public void test1() throws Exception
    {
        String s = "-r455 source target ";
        
        StringCharacterIterator iter = new StringCharacterIterator(s);
        SvnExternalsTokenizer m = new SvnExternalsTokenizer(iter);

        SvnExternalsTokenizer.Token t1 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.revision, t1.tokenType );
        assertEquals( "-r455", t1.value.toString() );
        SvnExternalsTokenizer.Token t2 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t2.tokenType );
        assertEquals( "source", t2.value.toString() );
        SvnExternalsTokenizer.Token t3 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t3.tokenType );
        assertEquals( "target", t3.value.toString() );
        SvnExternalsTokenizer.Token t4 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t4.tokenType );
        assertEquals( null, t4.value );
        SvnExternalsTokenizer.Token t5 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t5.tokenType );
        assertEquals( null, t5.value );
        
    }
    
    /**
     * @throws Exception if any
     */
    public void test2() throws Exception
    {
        String s = "-r2 \"^/source2\" target    ";
        
        StringCharacterIterator iter = new StringCharacterIterator(s);
        SvnExternalsTokenizer m = new SvnExternalsTokenizer(iter);

        SvnExternalsTokenizer.Token t1 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.revision, t1.tokenType );
        assertEquals( "-r2", t1.value.toString() );
        SvnExternalsTokenizer.Token t2 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t2.tokenType );
        assertEquals( "\"^/source2\"", t2.value.toString() );
        SvnExternalsTokenizer.Token t3 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t3.tokenType );
        assertEquals( "target", t3.value.toString() );
        SvnExternalsTokenizer.Token t4 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t4.tokenType );
        assertEquals( null, t4.value );
        SvnExternalsTokenizer.Token t5 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t5.tokenType );
        assertEquals( null, t5.value );
        
    }
    
    /**
     * @throws Exception if any
     */
    public void test3() throws Exception
    {
        String s = " \t-r223 ^/source\\ avec\\ espace target2    ";
        
        StringCharacterIterator iter = new StringCharacterIterator(s);
        SvnExternalsTokenizer m = new SvnExternalsTokenizer(iter);

        SvnExternalsTokenizer.Token t1 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.revision, t1.tokenType );
        assertEquals( "-r223", t1.value.toString() );
        SvnExternalsTokenizer.Token t2 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t2.tokenType );
        assertEquals( "^/source\\ avec\\ espace", t2.value.toString() );
        SvnExternalsTokenizer.Token t3 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t3.tokenType );
        assertEquals( "target2", t3.value.toString() );
        SvnExternalsTokenizer.Token t4 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t4.tokenType );
        assertEquals( null, t4.value );
        SvnExternalsTokenizer.Token t5 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t5.tokenType );
        assertEquals( null, t5.value );
        
    }
    
    
    /**
     * @throws Exception if any
     */
    public void test4() throws Exception
    {
        String s = " \t-r2230 \"^/source\\ avec\\ espace et quote\"   target3    ";
        
        StringCharacterIterator iter = new StringCharacterIterator(s);
        SvnExternalsTokenizer m = new SvnExternalsTokenizer(iter);

        SvnExternalsTokenizer.Token t1 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.revision, t1.tokenType );
        assertEquals( "-r2230", t1.value.toString() );
        SvnExternalsTokenizer.Token t2 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t2.tokenType );
        assertEquals( "\"^/source\\ avec\\ espace et quote\"", t2.value.toString() );
        SvnExternalsTokenizer.Token t3 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t3.tokenType );
        assertEquals( "target3", t3.value.toString() );
        SvnExternalsTokenizer.Token t4 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t4.tokenType );
        assertEquals( null, t4.value );
        SvnExternalsTokenizer.Token t5 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t5.tokenType );
        assertEquals( null, t5.value );
        
    }
    
    /**
     * @throws Exception if any
     */
    public void test5() throws Exception
    {
        String s = " \r\n http://test.server/trunk/ici\\ etLa -r2230bad   -r0000         -";
        
        StringCharacterIterator iter = new StringCharacterIterator(s);
        SvnExternalsTokenizer m = new SvnExternalsTokenizer(iter);

        SvnExternalsTokenizer.Token t1 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t1.tokenType );
        assertEquals( "http://test.server/trunk/ici\\ etLa", t1.value.toString() );
        SvnExternalsTokenizer.Token t2 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t2.tokenType );
        assertEquals( "-r2230bad", t2.value.toString() );
        SvnExternalsTokenizer.Token t3 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.revision, t3.tokenType );
        assertEquals( "-r0000", t3.value.toString() );
        SvnExternalsTokenizer.Token t4 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t4.tokenType );
        assertEquals( "-", t4.value.toString() );
        SvnExternalsTokenizer.Token t5 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.empty, t5.tokenType );
        assertEquals( null, t5.value );
        
    }
    
    /**
     * @throws Exception if any
     */
    public void test6() throws Exception
    {
        String s = "-r";
        
        StringCharacterIterator iter = new StringCharacterIterator(s);
        SvnExternalsTokenizer m = new SvnExternalsTokenizer(iter);

        SvnExternalsTokenizer.Token t1 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t1.tokenType );
        assertEquals( "-r", t1.value.toString() );

    }
    
    /**
     * @throws Exception if any
     */
    public void test7() throws Exception
    {
        String s = "-r ";
        
        StringCharacterIterator iter = new StringCharacterIterator(s);
        SvnExternalsTokenizer m = new SvnExternalsTokenizer(iter);

        SvnExternalsTokenizer.Token t1 = m.nextToken();
        assertEquals( SvnExternalsTokenizer.TokenType.libelle, t1.tokenType );
        assertEquals( "-r", t1.value.toString() );

    }
    
}

