package com.github.statemachine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Global registry of all state machines that exist within a jvm process.
 */
final class StateMachineRegistry {
  private static final Logger logger =
      LogManager.getLogger(StateMachineRegistry.class.getSimpleName());

  private final AtomicBoolean alive = new AtomicBoolean();

  // fugly and pathetic!! really gaurav, you couldn't do better??
  private static ConcurrentMap<String, StateMachine> allStateMachines = new ConcurrentHashMap<>();

  private GlobalStatsDaemon statsGatherer;
  private final static long statsGathererSleepMillis = 300 * 1000L;

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

  synchronized void demolish() {
    if (alive != null && !alive.get()) {
      logger.info("Global state machine registry is already shutdown");
      return;
    }
    logger.info("Shutting down global state machine registry");
    try {
      statsGatherer.interrupt();
      statsGatherer.join();
      statsGatherer = null;
    } catch (InterruptedException ignore) {
    }
    for (final StateMachine stateMachine : allStateMachines.values()) {
      try {
        if (stateMachine.alive()) {
          logger.info(stateMachine.getStatistics());
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
    alive.set(false);
    logger.info("Successfully shut down global state machine registry");
  }

  @Override
  public void finalize() {
    logger.info("Garbage collected stopped state machine registry");
  }

  private StateMachineRegistry() {
    statsGatherer = new GlobalStatsDaemon();
    statsGatherer.start();
    new StateMachineDestructor();
    alive.set(true);
    logger.info("Fired up global state machine registry");
  }

  /**
   * Daemon to periodically wake up and gather all FSM stats and dump them to log.
   */
  private final static class GlobalStatsDaemon extends Thread {
    private GlobalStatsDaemon() {
      setName("stats-gatherer");
      setDaemon(true);
      logger.info("Fired up global stats daemon");
    }

    @Override
    public void run() {
      while (!isInterrupted()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Global stats daemon woke up to gather all machines' stats");
        }
        StringBuilder builder = new StringBuilder("Global state machine statistics");
        for (final StateMachine stateMachine : allStateMachines.values()) {
          if (stateMachine != null) {
            builder.append("\n    ").append(stateMachine.getStatistics().toString());
          }
        }
        logger.info(builder.toString());
        try {
          Thread.sleep(statsGathererSleepMillis);
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
        }
      }
      logger.info("Successfully shut down global stats daemon");
    }
  }

}
