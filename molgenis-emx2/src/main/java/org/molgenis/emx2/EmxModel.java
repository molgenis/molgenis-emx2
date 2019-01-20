package org.molgenis.emx2;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.metamodel.annotation.TypeName;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeName("model")
public class EmxModel {

  protected Map<String, EmxTable> tables = new LinkedHashMap<>();

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

  public EmxTable createTable(String name) throws EmxException {
    EmxTable table = new EmxTable(this, name);
    tables.put(name, table);
    this.onTableChange(table);
    return table;
  }

  public void removeTable(String name) throws EmxException {
    tables.remove(name);
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

  public String diff(EmxModel otherModel) {
    Javers javers = JaversBuilder.javers().build();
    Diff diff = javers.compare(this, otherModel);
    return (diff.toString());
  }

  protected void onTableChange(EmxTable table) throws EmxException {
    // empty on purpose to allow subclass to add handler
  }

  protected void onColumnChange(EmxColumn column) throws EmxException {
    // empty on purpose to allow subclass to add handler
  }

  protected void onColumnRemove(EmxColumn column) throws EmxException {
    // empty on purpose to allow subclass to add handler
  }
}
