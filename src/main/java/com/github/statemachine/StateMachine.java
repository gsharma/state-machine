package com.github.statemachine;

/**
 * Skeleton/marker interface for the FSM. This exists purely as a header file for easier
 * demonstration of machine functionality.
 * 
 * @author gsharma1
 */
public interface StateMachine {

  boolean transitionTo(State nextState) throws StateMachineException;

  /**
   * Rewind the state machine to either undo the last step/transition or reset it all the way to the
   * very beginning and to the NOT_STARTED state.
   * 
   * @param mode
   * @return
   */
  boolean rewind(RewindMode mode) throws StateMachineException;

  State readCurrentState() throws StateMachineException;

  boolean alive();

  /**
   * Shutdown the state machine and clear all state flows and intermediate data structures.
   */
  boolean shutdown() throws StateMachineException;

}
