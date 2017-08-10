package com.github.statemachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.statemachine.StateMachineImpl.Flow;

/**
 * Holder of statistics for this FSM and all its child flows. Child flow stats are pre-cached for
 * sometime and/or lazily recomputed, as needed.
 */
public final class StateMachineStatistics {
  private final String stateMachineId;

  StateMachineStatistics(final String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  private final long startTstampMillis = System.currentTimeMillis();
  private final List<FlowStatistics> latestActiveFlowStats = new ArrayList<>();
  int totalStartedFlows;
  int totalStoppedFlows;
  private long lastComputedMillis;
  private final long recomputeIntervalMillis = 60 * 1000L;

  public long getStartTimeMillis() {
    return startTstampMillis;
  }

  public String getMachineId() {
    return stateMachineId;
  }

  /**
   * Report stats for all active flows that exist within an fsm.
   */
  public synchronized List<FlowStatistics> getActiveFlowStats() {
    if (lastComputedMillis == 0L
        || (lastComputedMillis + recomputeIntervalMillis < System.currentTimeMillis())) {
      final List<FlowStatistics> activeFlowStats = new ArrayList<>();
      final StateMachine stateMachine = StateMachineRegistry.getInstance().lookup(stateMachineId);
      for (final Flow flow : ((StateMachineImpl) stateMachine).allFlowsTable.values()) {
        activeFlowStats.add(flow.flowStats);
      }
      latestActiveFlowStats.clear();
      latestActiveFlowStats.addAll(activeFlowStats);
      lastComputedMillis = System.currentTimeMillis();
    }
    return Collections.unmodifiableList(latestActiveFlowStats);
  }

  public long getLastComputedMillis() {
    return lastComputedMillis;
  }

  public int getTotalStartedFlows() {
    return totalStartedFlows;
  }

  public int getTotalStoppedFlows() {
    return totalStoppedFlows;
  }

  @Override
  public String toString() {
    return "StateMachineStatistics [stateMachineId=" + stateMachineId + ", startTstampMillis="
        + startTstampMillis + ", totalStartedFlows=" + totalStartedFlows + ", totalStoppedFlows="
        + totalStoppedFlows + ", lastComputedMillis=" + lastComputedMillis + ", activeFlowStats="
        + latestActiveFlowStats + "]";
  }

}
