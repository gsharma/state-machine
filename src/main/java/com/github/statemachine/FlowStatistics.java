package com.github.statemachine;

import java.util.ArrayDeque;
import java.util.Deque;

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
  // bounded at 100; yay, hard-coded shit
  final Deque<StateTimePair> boundedStateRoute = new ArrayDeque<>();

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

  public final static class StateTimePair {
    public String stateId;
    public long startMillis;
    public long elapsedMillis;

    @Override
    public String toString() {
      return "StateTimePair [stateId=" + stateId + ", elapsedMillis=" + elapsedMillis + "]";
    }
  }

}
