package com.github.statemachine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Global registry of all state machines that exist within a jvm process.
 */
final class StateMachineRegistry {
  private static final Logger logger =
      LogManager.getLogger(StateMachineRegistry.class.getSimpleName());

  // fugly and pathetic!! really gaurav, you couldn't do better??
  private static ConcurrentMap<String, StateMachine> allStateMachines = new ConcurrentHashMap<>();

  private static final StateMachineRegistry instance = new StateMachineRegistry();

  static StateMachineRegistry getInstance() {
    return instance;
  }

  void register(final StateMachine stateMachine) {
    allStateMachines.putIfAbsent(stateMachine.getId(), stateMachine);
  }

  void unregister(final String stateMachineId) {
    StateMachine stateMachine = allStateMachines.remove(stateMachineId);
    stateMachine = null;
  }

  StateMachine lookup(final String stateMachineId) {
    return allStateMachines.get(stateMachineId);
  }

  @Override
  public void finalize() {
    logger.info("Garbage collected stopped state machine registry");
  }

  private StateMachineRegistry() {
    Runtime.getRuntime().addShutdownHook(new Thread("Registry-Purger") {
      @Override
      public void run() {
        logger.info("Shutting down global state machine registry");
        for (final StateMachine stateMachine : allStateMachines.values()) {
          try {
            if (stateMachine.alive()) {
              stateMachine.demolish();
            }
          } catch (StateMachineException problem) {
            logger.error(
                "Problem occurred while running demolish() as part of the shutdown hook sequence.",
                problem);
          }
        }
        allStateMachines.clear();
        allStateMachines = null;
        logger.info("Successfully shut down global state machine registry");
      }
    });
  }

}
