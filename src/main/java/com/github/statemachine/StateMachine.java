package com.github.statemachine;

/**
 * A simple Finite State Machine. Depending on how the transitions are implemented, this machine may
 * be setup as either deterministic or non-deterministic. There's nothing that forces the user's
 * hand one way or the other. Typical real world state machines are not so simplistic as to be
 * deterministic.
 * 
 * Notes for users:<br>
 * 1. this FSM instance is thread-safe<br>
 * 
 * 2. it is designed to not be singleton within a process, so, if there's a desire to have many
 * state machines, just create as many as needed<br>
 * 
 * 3. for every instance of FSM, various flows are meant to be reused. There's no need to make a new
 * FSM instance for the same flow every time.<br>
 * 
 * 4. expanding on #3, if a state machine is running and going through various state transitions,
 * the FSM itself does not expect any thread affinity ( meaning the caller does not have to use the
 * same thread to change states).<br>
 * 
 * 5. this is a skeleton/marker interface for the FSM. This exists purely as a header file for
 * easier demonstration of machine functionality. It is not meant as a way to extend or create
 * custom state machines.<br>
 * 
 * 6. state transitions can be setup such that a failure of any transition in either forward or
 * backward direction triggers an auto-reset of the machine to its init state. Note that this will
 * not entail users having to rehydrate the transitions table in the machine<br>
 * 
 * 7. the State and Transition objects themselves are intended to be stateless. All state management
 * is done within the confines of the machine itself and doesn't spill out. The underlying idea is
 * that state and transition objects should be reusable across state machines eg. given states a, b,
 * c and transitions tAB, tBA, tBC, tCA, one could easily construct 2 different machines m1 and m2
 * with a subset of these states (a,b), (b,c), or (c,a).<br>
 * 
 * @author gaurav
 */
public interface StateMachine {

  /**
   * Transition the state machine to the given nextState. This will be one of the states set up as
   * part of the stateTransitionTable during initialization of the machine.
   * 
   * Returns true iff the state transition was successful.
   */
  boolean transitionTo(final State nextState) throws StateMachineException;

  /**
   * Rewind the state machine to either undo the last step/transition or reset it all the way to the
   * very beginning and to the INIT state.
   * 
   * Returns true iff the state transition was successful.
   */
  boolean rewind(final RewindMode mode) throws StateMachineException;

  /**
   * On failing a transition, reset the state machine to INIT state
   */
  void resetMachineOnTransitionFailure(boolean resetStateMachineOnFailure);

  /**
   * Read/report the current state of the state machine.
   */
  State readCurrentState() throws StateMachineException;

  /**
   * Lookup a transition by its id.
   */
  Transition findTranstion(final String transitionId) throws StateMachineException;

  /**
   * Reports the id of this StateMachine instance. You can have as many instances as you like.
   */
  String getId();

  /**
   * Check if the state machine is alive.
   */
  boolean alive();

  /**
   * Shutdown the state machine and clear all state flows and intermediate data structures.
   */
  boolean demolish() throws StateMachineException;

  /**
   * Print the potential route of state transitions.
   */
  String printStateTransitionRoute() throws StateMachineException;

}
