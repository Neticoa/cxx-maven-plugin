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

import org.apache.maven.plugin.cxx.utils.Automate;
import org.apache.maven.plugin.cxx.utils.AutomateException;
import org.apache.maven.plugin.cxx.utils.Action;
import org.apache.maven.plugin.cxx.utils.ActionExecutor;

import java.text.StringCharacterIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger( SvnExternalsTokenizer.class );

    /**
     * define automate labels
     */
    private enum MainLabel
    {
        blank, backSlack, quote, minusChar, rChar, digit, diese, other, empty;

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
    }

    /**
     * token types
     */
    public enum TokenType
    {
        empty, libelle, revision, comment;
    }

    /**
     * a token descriptor
     */
    public class Token
    {
        private TokenType tokenType = TokenType.empty;
        private Object value = null;

        public TokenType getTokenType()
        {
            return tokenType;
        }

        public void setTokenType( TokenType tokenType )
        {
            this.tokenType = tokenType;
        }

        public Object getValue()
        {
            return value;
        }

        public void setValue( Object value )
        {
            this.value = value;
        }
    }

    /**
     * define automate states
     */
    private enum MainState
    {
        INIT, LEX, LEXESC, QUOBEG, QUOEND, MREV, REV, PREV, COMMENT, FINAL_LIBELLE, FINAL_REV, FINAL_EMPTY,
        FINAL_COMMENT;

        /**
         * 
         * @param i
         * @return enum
         */
        public static MainState fromOrdinal( final int i )
        {
            if ( ( i < 0 ) || ( i >= MainState.values().length ) )
            {
                throw new IndexOutOfBoundsException( "Invalid ordinal" );
            }
            return MainState.values()[i];
        }
    }

    /**
     * Automate definition
     */
    static
    {
        Action accumulate = new Action()
        {
            public void action( final ActionExecutor executor, final Object aiParam ) throws AutomateException
            {
                ( (SvnExternalsTokenizer) executor ).accumulate( aiParam );
            }

            public String getName()
            {
                return "accumulate";
            }
        };

        Action drop = new Action()
        {
            public void action( final ActionExecutor executor, final Object aiParam ) throws AutomateException
            {
                ( (SvnExternalsTokenizer) executor ).drop( aiParam );
            }

            public String getName()
            {
                return "drop";
            }
        };

        Action nothing = new Action()
        {
            public void action( final ActionExecutor executor, final Object aiParam ) throws AutomateException
            {
            }

            public String getName()
            {
                return "nothing";
            }
        };

        Action tokenTypeLibelle = new Action()
        {
            public void action( final ActionExecutor executor, final Object aiParam ) throws AutomateException
            {
                ( (SvnExternalsTokenizer) executor ).currentToken.setTokenType( TokenType.libelle );
            }

            public String getName()
            {
                return "tokenTypeLibelle";
            }
        };

        Action tokenTypeRevision = new Action()
        {
            public void action( final ActionExecutor executor, final Object aiParam ) throws AutomateException
            {
                ( (SvnExternalsTokenizer) executor ).currentToken.setTokenType( TokenType.revision );
            }

            public String getName()
            {
                return "tokenTypeRevision";
            }
        };

        Action tokenTypeComment = new Action()
        {
            public void action( final ActionExecutor executor, final Object aiParam ) throws AutomateException
            {
                ( (SvnExternalsTokenizer) executor ).currentToken.setTokenType( TokenType.comment );
            }

            public String getName()
            {
                return "tokenTypeComment";
            }
        };

        try
        {
            Automate.initializeAutomate( MainState.values().length, MainLabel.values().length );

            // override defaut transitions to handle all "other" char with state "default"
            // case
            Automate.setState( MainState.INIT.ordinal(), null, MainState.LEX.ordinal(), accumulate );
            Automate.setTransition( MainState.INIT.ordinal(), MainLabel.blank.ordinal(), MainState.INIT.ordinal(),
                drop );
            Automate.setTransition( MainState.INIT.ordinal(), MainLabel.quote.ordinal(), MainState.QUOBEG.ordinal(),
                accumulate );
            Automate.setTransition( MainState.INIT.ordinal(), MainLabel.minusChar.ordinal(), MainState.MREV.ordinal(),
                accumulate );
            Automate.setTransition( MainState.INIT.ordinal(), MainLabel.diese.ordinal(), MainState.COMMENT.ordinal(),
                accumulate );
            Automate.setTransition( MainState.INIT.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_EMPTY.ordinal(), nothing );

            // override defaut transitions to handle all "other" char with state "default"
            // case
            Automate.setState( MainState.QUOBEG.ordinal(), null, MainState.QUOBEG.ordinal(), accumulate );
            Automate.setTransition( MainState.QUOBEG.ordinal(), MainLabel.quote.ordinal(), MainState.QUOEND.ordinal(),
                accumulate );
            Automate.setTransition( MainState.QUOBEG.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing );

            // override defaut transitions to handle all "other" char with state "default"
            // case
            Automate.setState( MainState.QUOEND.ordinal(), null, MainState.QUOBEG.ordinal(), accumulate );
            Automate.setTransition( MainState.QUOEND.ordinal(), MainLabel.blank.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), drop );
            Automate.setTransition( MainState.QUOEND.ordinal(), MainLabel.quote.ordinal(), MainState.QUOEND.ordinal(),
                accumulate );
            Automate.setTransition( MainState.QUOEND.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing );

            // override defaut transitions to handle all "other" char with state "default"
            // case
            Automate.setState( MainState.LEX.ordinal(), null, MainState.LEX.ordinal(), accumulate );
            Automate.setTransition( MainState.LEX.ordinal(), MainLabel.blank.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), drop );
            Automate.setTransition( MainState.LEX.ordinal(), MainLabel.backSlack.ordinal(), MainState.LEXESC.ordinal(),
                accumulate );
            Automate.setTransition( MainState.LEX.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing );

            // override defaut transitions to handle all "other" char with state "default"
            // case
            Automate.setState( MainState.LEXESC.ordinal(), null, MainState.LEX.ordinal(), accumulate );
            Automate.setTransition( MainState.LEXESC.ordinal(), MainLabel.backSlack.ordinal(),
                MainState.LEXESC.ordinal(), accumulate );
            Automate.setTransition( MainState.LEXESC.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing );

            // override defaut transitions to handle all "other" char with state "default"
            // case
            Automate.setState( MainState.MREV.ordinal(), null, MainState.LEX.ordinal(), accumulate );
            Automate.setTransition( MainState.MREV.ordinal(), MainLabel.rChar.ordinal(), MainState.REV.ordinal(),
                accumulate );
            Automate.setTransition( MainState.MREV.ordinal(), MainLabel.blank.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), drop );
            Automate.setTransition( MainState.MREV.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing );

            // override defaut transitions to handle all "other" char with state "default"
            // case
            Automate.setState( MainState.REV.ordinal(), null, MainState.LEX.ordinal(), accumulate );
            Automate.setTransition( MainState.REV.ordinal(), MainLabel.digit.ordinal(), MainState.PREV.ordinal(),
                accumulate );
            Automate.setTransition( MainState.REV.ordinal(), MainLabel.blank.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), drop );
            Automate.setTransition( MainState.REV.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_LIBELLE.ordinal(), nothing );

            // override defaut transitions to handle all "other" char with state "default"
            // case
            Automate.setState( MainState.PREV.ordinal(), null, MainState.LEX.ordinal(), accumulate );
            Automate.setTransition( MainState.PREV.ordinal(), MainLabel.digit.ordinal(), MainState.PREV.ordinal(),
                accumulate );
            Automate.setTransition( MainState.PREV.ordinal(), MainLabel.blank.ordinal(), MainState.FINAL_REV.ordinal(),
                drop );
            Automate.setTransition( MainState.PREV.ordinal(), MainLabel.empty.ordinal(), MainState.FINAL_REV.ordinal(),
                nothing );

            // override defaut transitions to handle all "other" char with state "default"
            // case
            Automate.setState( MainState.COMMENT.ordinal(), null, MainState.COMMENT.ordinal(), accumulate );
            Automate.setTransition( MainState.COMMENT.ordinal(), MainLabel.empty.ordinal(),
                MainState.FINAL_COMMENT.ordinal(), nothing );

            // not needed : empty is defaut token type
            // Automate.setState( MainState.FINAL_EMPTY.ordinal(), tokenTypeEmpty);
            Automate.setState( MainState.FINAL_LIBELLE.ordinal(), tokenTypeLibelle );
            Automate.setState( MainState.FINAL_REV.ordinal(), tokenTypeRevision );
            Automate.setState( MainState.FINAL_COMMENT.ordinal(), tokenTypeComment );
        }
        catch ( Exception e )
        {
            LOG.error( "Automate", e );
        }
    }

    private StringCharacterIterator iterator = null;
    private Token currentToken = new Token();

    /**
     * Default constructor enter in initial state executor is SvnExternalsTokenizer
     * itself
     * 
     * @param iterator input stream
     * @throws AutomateException when error occurs
     */
    public SvnExternalsTokenizer( StringCharacterIterator iterator ) throws AutomateException
    {
        super( MainState.INIT.ordinal(), null );
        this.iterator = iterator;
    }

    public Token nextToken() throws AutomateException
    {
        Token ret = currentToken;
        do
        {
            char current = iterator.current();
            int etiquette = current == StringCharacterIterator.DONE ? MainLabel.empty.ordinal()
                : current == '-' ? MainLabel.minusChar.ordinal()
                    : current == 'r' ? MainLabel.rChar.ordinal()
                        : current == '\\' ? MainLabel.backSlack.ordinal()
                            : current == '"' ? MainLabel.quote.ordinal()
                                : current == '#' ? MainLabel.diese.ordinal()
                                    : Character.isWhitespace( current ) ? MainLabel.blank.ordinal()
                                        : Character.isDigit( current ) ? MainLabel.digit.ordinal()
                                            : MainLabel.other.ordinal();

            // System.out.println("nextToken call event with : " + etiquette + "," +
            // current);
            event( etiquette, new Character( current ) ); // iterator may change here !
        }
        while ( !isInFinalState() );

        initializer( MainState.INIT.ordinal(), null );
        currentToken = new Token();

        return ret;
    }

    @Override
    public String getName()
    {
        return "SvnExternalsTokenizer";
    }

    /**
     * @param s ignored parameter
     * 
     */
    public void accumulate( final Object s )
    {
        if ( currentToken.getValue() == null )
        {
            currentToken.setValue( new StringBuilder() );
        }
        if ( currentToken.getValue() instanceof StringBuilder )
        {
            // System.out.println("accumulate : " + iterator.current() );
            ( (StringBuilder) currentToken.getValue() ).append( iterator.current() );
        }
        char t = iterator.next();
        // System.out.println("accumulate next is : " + iterator.current() );
    }

    /**
     * @param s ignored parameter
     * 
     */
    public void drop( final Object s )
    {
        iterator.next();
        // System.out.println("drop next is : " + iterator.current() );
    }

    @Override
    protected boolean isInFinalState()
    {
        return getCurrentState() == MainState.FINAL_LIBELLE.ordinal()
            || getCurrentState() == MainState.FINAL_REV.ordinal()
            || getCurrentState() == MainState.FINAL_EMPTY.ordinal()
            || getCurrentState() == MainState.FINAL_COMMENT.ordinal();
    }

    @Override
    protected void notifyCurrentStateChanged()
    {
        // System.out.println("State changed to : " +
        // MainState.fromOrdinal(getCurrentState()));
    }
}
