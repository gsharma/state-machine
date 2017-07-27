package com.github.statemachine;

import com.github.statemachine.StateMachineException.Code;

public abstract class Transition {
  private final String forwardId;
  private final String reverseId;
  private final State fromState;
  private final State toState;

  public Transition(final State fromState, final State toState) throws StateMachineException {
    if (fromState == null || toState == null) {
      throw new StateMachineException(Code.INVALID_STATE);
    }
    this.fromState = fromState;
    this.toState = toState;
    this.forwardId = StateMachineImpl.transitionId(fromState, toState, true);
    this.reverseId = StateMachineImpl.transitionId(fromState, toState, false);
  }

  public String getForwardId() {
    return forwardId;
  }

  public String getReverseId() {
    return reverseId;
  }

  public State getFromState() {
    return fromState;
  }

  public State getToState() {
    return toState;
  }

  @Override
  public String toString() {
    return "Transition [forwardId=" + forwardId + ", reverseId=" + reverseId + ", fromState="
        + fromState + ", toState=" + toState + "]";
  }

  /**
   * The contract of progress() is quite simple - implementors should do their thing when they
   * extend it and simply encode the result of execution in the TransitionResult returned from it.
   * progress() moves the StateMachine forward: fromState->toState. If the machine is not in the
   * initial transition state (fromState), progress() will obviously fail.
   */
  public abstract TransitionResult progress();

  /**
   * The contract of regress() is quite simple - implementors should do their thing when they extend
   * it and simply encode the result of execution in the TransitionResult returned from it.
   * regress() moves the StateMachine backward: toState->fromState. If the machine is not in the
   * initial transition state (toState), regress() will obviously fail.
   */
  public abstract TransitionResult regress();
}
