package com.github.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.github.statemachine.StateMachine.StateMachineBuilder;
import com.github.statemachine.StateMachineConfiguration.StateMachineConfigurationBuilder;
import com.github.statemachine.StateMachineException.Code;

/**
 * Tests to maintain the sanity and correctness of StateMachine.
 */
public class StateMachineTest {
  private static final Logger logger = LogManager.getLogger(StateMachineImpl.class.getSimpleName());

  @Test
  public void testStateMachineFlow() throws StateMachineException {
    // 1. prep transitions
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    final TransitionAVsB AtoB = new TransitionAVsB();
    final TransitionBVsC BtoC = new TransitionBVsC();

    // 2. load up the fsm with all its transitions
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ALL_THE_WAY_HARD_RESET)
        .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
    final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
        .next(toA).next(AtoB).next(BtoC).build();
    assertTrue(machine.alive());

    // 3. start a flow
    final String flowId = machine.startFlow();
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    // 4a. execute a flow transition
    // INIT->A
    assertTrue(machine.transitionTo(flowId, toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState(flowId));

    // 4b. execute a flow transition
    // A->B
    assertTrue(machine.transitionTo(flowId, AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState(flowId));

    // 4c. execute a flow transition
    // B->C
    assertTrue(machine.transitionTo(flowId, BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState(flowId));

    // 5. stop the flow
    assertTrue(machine.stopFlow(flowId));

    // 6. stop the fsm
    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
  }

  @Test
  public void testStateMachineFlowAutoAsync() throws Exception {
    // 1. prep transitions
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    final TransitionAVsB AtoB = new TransitionAVsB();
    final TransitionBVsC BtoC = new TransitionBVsC();

    // 2. load up the fsm with all its transitions
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.AUTO_ASYNC).rewindMode(RewindMode.ALL_THE_WAY_HARD_RESET)
        .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
    final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
        .next(toA).next(AtoB).next(BtoC).build();
    assertTrue(machine.alive());

    // 3. start a flow
    final String flowId = machine.startFlow();


    // 4. give it a lil breather to finish running
    // since auto-async mode will auto-stop this 1 flow, check stats
    while (machine.getStatistics().getTotalStoppedFlows() != 1) {
      logger.info("Sleeping 5 millis for flow {} to finish", flowId);
      Thread.sleep(5L);
    }

    // 5. stop the fsm
    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
  }

  @Test
  public void testStateMachineFlowAutoCallerThread() throws Exception {
    // 1. prep transitions
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    final TransitionAVsB AtoB = new TransitionAVsB();
    final TransitionBVsC BtoC = new TransitionBVsC();

    // 2. load up the fsm with all its transitions
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.AUTO_CALLER_THREAD).rewindMode(RewindMode.ALL_THE_WAY_HARD_RESET)
        .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
    final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
        .next(toA).next(AtoB).next(BtoC).build();
    assertTrue(machine.alive());

    // 3. start a flow
    final String flowId = machine.startFlow();

    // 4. stop the flow
    assertTrue(machine.stopFlow(flowId));

    // 5. stop the fsm
    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
  }

  @Test
  public void testMultipleFlows() throws StateMachineException {
    // 1. prep transitions
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    final TransitionAVsB AtoB = new TransitionAVsB();
    final TransitionBVsC BtoC = new TransitionBVsC();

    // 2. load up the fsm with all its transitions
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ONE_STEP).resetMachineToInitOnFailure(true)
        .flowExpirationMillis(0).build();
    final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
        .next(toA).next(AtoB).next(BtoC).build();
    assertTrue(machine.alive());

    // 3. start a flow
    String flowId = machine.startFlow();
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    // 4a. execute a flow transition
    // INIT->A
    assertTrue(machine.transitionTo(flowId, toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState(flowId));

    // 4b. execute a flow transition
    // A->B
    assertTrue(machine.transitionTo(flowId, AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState(flowId));

    // 4c. execute a flow transition
    // B->C
    assertTrue(machine.transitionTo(flowId, BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState(flowId));

    // 5. stop the flow
    assertTrue(machine.stopFlow(flowId));

    // test another flow for the same fsm
    // 3. start a flow
    flowId = machine.startFlow();
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    // 4a. execute a flow transition
    // INIT->A
    assertTrue(machine.transitionTo(flowId, toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState(flowId));

    // 4b. execute a flow transition
    // A->B
    assertTrue(machine.transitionTo(flowId, AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState(flowId));

    // 4c. execute a flow transition
    // B->A
    assertTrue(machine.rewind(flowId));
    assertEquals(AtoB.getFromState(), machine.readCurrentState(flowId));

    // 5. stop the flow
    assertTrue(machine.stopFlow(flowId));

    // 6. stop the fsm
    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
  }

  @Test
  public void testStateMachineFlowFailure() throws StateMachineException {
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    final TransitionAVsB AtoB = new TransitionAVsB();
    final TransitionBVsC BtoC = new TransitionBVsC();
    final TransitionCVsD CtoD = new TransitionCVsD();
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ALL_THE_WAY_HARD_RESET)
        .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
    final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
        .next(toA).next(AtoB).next(BtoC).next(CtoD).build();
    final String flowId = machine.startFlow();
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    // INIT->A
    assertTrue(machine.transitionTo(flowId, toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState(flowId));

    // A->B
    assertTrue(machine.transitionTo(flowId, AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState(flowId));

    // B->C
    assertTrue(machine.transitionTo(flowId, BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState(flowId));

    // C->D will blow up, machine is instructed to reset
    assertFalse(machine.transitionTo(flowId, CtoD.getToState()));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    assertTrue(machine.stopFlow(flowId));

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
  }

  @Test
  public void testStateMachineFlowFullRewind() throws StateMachineException {
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    final TransitionAVsB AtoB = new TransitionAVsB();
    final TransitionBVsC BtoC = new TransitionBVsC();
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ALL_THE_WAY_STEP_WISE)
        .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
    final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
        .next(toA).next(AtoB).next(BtoC).build();
    final String flowId = machine.startFlow();
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    // INIT->A
    assertTrue(machine.transitionTo(flowId, toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState(flowId));

    // A->B
    assertTrue(machine.transitionTo(flowId, AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState(flowId));

    // B->C
    assertTrue(machine.transitionTo(flowId, BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState(flowId));

    // C->B->A->INIT
    assertTrue(machine.rewind(flowId));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    // INIT->A
    assertTrue(machine.transitionTo(flowId, toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState(flowId));

    // A->B
    assertTrue(machine.transitionTo(flowId, AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState(flowId));

    // B->C
    assertTrue(machine.transitionTo(flowId, BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState(flowId));

    // C->B->A->INIT
    assertTrue(machine.rewind(flowId));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    logger.info("route::" + Arrays.deepToString(machine.getStateTransitionRoute(flowId)));
    assertTrue(machine.stopFlow(flowId));

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
  }

  @Test
  public void testStateMachineFlowStepWiseRewind() throws StateMachineException {
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    final TransitionAVsB AtoB = new TransitionAVsB();
    final TransitionBVsC BtoC = new TransitionBVsC();
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ONE_STEP).resetMachineToInitOnFailure(true)
        .flowExpirationMillis(0).build();
    final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
        .next(toA).next(AtoB).next(BtoC).build();
    final String flowId = machine.startFlow();
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    // INIT->A
    assertTrue(machine.transitionTo(flowId, toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState(flowId));

    // A->B
    assertTrue(machine.transitionTo(flowId, AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState(flowId));

    // B->C
    assertTrue(machine.transitionTo(flowId, BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState(flowId));

    // C->B
    assertTrue(machine.rewind(flowId));
    assertEquals(BtoC.getFromState(), machine.readCurrentState(flowId));

    // B->A
    assertTrue(machine.rewind(flowId));
    assertEquals(AtoB.getFromState(), machine.readCurrentState(flowId));

    // A->INIT
    assertTrue(machine.rewind(flowId));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    assertTrue(machine.stopFlow(flowId));

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
  }

  @Test
  public void testStateMachineFlowRewindReset() throws StateMachineException {
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    final TransitionAVsB AtoB = new TransitionAVsB();
    final TransitionBVsC BtoC = new TransitionBVsC();
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ALL_THE_WAY_HARD_RESET)
        .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
    final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
        .next(toA).next(AtoB).next(BtoC).build();
    final String flowId = machine.startFlow();
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    // INIT->A
    assertTrue(machine.transitionTo(flowId, toA.getToState()));
    assertEquals(toA.getToState(), machine.readCurrentState(flowId));

    // A->B
    assertTrue(machine.transitionTo(flowId, AtoB.getToState()));
    assertEquals(AtoB.getToState(), machine.readCurrentState(flowId));

    // B->C
    assertTrue(machine.transitionTo(flowId, BtoC.getToState()));
    assertEquals(BtoC.getToState(), machine.readCurrentState(flowId));

    // C->INIT
    assertTrue(machine.rewind(flowId));
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

    assertTrue(machine.stopFlow(flowId));

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
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
    final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
        .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ALL_THE_WAY_HARD_RESET)
        .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
    final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
        .next(toA).next(AtoB).next(BtoC).build();

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
          final String flowId = machine.startFlow();
          // INIT->A
          boolean success = machine.transitionTo(flowId, toA.getToState());
          if (success && (toA.getToState() == machine.readCurrentState(flowId))) {
            toACounterSuccess.incrementAndGet();
          } else {
            toACounterFailure.incrementAndGet();
          }

          // A->B
          success = machine.transitionTo(flowId, AtoB.getToState());
          if (success && (AtoB.getToState() == machine.readCurrentState(flowId))) {
            AtoBCounterSuccess.incrementAndGet();
          } else {
            AtoBCounterFailure.incrementAndGet();
          }

          // B->C
          success = machine.transitionTo(flowId, BtoC.getToState());
          if (success && (BtoC.getToState() == machine.readCurrentState(flowId))) {
            BtoCCounterSuccess.incrementAndGet();
          } else {
            BtoCCounterFailure.incrementAndGet();
          }

          logger.info("route::" + Arrays.deepToString(machine.getStateTransitionRoute(flowId)));

          logger.info(machine.getStatistics().toString());
          assertTrue(machine.stopFlow(flowId));
        } catch (StateMachineException problem) {
          logger.error("machine:" + machine.getId() + " encountered an issue", problem);
        }
      }
    };

    int workerCount = 5;
    final List<Thread> workers = new ArrayList<>(workerCount);
    for (int iter = 0; iter < workerCount; iter++) {
      final Thread worker = new Thread(transitionWorker, "test-transition-worker-" + iter);
      workers.add(worker);
    }
    for (final Thread worker : workers) {
      worker.start();
    }
    for (final Thread worker : workers) {
      worker.join();
    }

    assertEquals(workerCount, toACounterSuccess.get());
    assertEquals(0, toACounterFailure.get());
    assertEquals(workerCount, AtoBCounterSuccess.get());
    assertEquals(0, AtoBCounterFailure.get());
    assertEquals(workerCount, BtoCCounterSuccess.get());
    assertEquals(0, BtoCCounterFailure.get());

    assertTrue(machine.alive());
    assertTrue(machine.demolish());
    assertFalse(machine.alive());
  }

  @Test
  public void testMultipleStateMachines() throws Exception {
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    final TransitionAVsB AtoB = new TransitionAVsB();
    final TransitionBVsC BtoC = new TransitionBVsC();

    final Runnable stateMachineWorker = new Runnable() {
      @Override
      public void run() {
        StateMachine machine = null;
        try {
          final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
              .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ONE_STEP)
              .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
          machine = StateMachineBuilder.newBuilder().config(config).transitions().next(toA)
              .next(AtoB).next(BtoC).build();
          final String flowId = machine.startFlow();
          assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

          // INIT->A
          assertTrue(machine.transitionTo(flowId, toA.getToState()));
          assertEquals(toA.getToState(), machine.readCurrentState(flowId));

          // A->B
          assertTrue(machine.transitionTo(flowId, AtoB.getToState()));
          assertEquals(AtoB.getToState(), machine.readCurrentState(flowId));

          // B->C
          assertTrue(machine.transitionTo(flowId, BtoC.getToState()));
          assertEquals(BtoC.getToState(), machine.readCurrentState(flowId));

          // C->B
          assertTrue(machine.rewind(flowId));
          assertEquals(BtoC.getFromState(), machine.readCurrentState(flowId));

          // B->A
          assertTrue(machine.rewind(flowId));
          assertEquals(AtoB.getFromState(), machine.readCurrentState(flowId));

          // A->INIT
          assertTrue(machine.rewind(flowId));
          assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState(flowId));

          assertTrue(machine.stopFlow(flowId));

          assertTrue(machine.alive());
          assertTrue(machine.demolish());
          assertFalse(machine.alive());
        } catch (StateMachineException problem) {
          logger.error("machine:" + machine.getId() + " encountered an issue", problem);
        }
      }
    };

    int workerCount = 5;
    final List<Thread> workers = new ArrayList<>(workerCount);
    for (int iter = 0; iter < workerCount; iter++) {
      final Thread worker = new Thread(stateMachineWorker, "test-machine-worker-" + iter);
      workers.add(worker);
    }
    for (final Thread worker : workers) {
      worker.start();
    }
    for (final Thread worker : workers) {
      worker.join();
    }
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
