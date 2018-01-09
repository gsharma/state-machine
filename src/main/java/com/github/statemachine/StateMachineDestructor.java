package com.github.statemachine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Destroys the entire FSM apparatus.
 */
final class StateMachineDestructor {
  private static final Logger logger =
      LogManager.getLogger(StateMachineDestructor.class.getSimpleName());

  StateMachineDestructor() {
    Runtime.getRuntime().addShutdownHook(new Thread("fsm-destructor") {
      @Override
      public void run() {
        StateMachineRegistry.getInstance().demolish();
      }
    });
    logger.info("Fired up state machine destructor");
  }

}
