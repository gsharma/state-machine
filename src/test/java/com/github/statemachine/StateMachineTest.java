package com.github.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.github.statemachine.StateMachineException.Code;

/**
 * Tests to maintain the sanity and correctness of StateMachine.
 */
public class StateMachineTest {
  static {
    System.setProperty("log4j.configurationFile", "log4j.properties");
  }

  private static final Logger logger = LogManager.getLogger(StateMachineImpl.class.getSimpleName());

  @Test
  public void testStateMachineFlow() throws StateMachineException {
    final List<TransitionFunctor> transitionFunctors = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitionFunctors.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitionFunctors.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitionFunctors.add(BtoC);
    final StateMachine machine = new StateMachineImpl(transitionFunctors);
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    // INIT->A
    assertTrue(machine.transitionTo(toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState());

    // A->B
    assertTrue(machine.transitionTo(AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState());

    // B->C
    assertTrue(machine.transitionTo(BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState());

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  @Test
  public void testStateMachineFlowFailure() throws StateMachineException {
    final List<TransitionFunctor> transitionFunctors = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitionFunctors.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitionFunctors.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitionFunctors.add(BtoC);
    final TransitionCVsD CtoD = new TransitionCVsD();
    transitionFunctors.add(CtoD);
    final StateMachine machine = new StateMachineImpl(transitionFunctors);
    machine.resetMachineOnTransitionFailure(true);
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    // INIT->A
    assertTrue(machine.transitionTo(toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState());

    // A->B
    assertTrue(machine.transitionTo(AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState());

    // B->C
    assertTrue(machine.transitionTo(BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState());

    // C->D will blow up, machine is instructed to reset
    assertFalse(machine.transitionTo(CtoD.getToState()));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  @Test
  public void testStateMachineFlowFullRewind() throws StateMachineException {
    final List<TransitionFunctor> transitionFunctors = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitionFunctors.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitionFunctors.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitionFunctors.add(BtoC);
    final StateMachine machine = new StateMachineImpl(transitionFunctors);
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    // INIT->A
    assertTrue(machine.transitionTo(toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState());

    // A->B
    assertTrue(machine.transitionTo(AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState());

    // B->C
    assertTrue(machine.transitionTo(BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState());

    // C->B->A->INIT
    assertTrue(machine.rewind(RewindMode.ALL_THE_WAY_STEP_WISE));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    // INIT->A
    assertTrue(machine.transitionTo(toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState());

    // A->B
    assertTrue(machine.transitionTo(AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState());

    // B->C
    assertTrue(machine.transitionTo(BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState());

    // C->B->A->INIT
    assertTrue(machine.rewind(RewindMode.ALL_THE_WAY_STEP_WISE));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  @Test
  public void testStateMachineFlowStepWiseRewind() throws StateMachineException {
    final List<TransitionFunctor> transitionFunctors = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitionFunctors.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitionFunctors.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitionFunctors.add(BtoC);
    final StateMachine machine = new StateMachineImpl(transitionFunctors);
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    // INIT->A
    assertTrue(machine.transitionTo(toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState());

    // A->B
    assertTrue(machine.transitionTo(AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState());

    // B->C
    assertTrue(machine.transitionTo(BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState());

    // C->B
    assertTrue(machine.rewind(RewindMode.ONE_STEP));
    assertEquals(BtoC.getFromState(), machine.readCurrentState());

    // B->A
    assertTrue(machine.rewind(RewindMode.ONE_STEP));
    assertEquals(AtoB.getFromState(), machine.readCurrentState());

    // A->INIT
    assertTrue(machine.rewind(RewindMode.ONE_STEP));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  @Test
  public void testStateMachineFlowRewindReset() throws StateMachineException {
    final List<TransitionFunctor> transitionFunctors = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitionFunctors.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitionFunctors.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitionFunctors.add(BtoC);
    final StateMachine machine = new StateMachineImpl(transitionFunctors);
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    // INIT->A
    assertTrue(machine.transitionTo(toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState());

    // A->B
    assertTrue(machine.transitionTo(AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState());

    // B->C
    assertTrue(machine.transitionTo(BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState());

    // C->INIT
    assertTrue(machine.rewind(RewindMode.ALL_THE_WAY_HARD_RESET));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  @Test
  public void testStateMachineThreadSafety() throws Exception {
    final List<TransitionFunctor> transitionFunctors = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitionFunctors.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitionFunctors.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitionFunctors.add(BtoC);
    final StateMachine machine = new StateMachineImpl(transitionFunctors);
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());

    final AtomicInteger toACounterSuccess = new AtomicInteger();
    final AtomicInteger toACounterFailure = new AtomicInteger();
    final AtomicInteger AtoBCounterSuccess = new AtomicInteger();
    final AtomicInteger AtoBCounterFailure = new AtomicInteger();
    final AtomicInteger BtoCCounterSuccess = new AtomicInteger();
    final AtomicInteger BtoCCounterFailure = new AtomicInteger();
    final Runnable transitionWorker = new Runnable() {
      @Override
      public void run() {
        try {
          // INIT->A
          boolean success = machine.transitionTo(toA.getToState());
          success &= toA.getToState() == machine.readCurrentState();
          if (success) {
            toACounterSuccess.incrementAndGet();
          } else {
            toACounterFailure.incrementAndGet();
          }

          // A->B
          success = machine.transitionTo(AtoB.getToState());
          success &= AtoB.getToState() == machine.readCurrentState();
          if (success) {
            AtoBCounterSuccess.incrementAndGet();
          } else {
            AtoBCounterFailure.incrementAndGet();
          }

          // B->C
          success = machine.transitionTo(BtoC.getToState());
          success &= BtoC.getToState() == machine.readCurrentState();
          if (success) {
            BtoCCounterSuccess.incrementAndGet();
          } else {
            BtoCCounterFailure.incrementAndGet();
          }
        } catch (StateMachineException exception) {
        }
      }
    };

    int workerCount = 10;
    final List<Thread> workers = new ArrayList<>(workerCount);
    for (int iter = 0; iter < workerCount; iter++) {
      final Thread worker = new Thread(transitionWorker, "transition-worker-" + iter);
      workers.add(worker);
    }
    for (final Thread worker : workers) {
      worker.start();
    }
    for (final Thread worker : workers) {
      worker.join();
    }

    assertEquals(1, toACounterSuccess.get());
    assertEquals(workerCount - 1, toACounterFailure.get());
    assertEquals(1, AtoBCounterSuccess.get());
    assertEquals(workerCount - 1, AtoBCounterFailure.get());
    assertEquals(1, BtoCCounterSuccess.get());
    assertEquals(workerCount - 1, BtoCCounterFailure.get());

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  public static final class States {
    public static State aState, bState, cState, dState;
    static {
      try {
        aState = new State(Optional.of("A"));
        bState = new State(Optional.of("B"));
        cState = new State(Optional.of("C"));
        dState = new State(Optional.of("D"));
      } catch (StateMachineException e) {
      }
    }
  }

  public static class TransitionNotStartedVsA extends TransitionFunctor {
    public TransitionNotStartedVsA() throws StateMachineException {
      super(StateMachineImpl.notStartedState, States.aState);
    }

    @Override
    public TransitionResult progress() {
      logger.info(StateMachineImpl.notStartedState.getName() + "->" + States.aState.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(States.aState.getName() + "->" + StateMachineImpl.notStartedState.getName());
      return new TransitionResult(true, null, null);
    }
  }

  public static class TransitionAVsB extends TransitionFunctor {
    public TransitionAVsB() throws StateMachineException {
      super(States.aState, States.bState);
    }

    @Override
    public TransitionResult progress() {
      logger.info(States.aState.getName() + "->" + States.bState.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(States.bState.getName() + "->" + States.aState.getName());
      return new TransitionResult(true, null, null);
    }
  }

  public static class TransitionBVsC extends TransitionFunctor {
    public TransitionBVsC() throws StateMachineException {
      super(States.bState, States.cState);
    }

    @Override
    public TransitionResult progress() {
      logger.info(States.bState.getName() + "->" + States.cState.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(States.cState.getName() + "->" + States.bState.getName());
      return new TransitionResult(true, null, null);
    }
  }

  public static class TransitionCVsD extends TransitionFunctor {
    public TransitionCVsD() throws StateMachineException {
      super(States.cState, States.dState);
    }

    @Override
    public TransitionResult progress() {
      logger.info(States.cState.getName() + "->" + States.dState.getName());
      return new TransitionResult(false, null, new StateMachineException(Code.TRANSITION_FAILURE));
    }

    @Override
    public TransitionResult regress() {
      logger.info(States.cState.getName() + "->" + States.bState.getName());
      return new TransitionResult(false, null, new StateMachineException(Code.TRANSITION_FAILURE));
    }
  }

}
