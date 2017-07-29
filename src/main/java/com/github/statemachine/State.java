package com.github.statemachine;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.github.statemachine.StateMachineException.Code;

/**
 * This object represents immutable metadata about a state.
 */
public final class State {
  // auto-generated
  private final String id = UUID.randomUUID().toString();

  // name and customMetadata are optional
  final static int maxStateNameLength = 20;
  private String name = "UNDEF"; // optional
  private Optional<Map<Object, Object>> customMetadata = Optional.empty();

  /**
   * State name is optional
   */
  public State(final Optional<String> name) throws StateMachineException {
    if (name != null && name.isPresent()) {
      if (name.get().trim().length() <= maxStateNameLength) {
        this.name = name.get().trim();
      } else {
        throw new StateMachineException(Code.INVALID_STATE_NAME);
      }
    }
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
