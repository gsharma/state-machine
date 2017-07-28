package com.github.statemachine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Global registry of all state machines that exist within a jvm process.
 */
final class StateMachineRegistry {
  private static final ConcurrentMap<String, StateMachine> allStateMachines =
      new ConcurrentHashMap<>();

  static void addMachine(final StateMachine stateMachine) {
    allStateMachines.putIfAbsent(stateMachine.getId(), stateMachine);
  }

  static void dropMachine(final String stateMachineId) {
    allStateMachines.remove(stateMachineId);
  }

  static StateMachine lookupMachine(final String stateMachineId) {
    return allStateMachines.get(stateMachineId);
  }

  private StateMachineRegistry() {}
}
