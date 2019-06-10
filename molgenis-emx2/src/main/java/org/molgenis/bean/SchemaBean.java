package org.molgenis.bean;

import org.molgenis.DatabaseException;
import org.molgenis.Schema;
import org.molgenis.Table;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SchemaBean implements Schema {
  protected Map<String, Table> tables = new LinkedHashMap<>();

  @Override
  public Collection<String> getTables() {
    return Collections.unmodifiableSet(tables.keySet());
  }

  @Override
  public Table createTable(String name) throws DatabaseException {

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
}
