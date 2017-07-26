package com.github.statemachine;

public final class TransitionResult {
  private final String description;
  private final StateMachineException error;
  private final boolean successful;

  public TransitionResult(final boolean successful, final String description,
      final StateMachineException error) {
    this.successful = successful;
    this.description = description;
    this.error = error;
  }

  public String getDescription() {
    return description;
  }

  public StateMachineException getError() {
    return error;
  }

  public boolean isSuccessful() {
    return successful;
  }

  @Override
  public String toString() {
    return "TransitionResult [description=" + description + ", error=" + error + ", successful="
        + successful + "]";
  }
}