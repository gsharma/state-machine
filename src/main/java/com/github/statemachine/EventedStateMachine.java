package com.github.statemachine;

/**
 * A simple Finite State Machine. Depending on how the transitions are implemented, this machine may
 * be setup as either deterministic or non-deterministic. There's nothing that forces the user's
 * hand one way or the other. Typical real world state machines are not so simplistic as to be
 * deterministic.
 * 
 * Notes for users:<br>
 * 0a. correctness is the most important virtue of this fsm<br>
 * 0b. less boilerplate code is the next most important virtue<br>
 * 
 * 1. this FSM instance is thread-safe<br>
 * 
 * 2. it is designed to not be singleton within a process, so, if there's a desire to have many
 * state machines, just create as many as needed<br>
 * 
 * 3. for every instance of FSM, various flows are meant to be reused. There's no need to make a new
 * FSM instance for the same flow every time.<br>
 * 
 * 4. expanding on #3, if a state machine is running and going through various state transitions,
 * the FSM itself does not expect any thread affinity (meaning the caller does not have to use the
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
public interface EventedStateMachine {
  // TODO
}
