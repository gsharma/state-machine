package com.github.statemachine;

/**
 * Unified single exception that's thrown and handled by this FSM. The idea is to use the code enum
 * to encapsulate various error/exception conditions. That said, stack traces, where available and
 * desired, are not meant to be kept from users.
 */
public final class StateMachineException extends Exception {
  private static final long serialVersionUID = 1L;
  private final Code code;

  public StateMachineException(final Code code) {
    super(code.getDescription());
    this.code = code;
  }

  public StateMachineException(final Code code, final String message) {
    super(message);
    this.code = code;
  }

  public StateMachineException(final Code code, final Throwable throwable) {
    super(throwable);
    this.code = code;
  }

  public Code getCode() {
    return code;
  }

  public static enum Code {
    // 1.
    TRANSITION_FAILURE(
        "Failed to transition to desired state. Check exception stacktrace for more details of the failure."),
    // 2.
    MACHINE_NOT_ALIVE("State machine is not running and cannot service requests"),
    // 3.
    OPERATION_LOCK_ACQUISITION_FAILURE(
        "Failed to acquire read or write lock to perform requested operation. This is retryable."),
    // 4.
    REWIND_FAILURE("Failed to rewind via unsupported RewindMode for State Machine"),
    // 5.
    INVALID_STATE_NAME(
        "State name cannot be null or greater than " + State.maxStateNameLength + " characters"),
    // 6.
    INVALID_STATE("Null state is invalid"),
    // 7.
    INVALID_TRANSITIONS("Transitions are null or empty"),
    // 8.
    ILLEGAL_TRANSITION("Attempted transition between from->to states is illegal"),
    // 9.
    INVALID_MACHINE_CONFIG("State machine configuration is invalid"),
    // 10.
    ILLEGAL_FLOW_ID("State machine failed to lookup flow with provided id"),
    // 11.
    INTERRUPTED("State machine was interrupted"),
    // 12.
    UNKNOWN_FAILURE(
        "State machine failed. Check exception stacktrace for more details of the failure");

    private String description;

    private Code(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

}
