package com.github.statemachine;

import java.util.concurrent.TimeUnit;

/**
 * This class encapsulates all the configuration parameters for the StateMachine. It is immutable
 * post-construction.
 * 
 * @author gaurav
 */
public final class StateMachineConfiguration {
  private final boolean resetMachineToInitOnFailure;
  private final long flowExpirationMillis;

  /**
   * Set a TTL for all flows of this FSM. The idea is to constrain the number of concurrent flows
   * running in an FSM. Ideally, most of the work done by a flow within an fsm will be completed and
   * {@link #stopFlow(String)} will have been called before the sweeper daemon goes about purging
   * lingering passive flows that have lastTouchTime + flowExpirationMillis < currentTime.
   * 
   * Notes:<br>
   * 1. If this is not set, a default TTL of 10 minutes will be set for all flows after which they
   * will be expired.<br>
   * 2. An upper-bound/ceiling is not yet hard-coded but if we set this to a non-sensible TTL value,
   * all those flow instances could lead to potential heap overflow.<br>
   */
  public StateMachineConfiguration(final boolean resetMachineToInitOnFailure,
      final long flowExpirationMillis) {
    this.resetMachineToInitOnFailure = resetMachineToInitOnFailure;
    if (flowExpirationMillis <= 0L) {
      this.flowExpirationMillis = TimeUnit.MINUTES.toMillis(10L);
    } else {
      this.flowExpirationMillis = flowExpirationMillis;
    }
  }

  public boolean resetMachineToInitOnFailure() {
    return resetMachineToInitOnFailure;
  }

  public long getFlowExpirationMillis() {
    return flowExpirationMillis;
  }

  @Override
  public String toString() {
    return "StateMachineConfiguration [resetMachineToInitOnFailure=" + resetMachineToInitOnFailure
        + ", flowExpirationMillis=" + flowExpirationMillis + "]";
  }

}
