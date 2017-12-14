# Finite State Machine

An implementation of a simple and hopefully elegant (from a user's perspective) FSM that allows both forward and backwards state transitions. At the moment, idempotency of state transitions is completely ignored. This could change and the machine could be enhanced to handle retries for idempotent state transitions.


# FSM Usage Manual
## Manual Sync mode
This function illustrates how the FSM can be manually transitioned through its states. This mode provides total control over all transitions.
```java
public void manualSyncMode() throws StateMachineException {
  // helper code for states and transitions is farther down
  // op1. prep transitions
  final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
  final TransitionAVsB AtoB = new TransitionAVsB();
  final TransitionBVsC BtoC = new TransitionBVsC();

  // op2. load up the fsm with all its transitions
  final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
      .flowMode(FlowMode.MANUAL).rewindMode(RewindMode.ALL_THE_WAY_HARD_RESET)
      .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
  final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
      .next(toA).next(AtoB).next(BtoC).build();
  
  // res2. check that fsm should be alive
  System.out.println(String.format("fsm is alive: %s", machine.alive()));

  // op3. start a flow
  final String flowId = machine.startFlow();

  // res3. check that fsm is in INIT
  System.out.println(String.format("fsm is in %s state", machine.readCurrentState(flowId)));

  // op4a. execute a flow transition: INIT->A
  boolean transitioned = machine.transitionTo(flowId, toA.getToState());

  // res4a. check that fsm is in A state
  System.out.println(String.format("fsm is in %s state", machine.readCurrentState(flowId)));

  // op4b. execute a flow transition: A->B
  transitioned = machine.transitionTo(flowId, AtoB.getToState());

  // res4b. check that fsm is in B state
  System.out.println(String.format("fsm is in %s state", machine.readCurrentState(flowId)));

  // op4c. execute a flow transition: B->C
  transitioned = machine.transitionTo(flowId, BtoC.getToState());

  // res4c. check that fsm is in C state
  System.out.println(String.format("fsm is in %s state", machine.readCurrentState(flowId)));

  // op5. stop the flow
  System.out.println(String.format("fsm flowId: %s is stopped: %s", flowId, machine.stopFlow(flowId)));

  // op6. stop the fsm
  System.out.println(String.format("fsm is alive: %s", machine.alive()));
  machine.demolish();
  System.out.println(String.format("fsm is alive: %s", machine.alive()));
}
```

## Auto Async mode
The auto async mode enables automatic transition of the FSM through its states but in a background thread. There is another option to allow doing it on the caller thread itself via the FlowMode.AUTO_CALLER_THREAD
```java
public void autoAsyncMode() throws Exception {
  // op1. prep transitions
  final TransitionNotStartedVsA toA = new TransitionNotStartedVsA();
  final TransitionAVsB AtoB = new TransitionAVsB();
  final TransitionBVsC BtoC = new TransitionBVsC();

  // op2. load up the fsm with all its transitions
  final StateMachineConfiguration config = StateMachineConfigurationBuilder.newBuilder()
      .flowMode(FlowMode.AUTO_ASYNC).rewindMode(RewindMode.ALL_THE_WAY_HARD_RESET)
      .resetMachineToInitOnFailure(true).flowExpirationMillis(0).build();
  final StateMachine machine = StateMachineBuilder.newBuilder().config(config).transitions()
      .next(toA).next(AtoB).next(BtoC).build();

  // res2. check that fsm should be alive
  System.out.println(String.format("fsm is alive: %s", machine.alive()));

  // op3. start a flow
  final String flowId = machine.startFlow();

  // op4. give it a lil breather to finish running
  Thread.sleep(10L);

  // op5. stop the fsm, flow will auto-stop
  System.out.println(String.format("fsm is alive: %s", machine.alive()));
  machine.demolish();
  System.out.println(String.format("fsm is alive: %s", machine.alive()));
}
```

## A note about the RewindMode
Failure cases during state transitions present the FSM with options to rewind and proceed in the opposite direction. 3 rewind modes are available:
1. ONE_STEP - rewind backwards one step only
2. ALL_THE_WAY_STEP_WISE - rewind backwards all the way to INIT state but transition step-wise
3. ALL_THE_WAY_HARD_RESET - rewind backwards all the way abruptly without trying to transition between individual states; effectively reset to INIT in one shot

## Helper code for examples
```java
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
```
