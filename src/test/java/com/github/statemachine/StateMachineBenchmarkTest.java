package com.github.statemachine;

import org.openjdk.jmh.annotations.Benchmark;

import com.github.statemachine.StateMachine.StateMachineBuilder;
import com.github.statemachine.StateMachineConfiguration.StateMachineConfigurationBuilder;
import com.github.statemachine.StateMachineTest.TransitionAVsB;
import com.github.statemachine.StateMachineTest.TransitionBVsC;
import com.github.statemachine.StateMachineTest.TransitionNotStartedVsA;

public class StateMachineBenchmarkTest {

  @Benchmark
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
    boolean alive = machine.alive();

    // 3. start a flow
    final String flowId = machine.startFlow();
    State currentState = machine.readCurrentState(flowId);

    // 4a. execute a flow transition
    // INIT->A
    boolean transitioned = machine.transitionTo(flowId, toA.getToState());
    currentState = machine.readCurrentState(flowId);

    // 4b. execute a flow transition
    // A->B
    transitioned = machine.transitionTo(flowId, AtoB.getToState());
    currentState = machine.readCurrentState(flowId);

    // 4c. execute a flow transition
    // B->C
    transitioned = machine.transitionTo(flowId, BtoC.getToState());
    currentState = machine.readCurrentState(flowId);

    // 5. stop the flow
    boolean stopped = machine.stopFlow(flowId);

    // 6. stop the fsm
    alive = machine.alive();
    stopped = machine.demolish();
    alive = machine.alive();
  }

  public static void main(String args[]) throws StateMachineException {
    StateMachineBenchmarkTest test = new StateMachineBenchmarkTest();
    test.testStateMachineFlow();
  }

}
