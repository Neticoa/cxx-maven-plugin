package org.apache.maven.plugin;

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

import org.codehaus.utils.Automate;
import org.codehaus.utils.AutomateException;
import org.codehaus.utils.Action;
import org.codehaus.utils.ActionExecutor;

/**
 * TODO
 * 
 * @version 3 nov. 2010
 * @author fbonin
 */
public class AutomateTest extends AbstractMojoTestCase
{
    // @formatter:off
    /**
     * We implement this funny automate :
     * </code>
     *     good/good_vibration()
     *     +---------------------+       +---------+
     * +---+----+                |   +---+----+    | good/good_vibration()
     * |        |                +-->|        |    | 
     * | INIT   |                    | FINAL  |<---+ 
     * |choose()|                +-->|        |
     * +---+----+                |   +--------+          
     *     | bad/bad_vibration() | 
     *     +---------------------+
     * 
     * Init state 'choose' something...
     * </code>
     * @author fbonin
     * 
     */
    // @formatter:on
    static class Sample extends Automate {

        /**
         * define automate labels
         */
        enum MainLabel 
        {
            bad,
            good,
            other;

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

        /**
         * define automate states
         */
        enum MainState {
            FINAL,
            INIT;

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
            Action good_vibration = new Action()
            {
                public void action(final ActionExecutor executor, final Object aiParam)
                    throws AutomateException
                {

                    ((Sample) executor).good_vibration(aiParam);
                }

                public String getName()
                {
                    return "good_vibration";
                }
            };
            Action bad_vibration = new Action()
            {
                public void action(final ActionExecutor executor, final Object aiParam)
                    throws AutomateException
                {
                    ((Sample) executor).bad_vibration(aiParam);
                }

                public String getName()
                {
                    return "bad_vibration";
                }
            };
            Action choose = new Action()
            {
                public void action(final ActionExecutor executor, final Object aiParam)
                                        throws AutomateException
                {
                    ((Sample) executor).choose(aiParam);
                }

                public String getName()
                {
                    return "choose";
                }
            };
            try
            {
                Automate.initializeAutomate(MainState.values().length, MainLabel.values().length);
                Automate.setTransition(MainState.INIT.ordinal(), MainLabel.good.ordinal(),
                                        MainState.FINAL.ordinal(), good_vibration);
                Automate.setTransition(MainState.INIT.ordinal(), MainLabel.bad.ordinal(),
                                        MainState.FINAL.ordinal(), bad_vibration);
                Automate.setTransition(MainState.FINAL.ordinal(), MainLabel.good.ordinal(),
                                        MainState.FINAL.ordinal(), good_vibration);
                Automate.setState(MainState.INIT.ordinal(), choose);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        /**
         * Default constructor enter in initial state executor is
         * Sample itself
         * 
         * @throws AutomateException
         */
        public Sample() throws AutomateException
        {
            super(MainState.INIT.ordinal(), "Parameter 1");
        }

        /**
         * Constructor to restore from any state executor is Sample
         * itself
         * 
         * @param aiCurrentState
         * @throws AutomateException
         */
        public Sample(final MainState aiCurrentState) throws AutomateException
        {
            super(aiCurrentState.ordinal(), "Parameter 2");
        }

        /**
         * TODO
         * 
         * @param s
         */
        public void bad_vibration(final Object s)
        {
            System.out.println("Bad Vibration transition has access to parameter : " + s);
        }

        /**
         * TODO
         * 
         * @param s
         * @throws AutomateException
         */
        public void choose(final Object s) throws AutomateException
        {
            // this is a state action that choose its own label to
            // follow :
            MainLabel l = MainLabel.bad;
            System.out.println("choose state action choose " + l + " label");
            // don't forget to let automate know of our choice
            event(l.ordinal(), s);
        }

        @Override
        public String getName()
        {
            return "sample";
        }

        /**
         * @param s
         * 
         */
        public void good_vibration(final Object s)
        {
            System.out.println("Good Vibration transition has access to parameter : " + s);
        }

        @Override
        protected boolean isInFinalState()
        {
            return getCurrentState() == MainState.FINAL.ordinal();
        }

        @Override
        protected void notifyCurrentStateChanged()
        {
            System.out.println("State changed to : " + MainState.fromOrdinal(getCurrentState()));
        }
    };

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
    public void testAutomateBinaire() throws Exception
    {
        // choose_your_vibration() will be called since we step
        // in initial state with its auto-choose action
        Sample m = new Sample();
        assertEquals(Sample.MainState.fromOrdinal(m.getCurrentState()),
                            Sample.MainState.FINAL);
        // 'good' event is allowed in init and final state
        m.event(Sample.MainLabel.good.ordinal(), "parameter 3");
        assertEquals(Sample.MainState.fromOrdinal(m.getCurrentState()),
                            Sample.MainState.FINAL);
    }
}
