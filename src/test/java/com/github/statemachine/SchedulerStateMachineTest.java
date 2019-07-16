package com.github.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import com.github.statemachine.StateMachine.StateMachineBuilder;
import com.github.statemachine.StateMachineConfiguration.StateMachineConfigurationBuilder;

/**
 * Tests for an FSM for Jobs and Tasks in a Distributed Scheduler.
 * 
 * @author gaurav
 */
public final class SchedulerStateMachineTest {
  private static final Logger logger =
      LogManager.getLogger(SchedulerStateMachineTest.class.getSimpleName());

  @Test
  public void testJobFlow() throws StateMachineException {
    // 1. prep transitions
    final TransitionNotStartedToSubmit notStartedToSubmit = new TransitionNotStartedToSubmit();
    final TransitionSubmitToPending submitToPending = new TransitionSubmitToPending();
    final TransitionPendingToRunning pendingToRunning = new TransitionPendingToRunning();
    final TransitionRunningToDead runningToDead = new TransitionRunningToDead();

    // 2. load up the fsm with all its transitions
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ALL_THE_WAY_HARD_RESET)
        .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
    final StateMachine machine =
        StateMachineBuilder.newBuilder().config(config).transitions().next(notStartedToSubmit)
            .next(submitToPending).next(pendingToRunning).next(runningToDead).build();
    assertTrue(machine.alive());

    // 3. start a flow
    final String flowId = machine.startFlow();
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    // 4a. execute a flow transition
    // notStarted->submit
    assertTrue(machine.transitionTo(flowId, notStartedToSubmit.getToState()));
    assertEquals(notStartedToSubmit.getToState(), machine.readCurrentState(flowId));

    // 4b. execute a flow transition
    // submit->pending
    assertTrue(machine.transitionTo(flowId, submitToPending.getToState()));
    assertEquals(submitToPending.getToState(), machine.readCurrentState(flowId));

    // 4c. execute a flow transition
    // pending->running
    assertTrue(machine.transitionTo(flowId, pendingToRunning.getToState()));
    assertEquals(pendingToRunning.getToState(), machine.readCurrentState(flowId));

    // 4d. execute a flow transition
    // running->dead
    assertTrue(machine.transitionTo(flowId, runningToDead.getToState()));
    assertEquals(runningToDead.getToState(), machine.readCurrentState(flowId));

    // 5. stop the flow
    assertTrue(machine.stopFlow(flowId));

    // 6. stop the fsm
    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
  }

  // Job States:: SUBMIT -> PENDING -> RUNNING -> DEAD
  // Transitions (user):: submit, kill, update
  // Transitions (full):: submit, accept, reject, update, schedule, evict, finish, fail, kill, lost
  public static final class JobStates {
    public static State submit, pending, running, dead;
    static {
      try {
        submit = new State(Optional.of("SUBMIT"));
        pending = new State(Optional.of("PENDING"));
        running = new State(Optional.of("RUNNING"));
        dead = new State(Optional.of("DEAD"));
      } catch (StateMachineException problem) {
      }
    }
  }

  public static final class TransitionNotStartedToSubmit extends TransitionFunctor {
    public TransitionNotStartedToSubmit() throws StateMachineException {
      super(StateMachineImpl.notStartedState, JobStates.submit);
    }

    @Override
    public TransitionResult progress() {
      logger.info(StateMachineImpl.notStartedState + "->" + JobStates.submit.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(StateMachineImpl.notStartedState.getName() + "->" + JobStates.submit.getName());
      return new TransitionResult(true, null, null);
    }
  }

  public static final class TransitionSubmitToPending extends TransitionFunctor {
    public TransitionSubmitToPending() throws StateMachineException {
      super(JobStates.submit, JobStates.pending);
    }

    @Override
    public TransitionResult progress() {
      logger.info(JobStates.submit.getName() + "->" + JobStates.pending.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(JobStates.pending.getName() + "->" + JobStates.submit.getName());
      return new TransitionResult(true, null, null);
    }
  }

  public static final class TransitionPendingToRunning extends TransitionFunctor {
    public TransitionPendingToRunning() throws StateMachineException {
      super(JobStates.pending, JobStates.running);
    }

    @Override
    public TransitionResult progress() {
      logger.info(JobStates.pending.getName() + "->" + JobStates.running.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(JobStates.running.getName() + "->" + JobStates.pending.getName());
      return new TransitionResult(true, null, null);
    }
  }

  public static final class TransitionRunningToDead extends TransitionFunctor {
    public TransitionRunningToDead() throws StateMachineException {
      super(JobStates.running, JobStates.dead);
    }

    @Override
    public TransitionResult progress() {
      logger.info(JobStates.running.getName() + "->" + JobStates.dead.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(JobStates.dead.getName() + "->" + JobStates.running.getName());
      return new TransitionResult(true, null, null);
    }
  }

}
