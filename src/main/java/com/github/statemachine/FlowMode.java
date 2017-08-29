package com.github.statemachine;

/**
 * This represents the mode to be used by the state machine when working to transition states.
 */
public enum FlowMode {
  // auto progress through transitions asynchronously on a thread different from the caller thread.
  // Note that AUTO_ASYNC automatically calls Flow.stopFlow()
  AUTO_ASYNC,
  // auto progress through transitions on the caller thread.
  AUTO_CALLER_THREAD,
  // manually progress through transition.
  MANUAL;
}
