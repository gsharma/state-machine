package com.github.statemachine;

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
    TRANSITION_FAILURE(
        "Failed to transition to desired state. Check exception stacktrace for more details of the failure."), MACHINE_NOT_ALIVE(
            "State machine is not running and cannot service requests"), OPERATION_LOCK_ACQUISITION_FAILURE(
                "Failed to acquire read or write lock to perform requested operation. This is retryable."), REWIND_FAILURE(
                    "Failed to rewind via unsupported RewindMode for State Machine"), INVALID_STATE_NAME(
                        "State name cannot be null or greater than " + State.maxStateNameLength
                            + " characters"), INVALID_STATE(
                                "Null state is invalid"), INVALID_TRANSITIONS(
                                    "Transitions are null or empty"), ILLEGAL_TRANSITION(
                                        "Attempted transition between from->to states is illegal");

    private String description;

    private Code(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

}
