package com.github.statemachine;

import java.util.EmptyStackException;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.statemachine.StateMachineException.Code;

/**
 * A simple Finite State Machine.
 * 
 * Notes for users:<br>
 * 1. this FSM instance is thread-safe<br>
 * 2. it is designed to not be singleton within a process, so, if there's a desire to have many
 * state machines, just create as many as needed<br>
 * 3. for every instance of FSM, various flows are meant to be reused. There's no need to make a new
 * FSM instance for the same flow every time.<br>
 * 4. expanding on #3, if a state machine is running and going through various state transitions,
 * the FSM itself does not expect any thread affinity ( meaning the caller does not have to use the
 * same thread to change states).<br>
 * 
 * @author gaurav
 */
public final class StateMachineImpl implements StateMachine {
  private static final Logger logger = LogManager.getLogger(StateMachineImpl.class.getSimpleName());
  private final ReentrantReadWriteLock superLock = new ReentrantReadWriteLock(true);
  private final ReadLock readLock = superLock.readLock();
  private final WriteLock writeLock = superLock.writeLock();

  private final static long lockAcquisitionMillis = 100L;

  private final AtomicBoolean machineAlive = new AtomicBoolean();

  // Allow safer state rewinding
  private final static Stack<State> stateFlowStack = new Stack<>();

  // K=fromState.id:toState.id, V=Transition
  private final static ConcurrentMap<String, Transition> stateTransitionTable =
      new ConcurrentHashMap<>();

  public static State notStartedState;
  static {
    try {
      notStartedState = new State(Optional.of("INIT"));
    } catch (StateMachineException ignored) {
    }
  }

  public StateMachineImpl(final List<Transition> transitions) throws StateMachineException {
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

  @Override
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

  @Override
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

  @Override
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

  @Override
  public boolean alive() {
    return machineAlive.get();
  }

  @Override
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

  static final class TransitionUtils {
    public static String transitionId(final State fromState, State toState, boolean forward) {
      return forward ? fromState.getId() + toState.getId() : toState.getId() + fromState.getId();
    }

    /**
     * TODO: this needs to be fixed, cannot have a stateful function as a static utility - it relies
     * on a hydrated stateTransitionTable specific to a particular state machine instance.
     */
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

}

