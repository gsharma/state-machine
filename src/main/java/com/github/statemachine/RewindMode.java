package com.github.statemachine;

/**
 * This represents the mode to be used by the state machine when working to transition states in
 * backwards direction.
 */
public enum RewindMode {
  // rewind backwards one step only
  ONE_STEP,
  // rewind backwards all the way to INIT state but transition step-wise
  ALL_THE_WAY_STEP_WISE,
  // rewind backwards all the way abruptly without trying to transition between individual states,
  // instead reset and go to INIT at once
  ALL_THE_WAY_HARD_RESET;
}
