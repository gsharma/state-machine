package com.github.statemachine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Global registry of all state machines that exist within a jvm process.
 */
final class StateMachineRegistry {
  // fugly and pathetic!! really gaurav, you couldn't do better??
  private static final ConcurrentMap<String, StateMachine> allStateMachines =
      new ConcurrentHashMap<>();

  static void register(final StateMachine stateMachine) {
    allStateMachines.putIfAbsent(stateMachine.getId(), stateMachine);
  }

  static void unregister(final String stateMachineId) {
    allStateMachines.remove(stateMachineId);
  }

  static StateMachine lookup(final String stateMachineId) {
    return allStateMachines.get(stateMachineId);
  }

  private StateMachineRegistry() {}
}
