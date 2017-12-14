# Finite State Machine

An implementation of a simple and hopefully elegant (from a user's perspective) FSM that allows both forward and backwards state transitions. At the moment, idempotency of state transitions is completely ignored. This could change and the machine could be enhanced to handle retries for idempotent state transitions.


# FSM Usage Manual
## Manual Sync mode
```java
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

    // 4c. execute a flow transition: B->C
    transitioned = machine.transitionTo(flowId, BtoC.getToState());

    // res4c. check that fsm is in C state
    System.out.println(String.format("fsm is in %s state", machine.readCurrentState(flowId)));

    // 5. stop the flow
    System.out.println(String.format("fsm flowId: %s is stopped: %s", flowId, machine.stopFlow(flowId)));

    // 6. stop the fsm
    System.out.println(String.format("fsm is alive: %s", machine.alive()));
    machine.demolish();
    System.out.println(String.format("fsm is alive: %s", machine.alive()));
```

## Auto Async mode
```java
```

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
