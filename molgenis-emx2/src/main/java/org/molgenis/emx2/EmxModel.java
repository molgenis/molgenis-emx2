package org.molgenis.emx2;

import org.javers.core.metamodel.annotation.TypeName;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeName("model")
public class EmxModel {

  private Map<String, EmxTable> tables = new LinkedHashMap<>();

  public EmxModel() {}

  public EmxModel(Map<String, EmxTable> tables) {
    this.tables = tables;
  }

  public Collection<String> getTableNames() {
    return Collections.unmodifiableSet(tables.keySet());
  }

  public EmxTable getTable(String name) {
    return tables.get(name);
  }

  public Collection<EmxTable> getTables() {
    return Collections.unmodifiableCollection(tables.values());
  }

  public EmxTable addTable(String name) throws EmxException {
    EmxTable table = new EmxTable(this, name);
    tables.put(name, table);
    return table;
  }

  public String print() {
    StringBuilder builder = new StringBuilder();
    builder.append("EmxModel(");

    for (EmxTable table : tables.values()) {
      builder.append("\n").append(table.print());
    }

    builder.append("\n);");

    return builder.toString();
  }
}
