package com.github.statemachine;

/**
 * Simple statistics holder for a flow.
 */
public final class FlowStatistics {
  private final long startMillis = System.currentTimeMillis();
  String flowId;
  int transitionSuccesses;
  int transitionFailures;
  // used to track activity level of a flow
  long lastTouchTimeMillis;

  public String getFlowId() {
    return flowId;
  }

  public int getTransitionSuccesses() {
    return transitionSuccesses;
  }

  public int getTransitionFailures() {
    return transitionFailures;
  }

  public long getAliveTimeMillis() {
    return System.currentTimeMillis() - startMillis;
  }

  @Override
  public String toString() {
    return "FlowStatistics [flowId=" + flowId + ", transitionSuccesses=" + transitionSuccesses
        + ", transitionFailures=" + transitionFailures + ", lastTouchTimeMillis="
        + lastTouchTimeMillis + ", aliveTimeMillis=" + getAliveTimeMillis() + "]";
  }

}
