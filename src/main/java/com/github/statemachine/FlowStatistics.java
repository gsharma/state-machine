package com.github.statemachine;

import java.util.concurrent.TimeUnit;

/**
 * Simple statistics holder for a flow.
 */
public final class FlowStatistics {
  private final long startMillis = System.currentTimeMillis();
  String flowId;
  int successes;
  int failures;
  // used to track activity level of a flow
  long lastTouchTimeMillis;

  public String getFlowId() {
    return flowId;
  }

  public int getSuccesses() {
    return successes;
  }

  public int getFailures() {
    return failures;
  }

  public long getAliveTimeSeconds() {
    return TimeUnit.SECONDS.convert(System.currentTimeMillis() - startMillis,
        TimeUnit.MILLISECONDS);
  }

  @Override
  public String toString() {
    return "FlowStatistics [flowId=" + flowId + ", startMillis=" + startMillis + ", successes="
        + successes + ", failures=" + failures + ", lastTouchTimeMillis=" + lastTouchTimeMillis
        + ", aliveTimeSeconds=" + getAliveTimeSeconds() + "]";
  }

}
