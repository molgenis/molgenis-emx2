package org.molgenis.beans;

import org.molgenis.MolgenisException;
import org.molgenis.Query;
import org.molgenis.Schema;
import org.molgenis.Table;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SchemaBean implements Schema {
  private String name;
  private Map<String, Table> tables = new LinkedHashMap<>();

  public SchemaBean(String name) {
    this.name = name;
  }

  /** for subclass to add table privately */
  public void loadTable(Table t) {
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
  public Collection<Table> getTables() throws MolgenisException {
    return Collections.unmodifiableCollection(tables.values());
  }

  @Override
  public Table createTable(String name) throws MolgenisException {

    tables.put(name, new TableBean(this, name));
    return getTable(name);
  }

  @Override
  public Table getTable(String name) throws MolgenisException {
    if (tables.containsKey(name)) return tables.get(name);
    throw new MolgenisException(String.format("Table '%s' unknown", name));
  }

  @Override
  public void grantAdmin(String user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void grantManage(String user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void grantEdit(String user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void grantView(String user) {
    throw new UnsupportedOperationException();
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
