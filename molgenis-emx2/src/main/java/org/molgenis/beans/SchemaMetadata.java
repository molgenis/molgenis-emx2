package org.molgenis.beans;

import org.molgenis.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SchemaMetadata implements Schema {
  private String name;
  protected Map<String, Table> tables = new LinkedHashMap<>();

  public SchemaMetadata(String name) {
    this.name = name;
  }

  /** for subclass to add table privately */
  protected void loadTable(Table t) {
    tables.put(t.getName(), t);
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Collection<String> getTableNames() throws MolgenisException {
    return Collections.unmodifiableCollection(tables.keySet());
  }

  @Override
  public Table createTableIfNotExists(String name) throws MolgenisException {
    try {
      return getTable(name);
    } catch (Exception e) {
      tables.put(name, new TableMetadata(this, name));
      return getTable(name);
    }
  }

  @Override
  public Table getTable(String name) throws MolgenisException {
    Table table = tables.get(name);
    if (table == null)
      throw new MolgenisException(
          "undefined_table",
          "Table not found",
          String.format("Table with name='%s' could not be found", name));
    return table;
  }

  public Boolean tableIsCached(String name) {
    return tables.containsKey(name);
  }

  @Override
  public void dropTable(String tableId) throws MolgenisException {
    tables.remove(tableId);
  }

  @Override
  public void grant(Permission permission, String role) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void revokePermission(Permission permission, String user) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Query query(String tableName) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Table t : tables.values()) {
      sb.append(t);
    }
    return sb.toString();
  }
}
