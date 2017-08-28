package com.github.statemachine;

/**
 * This represents the mode to be used by the state machine when working to transition states.
 */
public enum FlowMode {
  // auto progress through transitions
  AUTO,
  // manually progress through transition
  MANUAL;
}
