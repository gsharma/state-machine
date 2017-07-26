package com.github.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

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
    final List<Transition> transitions = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitions.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitions.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitions.add(BtoC);
    final StateMachine machine = new StateMachineImpl(transitions);
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
    assertTrue(machine.shutdown());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  @Test
  public void testStateMachineFlowFullRewind() throws StateMachineException {
    final List<Transition> transitions = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitions.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitions.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitions.add(BtoC);
    final StateMachine machine = new StateMachineImpl(transitions);
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
    assertTrue(machine.shutdown());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  @Test
  public void testStateMachineFlowStepWiseRewind() throws StateMachineException {
    final List<Transition> transitions = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitions.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitions.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitions.add(BtoC);
    final StateMachine machine = new StateMachineImpl(transitions);
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
    assertTrue(machine.shutdown());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  @Test
  public void testStateMachineFlowRewindReset() throws StateMachineException {
    final List<Transition> transitions = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitions.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitions.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitions.add(BtoC);
    final StateMachine machine = new StateMachineImpl(transitions);
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
    assertTrue(machine.shutdown());
    assertFalse(machine.alive());
    assertEquals(StateMachineImpl.notStartedState, machine.readCurrentState());
  }

  public static final class States {
    public static State aState, bState, cState;
    static {
      try {
        aState = new State(Optional.of("A"));
        bState = new State(Optional.of("B"));
        cState = new State(Optional.of("C"));
      } catch (StateMachineException e) {
      }
    }
  }

  public static class TransitionNotStartedVsA extends Transition {
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

  public static class TransitionAVsB extends Transition {
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

  public static class TransitionBVsC extends Transition {
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

}
