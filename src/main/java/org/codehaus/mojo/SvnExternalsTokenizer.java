package org.codehaus.mojo;

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
 
import org.codehaus.utils.Automate;
import org.codehaus.utils.AutomateException;
import org.codehaus.utils.Action;
import org.codehaus.utils.ActionExecutor;

import java.text.StringCharacterIterator;
import java.lang.Character;
import java.lang.StringBuilder;

// @formatter:off
/**
 * We implement this funny automate :
 * 
 * @author fbonin
 * 
 */
// @formatter:on
public class SvnExternalsTokenizer extends Automate
{

    /**
     * define automate labels
     */
    private enum MainLabel 
    {
        blank,
        backSlack,
        quote,
        minusChar,
        rChar,
        digit,
        other,
        empty;

        /**
         * TODO 
         * 
         * @param i
         * @return enum
         */
        public static MainLabel fromOrdinal( final int i )
        {
            if ( ( i < 0 ) || ( i >= MainLabel.values().length ) )
            {
                throw new IndexOutOfBoundsException( "Invalid ordinal" );
            }
            return MainLabel.values()[i];
        }
    };
    
    public enum TokenType 
    {
        empty,
        libelle,
        revision;
    };
    
    public class Token
    {      
        public TokenType tokenType = TokenType.empty;
        public Object value = null;
    };

    /**
     * define automate states
     */
    private enum MainState {
        INIT,
        LEX,
        LEXESC,
        QUOBEG,
        QUOEND,
        MREV,
        REV,
        PREV,
        FINAL_LIBELLE,
        FINAL_REV,
        FINAL_EMPTY;

        /**
         * 
         * @param i
         * @return enum
         */
        public static MainState fromOrdinal(final int i)
        {
            if ((i < 0) || (i >= MainState.values().length))
            {
                throw new IndexOutOfBoundsException("Invalid ordinal");
            }
            return MainState.values()[i];
        }
    };

    /**
     * Automate definition
     */
    static {
        Action accumulate = new Action()
        {
            public void action(final ActionExecutor executor, final Object aiParam)
                throws AutomateException
            {
                ( ( SvnExternalsTokenizer ) executor).accumulate(aiParam);
            }

            public String getName()
            {
                return "accumulate";
            }
        };
        
        Action drop = new Action()
        {
            public void action(final ActionExecutor executor, final Object aiParam)
                throws AutomateException
            {
                ( ( SvnExternalsTokenizer ) executor).drop(aiParam);
            }

            public String getName()
            {
                return "drop";
            }
        };
        
        Action nothing = new Action()
        {
            public void action(final ActionExecutor executor, final Object aiParam)
                throws AutomateException
            {
            }

            public String getName()
            {
                return "nothing";
            }
        };
        
        Action tokenTypeLibelle = new Action()
        {
            public void action(final ActionExecutor executor, final Object aiParam)
                throws AutomateException
            {
                ( ( SvnExternalsTokenizer ) executor).currentToken.tokenType = TokenType.libelle;
            }

            public String getName()
            {
                return "tokenTypeLibelle";
            }
        };
        
        Action tokenTypeRevision = new Action()
        {
            public void action(final ActionExecutor executor, final Object aiParam)
                throws AutomateException
            {
                ( ( SvnExternalsTokenizer ) executor).currentToken.tokenType = TokenType.revision;
            }

            public String getName()
            {
                return "tokenTypeRevision";
            }
        };

        try
        {
            Automate.initializeAutomate(MainState.values().length, MainLabel.values().length);
            
            // override defaut transitions to handle all "other" char with state "default" case
            Automate.setState( MainState.INIT.ordinal(), null, MainState.LEX.ordinal(), accumulate );
            Automate.setTransition(MainState.INIT.ordinal(), MainLabel.blank.ordinal(),
                MainState.INIT.ordinal(), drop);
            Automate.setTransition(MainState.INIT.ordinal(), MainLabel.quote.ordinal(),
                MainState.QUOBEG.ordinal(), accumulate);
            Automate.setTransition(MainState.INIT.ordinal(), MainLabel.minusChar.ordinal(),
                MainState.MREV.ordinal(), accumulate);
            Automate.setTransition(MainState.INIT.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_EMPTY.ordinal(), nothing);
                
            // override defaut transitions to handle all "other" char with state "default" case
            Automate.setState( MainState.QUOBEG.ordinal(), null, MainState.QUOBEG.ordinal(), accumulate );
            Automate.setTransition(MainState.QUOBEG.ordinal(), MainLabel.quote.ordinal(),
                MainState.QUOEND.ordinal(), accumulate);                                  
            Automate.setTransition(MainState.QUOBEG.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing);

            // override defaut transitions to handle all "other" char with state "default" case
            Automate.setState( MainState.QUOEND.ordinal(), null, MainState.QUOBEG.ordinal(), accumulate );
            Automate.setTransition(MainState.QUOEND.ordinal(), MainLabel.blank.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), drop);
            Automate.setTransition(MainState.QUOEND.ordinal(), MainLabel.quote.ordinal(),
                MainState.QUOEND.ordinal(), accumulate);                               
            Automate.setTransition(MainState.QUOEND.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing);
                
            // override defaut transitions to handle all "other" char with state "default" case
            Automate.setState( MainState.LEX.ordinal(), null, MainState.LEX.ordinal(), accumulate );                
            Automate.setTransition(MainState.LEX.ordinal(), MainLabel.blank.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), drop);
            Automate.setTransition(MainState.LEX.ordinal(), MainLabel.backSlack.ordinal(),
                MainState.LEXESC.ordinal(), accumulate);                                
            Automate.setTransition(MainState.LEX.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing);

            // override defaut transitions to handle all "other" char with state "default" case
            Automate.setState( MainState.LEXESC.ordinal(), null, MainState.LEX.ordinal(), accumulate );                 
            Automate.setTransition(MainState.LEXESC.ordinal(), MainLabel.backSlack.ordinal(),
                MainState.LEXESC.ordinal(), accumulate);                                  
            Automate.setTransition(MainState.LEXESC.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing);
                
            // override defaut transitions to handle all "other" char with state "default" case
            Automate.setState( MainState.MREV.ordinal(), null, MainState.LEX.ordinal(), accumulate );     
            Automate.setTransition(MainState.MREV.ordinal(), MainLabel.rChar.ordinal(),
                MainState.REV.ordinal(), accumulate);
            Automate.setTransition(MainState.MREV.ordinal(), MainLabel.blank.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), drop);
            Automate.setTransition(MainState.MREV.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing);
                
            // override defaut transitions to handle all "other" char with state "default" case
            Automate.setState( MainState.REV.ordinal(), null, MainState.LEX.ordinal(), accumulate );     
            Automate.setTransition(MainState.REV.ordinal(), MainLabel.digit.ordinal(),
                MainState.PREV.ordinal(), accumulate);
            Automate.setTransition(MainState.REV.ordinal(), MainLabel.blank.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), drop);
            Automate.setTransition(MainState.REV.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing);
                
            // override defaut transitions to handle all "other" char with state "default" case
            Automate.setState( MainState.PREV.ordinal(), null, MainState.LEX.ordinal(), accumulate );     
            Automate.setTransition(MainState.PREV.ordinal(), MainLabel.digit.ordinal(),
                MainState.PREV.ordinal(), accumulate);
            Automate.setTransition(MainState.PREV.ordinal(), MainLabel.blank.ordinal(),
                MainState.FINAL_REV.ordinal(), drop);
            Automate.setTransition(MainState.PREV.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_REV.ordinal(), nothing);
                              
            // not needed : empty is defaut token type
            //Automate.setState( MainState.FINAL_EMPTY.ordinal(), tokenTypeEmpty);
            Automate.setState( MainState.FINAL_LIBELLE.ordinal(), tokenTypeLibelle);
            Automate.setState( MainState.FINAL_REV.ordinal(), tokenTypeRevision);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private StringCharacterIterator iterator = null;
    private Token currentToken = new Token();
    
    /**
     * Default constructor enter in initial state executor is
     * SvnExternalsTokenizer itself
     * 
     * @throws AutomateException
     */
    public SvnExternalsTokenizer(StringCharacterIterator iterator) throws AutomateException
    {
        super(MainState.INIT.ordinal(), null);
        this.iterator = iterator;
    }
     
    public Token nextToken() throws AutomateException
    {
        Token ret = currentToken;
        do
        {
            char current = iterator.current();
            int etiquette = current == StringCharacterIterator.DONE ? MainLabel.empty.ordinal() :
                current == '-' ? MainLabel.minusChar.ordinal() :
                current == 'r' ? MainLabel.rChar.ordinal() :
                current == '\\' ? MainLabel.backSlack.ordinal() :
                current == '"' ? MainLabel.quote.ordinal() : 
                Character.isWhitespace(current) ? MainLabel.blank.ordinal() :
                Character.isDigit(current) ? MainLabel.digit.ordinal() :
                MainLabel.other.ordinal();
                    
            //System.out.println("nextToken call event with : " + etiquette + "," + current);
            event(etiquette, new Character(current) ); //  iterator may change here !
        }
        while ( ! isInFinalState() );
        
        initializer(MainState.INIT.ordinal(), null);
        currentToken = new Token();
        
        return ret;
    }

    @Override
    public String getName()
    {
        return "SvnExternalsTokenizer";
    }

    /**
     * @param s
     * 
     */
    public void accumulate(final Object s)
    {
        if (currentToken.value == null)
        {
            currentToken.value = new StringBuilder();
        }
        if ( currentToken.value instanceof StringBuilder)
        {
            //System.out.println("accumulate : " + iterator.current() );
            ( (StringBuilder) currentToken.value ).append( iterator.current() );
        }
        char t = iterator.next();
        //System.out.println("accumulate next is : " + iterator.current() );
    }
    
    /**
     * @param s
     * 
     */
    public void drop(final Object s)
    {
        iterator.next();
        //System.out.println("drop next is : " + iterator.current() );
    }

    @Override
    protected boolean isInFinalState()
    {
        return getCurrentState() == MainState.FINAL_LIBELLE.ordinal() ||
               getCurrentState() == MainState.FINAL_REV.ordinal() ||
               getCurrentState() == MainState.FINAL_EMPTY.ordinal();
    }

    @Override
    protected void notifyCurrentStateChanged()
    {
        //System.out.println("State changed to : " + MainState.fromOrdinal(getCurrentState()));
    }
};
