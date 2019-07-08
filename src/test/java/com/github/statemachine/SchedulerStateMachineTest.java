package com.github.statemachine;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tests for an FSM for Jobs and Tasks in a Distributed Scheduler.
 * 
 * @author gaurav
 */
public final class SchedulerStateMachineTest {
  private static final Logger logger =
      LogManager.getLogger(SchedulerStateMachineTest.class.getSimpleName());

  // TODO
  // States:: SUBMIT -> PENDING -> RUNNING -> DEAD
  // Transitions (user):: submit, kill, update
  // Transitions (full):: submit, accept, reject, update, schedule, evict, finish, fail, kill, lost,

  public static final class States {
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

  public static final class TransitionSubmitToPending extends TransitionFunctor {
    public TransitionSubmitToPending() throws StateMachineException {
      super(States.submit, States.pending);
    }

    @Override
    public TransitionResult progress() {
      logger.info(States.submit.getName() + "->" + States.pending.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(States.pending.getName() + "->" + States.submit.getName());
      return new TransitionResult(true, null, null);
    }
  }

  public static final class TransitionPendingToRunning extends TransitionFunctor {
    public TransitionPendingToRunning() throws StateMachineException {
      super(States.pending, States.running);
    }

    @Override
    public TransitionResult progress() {
      logger.info(States.pending.getName() + "->" + States.running.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(States.running.getName() + "->" + States.pending.getName());
      return new TransitionResult(true, null, null);
    }
  }

  public static final class TransitionRunningToDead extends TransitionFunctor {
    public TransitionRunningToDead() throws StateMachineException {
      super(States.running, States.dead);
    }

    @Override
    public TransitionResult progress() {
      logger.info(States.running.getName() + "->" + States.dead.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(States.dead.getName() + "->" + States.running.getName());
      return new TransitionResult(true, null, null);
    }
  }

}
