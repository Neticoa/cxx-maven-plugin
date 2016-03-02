package org.codehaus.utils;

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
 * 
 * @author fbonin
 * 
 */
public abstract class Automate implements ActionExecutor
{
    private static class Transition
    {
        /**
         * Etat suivant
         */
        private int nextState = -1;
        
        /**
         * Action à effectuée
         */
        private Action transitionAction = null;

        /**
         * @param pINextState
         * @param pIAction
         */
        public Transition( final int pINextState, final Action pIAction )
        {
            nextState = pINextState;
            transitionAction = pIAction;
        }

        /**
         * tell if transition exists
         * 
         * @return true if transition exist
         */
        public boolean isPossible()
        {
            return ( transitionAction != null ) && ! transitionAction.equals( impossible );
        }

        /**
         * 
         * @param pIExecutor
         * @param pIAutomate
         * @param pIParam
         * @throws AutomateException
         */
        public void passThrough( final ActionExecutor pIExecutor, final Automate pIAutomate,
                        final Object pIParam ) throws AutomateException
        {
            transitionAction.action( pIExecutor, pIParam );
            pIAutomate.changeState( nextState, pIParam );
        }
    };

    private static boolean classInititialized = false;
    private static Action empty = new Action()
    {
        public void action( final ActionExecutor pIExecutor, final Object pIParam )
        {
            // System.out.println(" Information : empty() action called for executor "
            // + aiExecutor.getName());
        }

        public String getName()
        {
            return "empty";
        }
    };
    private static Action impossible = new Action()
    {
        public void action( final ActionExecutor aiExecutor, final Object aiParam ) throws AutomateException
        {
            throw new AutomateException( "action 'impossible(Object aiParam)' was called for executor "
                 + aiExecutor.getName() );
        }

        public String getName()
        {
            return "impossible";
        }
    };
    
    /**
     * 
     */
    private static int labelCount = 0;
    private static Transition model[][] = null;
    private static Action stateActions[] = null;
    
    /**
     * 
     */
    private static int stateCount = 0;

    /**
     * TODO
     * 
     * @param aiStateCount
     * @param aiLabelCount
     * @throws AutomateException
     */
    protected static void initializeAutomate( final int aiStateCount, final int aiLabelCount ) throws AutomateException
    {
        stateCount = aiStateCount;
        labelCount = aiLabelCount;
        if ( ( 0 >= stateCount ) || ( 0 >= labelCount ) )
        {
            throw new AutomateException(
                 "You must provide a positive value for stateCount and labelCount when calling initializeAutomate()" );
        }
        model = new Transition[stateCount][labelCount];
        stateActions = new Action[stateCount];
        for ( int e = 0; e < stateCount; e++ )
        {
            stateActions[e] = empty;
            for ( int t = 0; t < labelCount; t++ )
            {
                model[e][t] = new Transition( e, impossible );
            }
        }
        classInititialized = true;
    }

    /**
     * 
     * @param aiState
     * @param aiStateAction
     */
    protected static void setState( final int aiState, final Action aiStateAction )
    {
        setState( aiState, aiStateAction, -1, null );
    } 
    
    /**
     * @param aiState
     * @param aiAction
     * @param aiDefaultAction
     */
    protected static void setState( final int aiState, final Action aiStateAction,
       final int aiDefaultTargetState, final Action aiDefaultAction )
    {
        if ( null != aiStateAction )
        {
            stateActions[aiState] = aiStateAction;
        }
        for ( int t = 0; t < labelCount; t++ )
        {
            // we focus on unhandled transitions
            if ( null == model[aiState][t].transitionAction || impossible == model[aiState][t].transitionAction )
            {
                if ( null != aiDefaultAction )
                {
                    model[aiState][t].transitionAction = aiDefaultAction;
                }
                if ( aiDefaultTargetState >= 0 )
                {
                    model[aiState][t].nextState = aiDefaultTargetState;
                }
            }
        }
    }

    /**
     * setTransition wire your automate
     * 
     * @param aiState
     * @param aiLabel
     * @param aiNextState
     * @param aiAction
     */
    protected static void setTransition( final int aiState, final int aiLabel, final int aiNextState,
                final Action aiAction )
    {
        model[aiState][aiLabel] = new Transition( aiNextState, aiAction );
    }

    /**
     * Etat courant
     */
    private int currentState = -1;
    
    /**
     * 
     */
    private ActionExecutor executor = null;
    
    /**
     * Objet initialisé
     */
    private boolean objectInitialized = false;
    

    protected Automate( final ActionExecutor aiExecutor, final int aiCurrentState,
                final Object aioParam ) throws AutomateException
    {
        initializer( aiExecutor, aiCurrentState, aioParam );
    }

    protected Automate( final int aiCurrentState, final Object aioParam )
                throws AutomateException 
    {
        initializer( this, aiCurrentState, aioParam );
    }

    private void changeState( final int aiNextState, final Object aiParam ) throws AutomateException
    {
        if ( ! objectInitialized || ( currentState != aiNextState ) )
        {
            currentState = aiNextState;
            notifyCurrentStateChanged();
            objectInitialized = true;
            stateActions[currentState].action( executor, aiParam ); // <=> // this.stateActions[currentState](aiParam);
        }
    }

    /**
     * event function top inform automate from label to try
     * 
     * @param aiLabel
     *            label to try
     * @param aiParam
     *            associated data that transition action will receive
     *            (and next state action too)
     * @throws AutomateException
     */
    public void event( final int aiLabel, final Object aiParam ) throws AutomateException
    {
        model[currentState][aiLabel].passThrough( executor, this, aiParam );
    }

    /**
     * @return currentState
     */
    public int getCurrentState()
    {
        return currentState;
    }
    
    protected void initializer( final int aiCurrentState, final Object aiParam )
        throws AutomateException
    {
        initializer( this, aiCurrentState, aiParam );
    }

    protected void initializer( final ActionExecutor aiExecutor, final int aiCurrentState, final Object aiParam )
        throws AutomateException
    {
        //System.out.println("Automate.initializer : " + classInititialized + "," + aiCurrentState
        //    + "," + isInFinalState() );
        if ( classInititialized && ( currentState == -1 || isInFinalState() ) )
        {
            executor = aiExecutor;
            changeState( aiCurrentState, aiParam );
        } 
        else
        {
            throw new AutomateException(
                "public static void initializeAutomate() must be called before trying to instantiate an Automate" );
        }
    }

    /**
     * 
     */
    protected abstract boolean isInFinalState();

    /**
     * according to current state, tell if transition exists useful to
     * activate/unactivate GUI components
     * 
     * @param aiLabel transition label
     * 
     * @return true if transition exist
     */
    public boolean isPossible( final int aiLabel )
    {
        return model[currentState][aiLabel].isPossible();
    }

    /**
     * 
     */
    protected abstract void notifyCurrentStateChanged();
};
