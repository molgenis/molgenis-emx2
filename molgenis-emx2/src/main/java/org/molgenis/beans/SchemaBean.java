package org.molgenis.beans;

import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.Table;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SchemaBean implements Schema {
  protected Map<String, Table> tables = new LinkedHashMap<>();

  @Override
  public Collection<Table> getTables() {
    return Collections.unmodifiableCollection(tables.values());
  }

  @Override
  public Table createTable(String name) throws MolgenisException {

    tables.put(name, new TableBean(name));
    return getTable(name);
  }

  @Override
  public Table getTable(String name) {
    return tables.get(name);
  }

  @Override
  public void dropTable(String tableId) {
    tables.remove(tableId);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Table t : tables.values()) {
      sb.append(t);
    }
    return sb.toString();
  }
}
