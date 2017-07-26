package com.github.statemachine;

import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple Finite State Machine.
 * 
 * @author gaurav
 */
public final class StateMachine {
  private static final Logger logger = LogManager.getLogger(StateMachine.class.getSimpleName());
  private final ReentrantReadWriteLock superLock = new ReentrantReadWriteLock(true);
  private final ReadLock readLock = superLock.readLock();
  private final WriteLock writeLock = superLock.writeLock();

  private final static long lockAcquisitionMillis = 100L;
  private final static int maxStateNameLength = 20;

  private final AtomicBoolean machineAlive = new AtomicBoolean();

  public static State notStartedState;

  // Allow safer state rewinding
  private final static Stack<State> stateFlowStack = new Stack<>();

  // K=fromState.id:toState.id, V=Transition
  private final static ConcurrentMap<String, Transition> stateTransitionTable =
      new ConcurrentHashMap<>();

  static {
    try {
      notStartedState = new State("INIT");
    } catch (StateMachineException ignored) {
    }
  }

  public StateMachine(final List<Transition> transitions) throws StateMachineException {
    logger.info("Firing up state machine");
    if (alive()) {
      logger.info("Cannot fire up an already running state machine");
      return;
    }
    try {
      if (writeLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          if (transitions == null || transitions.isEmpty()) {
            throw new StateMachineException(Code.INVALID_TRANSITIONS);
          }
          // final List<Transition> unmodifiableTransitions = new ArrayList<>(transitions.size());
          // Collections.copy(unmodifiableTransitions, transitions);
          for (final Transition transition : transitions) {
            if (transition != null) {
              stateTransitionTable.put(transition.getForwardId(), transition);
              stateTransitionTable.put(transition.getReverseId(), transition);
            }
          }
          logger.info("Successfully hydrated stateTransitionTable: " + stateTransitionTable);
          pushNextState(notStartedState);
          machineAlive.set(true);
          logger.info("Successfully fired up state machine");
        } finally {
          writeLock.unlock();
        }
      } else {
        throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE,
            "Timed out while trying to curate state machine");
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
  }

  public static class State {
    private final String id = UUID.randomUUID().toString();
    private final String name;
    private Optional<Map<Object, Object>> customMetadata = Optional.empty();

    public State(final String name) throws StateMachineException {
      if (name == null || name.trim().length() > maxStateNameLength) {
        throw new StateMachineException(Code.INVALID_STATE_NAME);
      }
      this.name = name;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public void setCustomMetadata(final Map<Object, Object> customMetadata) {
      if (customMetadata != null && !customMetadata.isEmpty()) {
        for (Map.Entry<Object, Object> metadataEntry : customMetadata.entrySet()) {
          this.customMetadata.get().put(metadataEntry.getKey(), metadataEntry.getValue());
        }
      }
    }

    public Optional<Map<Object, Object>> getCustomMetadata() {
      return customMetadata;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      State other = (State) obj;
      if (id == null) {
        if (other.id != null) {
          return false;
        }
      } else if (!id.equals(other.id)) {
        return false;
      }
      if (name == null) {
        if (other.name != null) {
          return false;
        }
      } else if (!name.equals(other.name)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "State [id=" + id + ", name=" + name + "]";
    }
  }

  public static final class TransitionUtils {
    public static String transitionId(final State fromState, State toState, boolean forward) {
      return forward ? fromState.getId() + toState.getId() : toState.getId() + fromState.getId();
    }

    public static boolean isForwardTransition(final State stateOne, final State stateTwo)
        throws StateMachineException {
      boolean forward = false;
      final String transitionId = TransitionUtils.transitionId(stateOne, stateTwo, true);
      final Transition transition = stateTransitionTable.get(transitionId);
      if (transition != null) {
        forward = transition.getForwardId().equals(transitionId);
        if (!forward) {
          if (!transition.getReverseId().equals(transitionId)) {
            throw new StateMachineException(Code.ILLEGAL_TRANSITION);
          }
        }
      }
      return forward;
    }

    private TransitionUtils() {}
  }

  public static abstract class Transition {
    private final String forwardId;
    private final String reverseId;
    private final State fromState;
    private final State toState;

    public Transition(final State fromState, final State toState) throws StateMachineException {
      if (fromState == null || toState == null) {
        throw new StateMachineException(Code.INVALID_STATE);
      }
      this.fromState = fromState;
      this.toState = toState;
      this.forwardId = TransitionUtils.transitionId(fromState, toState, true);
      this.reverseId = TransitionUtils.transitionId(fromState, toState, false);
    }

    public String getForwardId() {
      return forwardId;
    }

    public String getReverseId() {
      return reverseId;
    }

    public State getFromState() {
      return fromState;
    }

    public State getToState() {
      return toState;
    }

    @Override
    public String toString() {
      return "Transition [forwardId=" + forwardId + ", reverseId=" + reverseId + ", fromState="
          + fromState + ", toState=" + toState + "]";
    }

    /**
     * The contract of progress() is quite simple - implementors should do their thing when they
     * extend it and simply encode the result of execution in the TransitionResult returned from it.
     * progress() moves the StateMachine forward: fromState->toState. If the machine is not in the
     * initial transition state (fromState), progress() will obviously fail.
     */
    public abstract TransitionResult progress();

    /**
     * The contract of regress() is quite simple - implementors should do their thing when they
     * extend it and simply encode the result of execution in the TransitionResult returned from it.
     * regress() moves the StateMachine backward: toState->fromState. If the machine is not in the
     * initial transition state (toState), regress() will obviously fail.
     */
    public abstract TransitionResult regress();
  }

  public static final class TransitionResult {
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

  public static enum RewindMode {
    ONE_STEP, ALL_THE_WAY_STEP_WISE, ALL_THE_WAY_HARD_RESET;
  }

  /**
   * Shutdown the state machine and clear all state flows and intermediate data structures.
   */
  public boolean shutdown() throws StateMachineException {
    boolean success = false;
    logger.info("Shutting down state machine");
    try {
      if (writeLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        if (machineAlive.compareAndSet(true, false)) {
          try {
            stateFlowStack.clear();
            pushNextState(notStartedState);
            stateTransitionTable.clear();
            logger.info("Drained stateTransitionTable, reset stateFlowStack to "
                + notStartedState.getName() + " state");
            logger.info("Successfully shut down state machine");
            success = true;
          } finally {
            writeLock.unlock();
          }
        } else {
          // was already shutdown
          logger.info("Not shutting down an already shutdown state machine");
        }
      } else {
        throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE,
            "Timed out while trying to shutdown state machine");
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
    return success;
  }

  public boolean transitionTo(final State nextState) throws StateMachineException {
    boolean success = false;
    machineAlive();
    try {
      if (writeLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          State currentState = readCurrentState();
          if (currentState == null || nextState == null || currentState.equals(nextState)) {
            logger.warn(String.format("Failed to transition from current:%s to next:%s",
                currentState, nextState));
            return success;
          }
          currentState = popState();
          try {
            boolean isForwardTransition =
                TransitionUtils.isForwardTransition(currentState, nextState);
            success = transitionTo(currentState, nextState, !isForwardTransition);
          } finally {
            // in case of transition failure, remember to revert the stateFlowStack
            // TODO: log reverting the state of the stateFlowStack
            if (!success) {
              pushNextState(currentState);
            }
          }
        } finally {
          writeLock.unlock();
        }
      } else {
        throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE,
            "Timed out while trying to transition state machine");
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
    return success;
  }

  /**
   * Rewind the state machine to either undo the last step/transition or reset it all the way to the
   * very beginning and to the NOT_STARTED state.
   * 
   * @param mode
   * @return
   */
  public boolean rewind(final RewindMode mode) throws StateMachineException {
    boolean success = false;
    machineAlive();
    try {
      if (writeLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          State currentState;
          State previousState;
          switch (mode) {
            case ONE_STEP:
              // check if current state is the init not started state
              currentState = readCurrentState();
              if (currentState == null || currentState.equals(notStartedState)) {
                // TODO: log
                return success;
              }
              currentState = popState();
              previousState = popState();
              success = transitionTo(currentState, previousState, true);
              break;
            case ALL_THE_WAY_STEP_WISE:
              // check if current state is the init not started state
              while ((currentState = readCurrentState()) != null
                  && !currentState.equals(notStartedState)) {
                currentState = popState();
                previousState = popState();
                try {
                  success = transitionTo(currentState, previousState, true);
                } finally {
                  // in case of transition failure, remember to revert the stateFlowStack
                  if (!success) {
                    pushNextState(previousState);
                    pushNextState(currentState);
                    break;
                  }
                }
              }
              break;
            case ALL_THE_WAY_HARD_RESET:
              currentState = readCurrentState();
              if (currentState.equals(notStartedState)) {
                // TODO: log
                return success;
              }
              stateFlowStack.clear();
              pushNextState(notStartedState);
              success = true;
              break;
            default:
              throw new StateMachineException(Code.REWIND_FAILURE);
          }
        } finally {
          writeLock.unlock();
        }
      } else {
        throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE,
            "Timed out while trying to rewind state machine");
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
    return success;
  }

  public boolean alive() {
    return machineAlive.get();
  }

  public State readCurrentState() throws StateMachineException {
    State currentState = null;
    try {
      if (readLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          currentState = stateFlowStack.peek();
        } finally {
          readLock.unlock();
        }
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
    return currentState;
  }

  /**
   * Transitions the machine from->to states. Note that if the transition is successful, the
   * stateFlowStack will have the toState at the top reflecting the current state of the machine.
   * 
   * Callers should remember to:<br/>
   * 1. pop both fromState and toState states from the stateFlowStack before calling this
   * function.<br/>
   * 2. in case of a return value of false, push the fromState back on the stateFlowStack
   */
  private boolean transitionTo(final State fromState, final State toState, boolean rewinding)
      throws StateMachineException {
    boolean success = false;
    machineAlive();
    try {
      if (writeLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        if (fromState == null || toState == null) {
          // TODO: log this
          return success;
        }
        try {
          final Transition transition =
              stateTransitionTable.get(TransitionUtils.transitionId(fromState, toState, true));
          if (transition != null) {
            final TransitionResult result =
                transition.getFromState().equals(fromState) ? transition.progress()
                    : transition.regress();
            if (result != null && result.isSuccessful()) {
              if (!rewinding) {
                pushNextState(fromState);
              }
              pushNextState(toState);
              success = true;
            } else {
              if (!rewinding) {
                logger.error(String.format("Failed to transition from %s to %s, %s", fromState,
                    toState, result));
              } else {
                logger.error(String.format("Failed to transition from %s to %s, %s", toState,
                    fromState, result));
              }
            }
          } else {
            if (!rewinding) {
              logger.error(String.format("Failed to transition from %s to %s", fromState, toState));
            } else {
              logger.error(String.format("Failed to transition from %s to %s", toState, fromState));
            }
          }
        } finally {
          writeLock.unlock();
        }
      } else {
        throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE,
            "Timed out while trying to transition state machine");
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
    return success;
  }

  private State popState() throws StateMachineException {
    State nextState = null;
    try {
      if (writeLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          nextState = stateFlowStack.peek();
          // if (!nextState.equals(notStartedState)) {
          nextState = stateFlowStack.pop();
          // }
        } catch (EmptyStackException stackIsEmpty) {
          // stack is finally empty, nothing to return
        } finally {
          writeLock.unlock();
        }
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
    return nextState;
  }

  private void pushNextState(final State nextState) throws StateMachineException {
    try {
      if (writeLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          stateFlowStack.push(nextState);
        } finally {
          writeLock.unlock();
        }
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
  }

  private void machineAlive() throws StateMachineException {
    if (!machineAlive.get()) {
      throw new StateMachineException(Code.MACHINE_NOT_ALIVE);
    }
  }

  public static final class StateMachineException extends Exception {
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

  }

  public static enum Code {
    TRANSITION_FAILURE(
        "Failed to transition to desired state. Check exception stacktrace for more details of the failure."), MACHINE_NOT_ALIVE(
            "State machine is not running and cannot service requests"), OPERATION_LOCK_ACQUISITION_FAILURE(
                "Failed to acquire read or write lock to perform requested operation. This is retryable."), REWIND_FAILURE(
                    "Failed to rewind via unsupported RewindMode for State Machine"), INVALID_STATE_NAME(
                        "State name cannot be null or greater than " + maxStateNameLength
                            + " characters"), INVALID_STATE(
                                "Null state is invalid"), INVALID_TRANSITIONS(
                                    "Transition is null or empty"), ILLEGAL_TRANSITION(
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

