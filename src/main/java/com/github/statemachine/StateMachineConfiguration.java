package com.github.statemachine;

import java.util.concurrent.TimeUnit;

/**
 * This class encapsulates all the configuration parameters for the StateMachine. Use the
 * {@code StateMachineConfigurationBuilder} to build it.
 * 
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
 * 
 * @author gaurav
 */
public final class StateMachineConfiguration {
  private FlowMode flowMode;
  private RewindMode rewindMode;
  private boolean resetMachineToInitOnFailure;
  private long flowExpirationMillis;

  public boolean getResetMachineToInitOnFailure() {
    return resetMachineToInitOnFailure;
  }

  public long getFlowExpirationMillis() {
    return flowExpirationMillis;
  }

  public FlowMode getFlowMode() {
    return flowMode;
  }

  public RewindMode getRewindMode() {
    return rewindMode;
  }

  public final static class StateMachineConfigurationBuilder {
    private FlowMode flowMode;
    private RewindMode rewindMode;
    private boolean resetMachineToInitOnFailure;
    private long flowExpirationMillis;

    public static StateMachineConfigurationBuilder newBuilder() {
      return new StateMachineConfigurationBuilder();
    }

    public StateMachineConfigurationBuilder resetMachineToInitOnFailure(
        boolean resetMachineToInitOnFailure) {
      this.resetMachineToInitOnFailure = resetMachineToInitOnFailure;
      return this;
    }

    public StateMachineConfigurationBuilder flowExpirationMillis(long flowExpirationMillis) {
      this.flowExpirationMillis = flowExpirationMillis;
      return this;
    }

    public StateMachineConfigurationBuilder flowMode(final FlowMode flowMode) {
      this.flowMode = flowMode;
      return this;
    }

    public StateMachineConfigurationBuilder rewindMode(final RewindMode rewindMode) {
      this.rewindMode = rewindMode;
      return this;
    }

    public StateMachineConfiguration build() throws StateMachineException {
      final StateMachineConfiguration config = new StateMachineConfiguration(flowMode, rewindMode,
          resetMachineToInitOnFailure, flowExpirationMillis);
      config.validate();
      return config;
    }

    private StateMachineConfigurationBuilder() {}
  }

  private void validate() throws StateMachineException {
    StringBuilder messages = new StringBuilder();
    if (flowMode == null) {
      messages.append("FlowMode cannot be null. ");
    }
    if (rewindMode == null) {
      messages.append("RewindMode cannot be null. ");
    }
    if (messages.length() > 0) {
      throw new StateMachineException(StateMachineException.Code.INVALID_MACHINE_CONFIG,
          messages.toString());
    }
  }

  @Override
  public String toString() {
    return "StateMachineConfiguration [flowMode=" + flowMode + ", rewindMode=" + rewindMode
        + ", resetMachineToInitOnFailure=" + resetMachineToInitOnFailure + ", flowExpirationMillis="
        + flowExpirationMillis + "]";
  }

  private StateMachineConfiguration(final FlowMode flowMode, final RewindMode rewindMode,
      final boolean resetMachineToInitOnFailure, final long flowExpirationMillis) {
    this.resetMachineToInitOnFailure = resetMachineToInitOnFailure;
    this.flowMode = flowMode;
    this.rewindMode = rewindMode;
    if (flowExpirationMillis <= 0L) {
      this.flowExpirationMillis = TimeUnit.MINUTES.toMillis(10L);
    } else {
      this.flowExpirationMillis = flowExpirationMillis;
    }
  }

}
