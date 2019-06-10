package com.github.statemachine;

/**
 * Tests for an FSM for Jobs and Tasks in a Distributed Scheduler.
 * 
 * @author gaurav
 */
public final class SchedulerStateMachineTest {
  // TODO
  // States:: SUBMIT -> PENDING -> RUNNING -> DEAD
  // Transitions (user):: submit, kill, update
  // Transitions (full):: submit, accept, reject, update, schedule, evict, finish, fail, kill, lost,
}
