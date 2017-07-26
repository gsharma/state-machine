package com.github.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.github.statemachine.StateMachine.RewindMode;
import com.github.statemachine.StateMachine.State;
import com.github.statemachine.StateMachine.StateMachineException;
import com.github.statemachine.StateMachine.Transition;
import com.github.statemachine.StateMachine.TransitionResult;

/**
 * Tests to maintain the sanity and correctness of StateMachine.
 */
public class StateMachineTest {
  static {
    System.setProperty("log4j.configurationFile", "log4j.properties");
  }

  private static final Logger logger = LogManager.getLogger(StateMachine.class.getSimpleName());

  @Test
  public void testStateMachineFlow() throws StateMachineException {
    final List<Transition> transitions = new ArrayList<>();
    final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
    transitions.add(toA);
    final TransitionAVsB AtoB = new TransitionAVsB();
    transitions.add(AtoB);
    final TransitionBVsC BtoC = new TransitionBVsC();
    transitions.add(BtoC);
    final StateMachine machine = new StateMachine(transitions);
    assertEquals(StateMachine.notStartedState, machine.readCurrentState());

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
    assertEquals(StateMachine.notStartedState, machine.readCurrentState());

    /*
     * success = machine.rewind(RewindMode.ONE_STEP); logger.info(machine.readCurrentState());
     * success = machine.rewind(RewindMode.ONE_STEP); logger.info(machine.readCurrentState());
     * success = machine.rewind(RewindMode.ONE_STEP);
     */

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
    assertEquals(StateMachine.notStartedState, machine.readCurrentState());

    assertTrue(machine.alive());
    assertTrue(machine.shutdown());
    assertFalse(machine.alive());
    assertEquals(StateMachine.notStartedState, machine.readCurrentState());
  }

  public static final class States {
    public static State aState, bState, cState;
    static {
      try {
        aState = new State("A");
        bState = new State("B");
        cState = new State("C");
      } catch (StateMachineException e) {
      }
    }
  }

  public static class TransitionNotStartedVsA extends Transition {
    public TransitionNotStartedVsA() throws StateMachineException {
      super(StateMachine.notStartedState, States.aState);
    }

    @Override
    public TransitionResult progress() {
      logger.info(StateMachine.notStartedState.getName() + "->" + States.aState.getName());
      return new TransitionResult(true, null, null);
    }

    @Override
    public TransitionResult regress() {
      logger.info(States.aState.getName() + "->" + StateMachine.notStartedState.getName());
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
