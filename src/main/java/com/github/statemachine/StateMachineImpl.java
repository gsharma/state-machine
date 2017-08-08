package com.github.statemachine;

import java.util.EmptyStackException;
import java.util.List;
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

import com.github.statemachine.StateMachineException.Code;

/**
 * A simple Finite State Machine.
 * 
 * Notes for users:<br>
 * 1. this FSM instance is thread-safe<br>
 * 
 * 2. it is designed to not be singleton within a process, so, if there's a desire to have many
 * state machines, just create as many as needed<br>
 * 
 * 3. for every instance of FSM, various flows are meant to be reused. There's no need to make a new
 * FSM instance for the same flow every time.<br>
 * 
 * 4. expanding on #3, if a state machine is running and going through various state transitions,
 * the FSM itself does not expect any thread affinity ( meaning the caller does not have to use the
 * same thread to change states).<br>
 * 
 * 5. state transitions can be setup such that a failure of any transition in either forward or
 * backward direction triggers an auto-reset of the machine to its init state. Note that this will
 * not entail users having to rehydrate the transitions table in the machine<br>
 * 
 * 6. the State and Transition objects themselves are intended to be stateless. All state management
 * is done within the confines of the machine itself and doesn't spill out. The underlying idea is
 * that state and transition objects should be reusable across state machines eg. given states a, b,
 * c and transitions tAB, tBA, tBC, tCA, one could easily construct 2 different machines m1 and m2
 * with a subset of these states (a,b), (b,c), or (c,a).<br>
 * 
 * @author gaurav
 */
public final class StateMachineImpl implements StateMachine {
  private static final Logger logger = LogManager.getLogger(StateMachineImpl.class.getSimpleName());

  private final String machineId = UUID.randomUUID().toString();

  // dumb hardcoded value in code, oh well
  private final static long lockAcquisitionMillis = 100L;

  private final AtomicBoolean machineAlive = new AtomicBoolean();

  // K=fromState.id:toState.id, V=TransitionFunctor. This table is either fully hydrated or fully
  // dehydrated. It is never modified any differently.
  private final ConcurrentMap<String, TransitionFunctor> stateTransitionTable =
      new ConcurrentHashMap<>();

  private final ConcurrentMap<String, Flow> allFlowsTable = new ConcurrentHashMap<>();

  private final ReentrantReadWriteLock machineSuperLock = new ReentrantReadWriteLock(true);
  private final WriteLock machineWriteLock = machineSuperLock.writeLock();

  private volatile boolean resetMachineToInitOnFailure;

  public static State notStartedState;
  static {
    try {
      notStartedState = new State(Optional.of("INIT"));
    } catch (StateMachineException ignored) {
    }
  }

  public StateMachineImpl(final List<TransitionFunctor> transitionFunctors)
      throws StateMachineException {
    logInfo(machineId, null, "Firing up state machine");
    if (alive()) {
      logInfo(machineId, null, "Cannot fire up an already running state machine");
      return;
    }
    try {
      if (machineWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          if (transitionFunctors == null || transitionFunctors.isEmpty()) {
            throw new StateMachineException(Code.INVALID_TRANSITIONS);
          }
          // final List<Transition> unmodifiableTransitions = new ArrayList<>(transitions.size());
          // Collections.copy(unmodifiableTransitions, transitions);
          for (final TransitionFunctor transitionFunctor : transitionFunctors) {
            if (transitionFunctor != null) {
              stateTransitionTable.put(transitionFunctor.getForwardId(), transitionFunctor);
              stateTransitionTable.put(transitionFunctor.getReverseId(), transitionFunctor);
            }
          }
          logInfo(machineId, null,
              "Successfully hydrated stateTransitionTable: " + stateTransitionTable);

          machineAlive.set(true);
          logInfo(machineId, null, "Successfully fired up state machine, id:" + machineId);

          StateMachineRegistry.register(this);
        } finally {
          machineWriteLock.unlock();
        }
      } else {
        throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE,
            "Timed out while trying to curate state machine, id:" + machineId);
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
  }

  @Override
  public String startFlow() throws StateMachineException {
    String flowId = null;
    machineAlive();
    try {
      if (machineWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          // TODO: need to put upper-bounds on max live concurrent flows per machine
          final Flow flow = new Flow();
          flowId = flow.flowId;
          allFlowsTable.put(flowId, flow);
          pushNextState(flowId, notStartedState);
        } finally {
          machineWriteLock.unlock();
        }
      } else {
        throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE,
            "Timed out while trying to shutdown state machine");
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
    return flowId;
  }

  @Override
  public boolean stopFlow(final String flowId) throws StateMachineException {
    boolean success = false;
    machineAlive();
    try {
      if (machineWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          Flow flow = lookupFlow(flowId);
          flow.stateFlowStack.clear();
          allFlowsTable.remove(flow.flowId);
          flow = null;
          success = true;
        } finally {
          machineWriteLock.unlock();
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
  public String getId() {
    return machineId;
  }

  @Override
  public void resetMachineOnTransitionFailure(final boolean resetMachineOnTransitionFailure) {
    this.resetMachineToInitOnFailure = resetMachineOnTransitionFailure;
  }

  @Override
  public boolean demolish() throws StateMachineException {
    boolean success = false;
    logInfo(machineId, null, "Demolishing state machine, id:" + machineId);
    machineAlive();
    try {
      if (machineWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        machineAlive.set(false);
        try {
          for (Flow flow : allFlowsTable.values()) {
            stopFlow(flow.flowId);
          }
          stateTransitionTable.clear();
          StateMachineRegistry.unregister(machineId);
          logInfo(machineId, null, "Drained stateTransitionTable, reset stateFlowStack to "
              + notStartedState.getName() + " state, purged from globalStateMachineHolder");
          logInfo(machineId, null, "Successfully shut down state machine, id:" + machineId);
          success = true;
        } finally {
          machineWriteLock.unlock();
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
  public boolean transitionTo(final String flowId, final State nextState)
      throws StateMachineException {
    boolean success = false;
    machineAlive();
    try {
      final Flow flow = lookupFlow(flowId);
      if (flow.flowWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          State currentState = readCurrentState(flowId);
          if (currentState == null || nextState == null) {
            logError(machineId, flowId, String
                .format("Invalid transition between null states: %s->%s", currentState, nextState));
            return success;
          }
          if (currentState == null || nextState == null || currentState.equals(nextState)) {
            logError(machineId, flowId, String
                .format("Invalid transition between same state: %s->%s", currentState, nextState));
            return success;
          }
          currentState = popState(flowId);
          try {
            final boolean isForwardTransition =
                isForwardTransition(machineId, currentState, nextState);
            success = transitionTo(flowId, currentState, nextState, !isForwardTransition);
            if (success) {
              logInfo(machineId, flowId, String.format("Successfully transitioned from %s->%s",
                  currentState.getName(), nextState.getName()));
            }
          } finally {
            // in case of transition failure, remember to revert the stateFlowStack
            // TODO: log reverting the state of the stateFlowStack
            if (!success) {
              if (resetMachineToInitOnFailure) {
                resetMachineToInitOnTransitionFailure(flowId);
              } else {
                pushNextState(flowId, currentState);
              }
            }
          }
        } finally {
          flow.flowWriteLock.unlock();
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
  public boolean rewind(final String flowId, final RewindMode mode) throws StateMachineException {
    boolean success = false;
    machineAlive();
    try {
      final Flow flow = lookupFlow(flowId);
      if (flow.flowWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          State currentState;
          State previousState;
          switch (mode) {
            case ONE_STEP:
              // check if current state is the init not started state
              currentState = readCurrentState(flowId);
              if (currentState == null || currentState.equals(notStartedState)) {
                // TODO: log
                return success;
              }
              currentState = popState(flowId);
              previousState = popState(flowId);
              success = transitionTo(flowId, currentState, previousState, true);
              if (!success && resetMachineToInitOnFailure) {
                resetMachineToInitOnTransitionFailure(flowId);
              }
              break;
            case ALL_THE_WAY_STEP_WISE:
              // check if current state is the init not started state
              while ((currentState = readCurrentState(flowId)) != null
                  && !currentState.equals(notStartedState)) {
                currentState = popState(flowId);
                previousState = popState(flowId);
                try {
                  success = transitionTo(flowId, currentState, previousState, true);
                } finally {
                  // in case of transition failure, remember to revert the stateFlowStack
                  if (!success) {
                    if (resetMachineToInitOnFailure) {
                      resetMachineToInitOnTransitionFailure(flowId);
                    } else {
                      pushNextState(flowId, previousState);
                      pushNextState(flowId, currentState);
                    }
                    break;
                  }
                }
              }
              break;
            case ALL_THE_WAY_HARD_RESET:
              currentState = readCurrentState(flowId);
              if (currentState.equals(notStartedState)) {
                // TODO: log
                return success;
              }
              resetMachineToInitOnTransitionFailure(flowId);
              flow.stateFlowStack.clear();
              pushNextState(flowId, notStartedState);
              success = true;
              break;
            default:
              throw new StateMachineException(Code.REWIND_FAILURE);
          }
        } finally {
          flow.flowWriteLock.unlock();
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
  public State readCurrentState(final String flowId) throws StateMachineException {
    State currentState = null;
    try {
      final Flow flow = lookupFlow(flowId);
      if (flow.flowReadLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          currentState = flow.stateFlowStack.peek();
        } catch (EmptyStackException emptyStack) {
          // do nothing, returned currentState should be null
        } finally {
          flow.flowReadLock.unlock();
        }
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
    return currentState;
  }

  @Override
  public TransitionFunctor findTranstionFunctor(final String transitionId)
      throws StateMachineException {
    return stateTransitionTable.get(transitionId);
  }

  @Override
  public String printStateTransitionRoute(final String flowId) throws StateMachineException {
    final Flow flow = lookupFlow(flowId);
    return flow.stateFlowStack.toString();
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
  private boolean transitionTo(final String flowId, final State fromState, final State toState,
      boolean rewinding) throws StateMachineException {
    boolean success = false;
    Flow flow = null;
    try {
      machineAlive();
      flow = lookupFlow(flowId);
      if (!flow.flowWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE,
            "Timed out while trying to transition state machine");
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }

    try {
      if (fromState == null || toState == null) {
        // TODO: log this
        return success;
      }
      final TransitionFunctor transitionFunctor =
          stateTransitionTable.get(transitionId(fromState, toState, true));
      if (transitionFunctor != null) {
        final TransitionResult result =
            transitionFunctor.getFromState().equals(fromState) ? transitionFunctor.progress()
                : transitionFunctor.regress();
        if (result != null && result.isSuccessful()) {
          if (!rewinding) {
            pushNextState(flowId, fromState);
          }
          pushNextState(flowId, toState);
          success = true;
        } else {
          if (!rewinding) {
            logError(machineId, flowId, String.format("Failed to transition from %s to %s, %s",
                fromState, toState, result));
          } else {
            logError(machineId, flowId, String.format("Failed to transition from %s to %s, %s",
                toState, fromState, result));
          }
        }
      } else {
        if (!rewinding) {
          logError(machineId, flowId,
              String.format(
                  "Failed to lookup transition functor for state transition from %s to %s",
                  fromState, toState));
        } else {
          logError(machineId, flowId,
              String.format(
                  "Failed to lookup transition functor for state transition from %s to %s", toState,
                  fromState));
        }
      }
    } finally {
      flow.flowWriteLock.unlock();
    }
    return success;
  }

  static String transitionId(final State fromState, State toState, boolean forward) {
    return forward ? fromState.getId() + toState.getId() : toState.getId() + fromState.getId();
  }

  private void resetMachineToInitOnTransitionFailure(final String flowId)
      throws StateMachineException {
    machineAlive();
    try {
      final Flow flow = lookupFlow(flowId);
      if (flow.flowWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          flow.stateFlowStack.clear();
          pushNextState(flowId, notStartedState);
        } finally {
          flow.flowWriteLock.unlock();
        }
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
  }

  private State popState(final String flowId) throws StateMachineException {
    State nextState = null;
    try {
      final Flow flow = lookupFlow(flowId);
      if (flow.flowWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          nextState = flow.stateFlowStack.peek();
          // if (!nextState.equals(notStartedState)) {
          nextState = flow.stateFlowStack.pop();
          // }
        } catch (EmptyStackException stackIsEmpty) {
          logWarning(machineId, flowId,
              "stateFlowStack is empty, popState() has nothing to return");
        } finally {
          flow.flowWriteLock.unlock();
        }
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
    return nextState;
  }

  private void pushNextState(final String flowId, final State nextState)
      throws StateMachineException {
    try {
      final Flow flow = lookupFlow(flowId);
      if (flow.flowWriteLock.tryLock(lockAcquisitionMillis, TimeUnit.MILLISECONDS)) {
        try {
          flow.stateFlowStack.push(nextState);
        } finally {
          flow.flowWriteLock.unlock();
        }
      }
    } catch (InterruptedException exception) {
      throw new StateMachineException(Code.OPERATION_LOCK_ACQUISITION_FAILURE, exception);
    }
  }

  private void machineAlive() throws StateMachineException {
    if (!machineAlive.get()) {
      throw new StateMachineException(Code.MACHINE_NOT_ALIVE,
          "State machine id:" + machineId + " is not alive");
    }
  }

  private boolean isForwardTransition(final String stateMachineId, final State stateOne,
      final State stateTwo) throws StateMachineException {
    boolean forward = false;
    final String transitionId = transitionId(stateOne, stateTwo, true);
    final StateMachine stateMachine = StateMachineRegistry.lookup(stateMachineId);
    final TransitionFunctor transitionFunctor =
        stateMachine != null ? stateMachine.findTranstionFunctor(transitionId) : null;
    if (transitionFunctor != null) {
      forward = transitionFunctor.getForwardId().equals(transitionId);
      if (!forward) {
        if (!transitionFunctor.getReverseId().equals(transitionId)) {
          throw new StateMachineException(Code.ILLEGAL_TRANSITION);
        }
      }
    }
    return forward;
  }

  private Flow lookupFlow(final String flowId) throws StateMachineException {
    final Flow flow = allFlowsTable.get(flowId);
    if (flow == null) {
      throw new StateMachineException(Code.ILLEGAL_FLOW_ID);
    }
    return flow;
  }

  private static void logError(final String machineId, final String flowId, final String message) {
    logger.error(new StringBuilder().append("[m:").append(machineId).append("][f:").append(flowId)
        .append("] ").append(message).toString());
  }

  private static void logWarning(final String machineId, final String flowId,
      final String message) {
    logger.warn(new StringBuilder().append("[m:").append(machineId).append("][f:").append(flowId)
        .append("] ").append(message).toString());
  }

  private static void logInfo(final String machineId, final String flowId, final String message) {
    logger.info(new StringBuilder().append("[m:").append(machineId).append("][f:").append(flowId)
        .append("] ").append(message).toString());
  }

  private static void logDebug(final String machineId, final String flowId, final String message) {
    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder().append("[m:").append(machineId).append("][f:").append(flowId)
          .append("] ").append(message).toString());
    }
  }

  /**
   * In order to support multiple user threads concurrently working on the same state machine, the
   * idea is to split the actual state management into its own container that can then be handed to
   * every thread or looked up by a dedicated id even if it's used by more than a thread within the
   * context of the same "global/meta customer transaction".
   * 
   * The parent state machine itself will be configured and torn down just once but flows will get
   * initiated and torn down within the scope of every customer "transaction". Note that this split
   * means more fine-grained locking for operations that modify the per-flow state vs ones that
   * impact the overall lifecycle of the parent state machine.
   * 
   * An associated challenge with this approach is to keep flows from leaking memory. One way to
   * ensure this doesn't become a problem is by putting a sensible upper bound on the total set of
   * live flows at any given point during the lifetime of a process.
   */
  private final static class Flow {
    private final String flowId = UUID.randomUUID().toString();

    private final ReentrantReadWriteLock superFlowLock = new ReentrantReadWriteLock(true);
    private final ReadLock flowReadLock = superFlowLock.readLock();
    private final WriteLock flowWriteLock = superFlowLock.writeLock();

    // allow safer state rewinding. Note that apart from the stateFlowStack, there is no modifiable
    // transient state held by the state machine. The stateTransitionTable is either completely
    // filled or completely drained but rarely ever modified other than these 2 terminal states.
    private final Stack<State> stateFlowStack = new Stack<>();
  }

}

