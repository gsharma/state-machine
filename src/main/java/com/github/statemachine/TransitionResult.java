package com.github.statemachine;

/**
 * This object encapsulates the result of application of TransitionFunctor to transition a
 * StateMachine between a State pair.
 * 
 * Typically, successes are encoded with {@link #successful} being set to true. Failures are
 * expected to report {@link #isSuccessful()} as false and typically carry an associated
 * {@link #error}. {@link #description} is optional.
 * 
 * Users should not try to sub-class and extend this, it would serve little purpose.
 */
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
