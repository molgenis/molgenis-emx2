package org.molgenis.emx2.fairmapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class MappingScope {
  private final Map<String, Object> variables = new LinkedHashMap<>();
  private final MappingScope parent;

  public MappingScope() {
    this.parent = null;
  }

  public MappingScope(MappingScope parent) {
    this.parent = parent;
  }

  public MappingScope child() {
    return new MappingScope(this);
  }

  public void put(String name, Object value) {
    variables.put(name, value);
  }

  public Object get(String name) {
    if (variables.containsKey(name)) {
      return variables.get(name);
    }
    return parent != null ? parent.get(name) : null;
  }

  public Map<String, Object> flatten() {
    Map<String, Object> result = parent != null ? parent.flatten() : new LinkedHashMap<>();
    result.putAll(variables);
    return result;
  }
}
