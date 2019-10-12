package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import java.util.*;

public class SchemaMetadata {

  private String name;
  protected Map<String, TableMetadata> tables = new LinkedHashMap<>();

  public SchemaMetadata() {}

  public SchemaMetadata(String name) {
    this.name = name;
  }

  /** for subclass to add table privately */
  protected void loadTable(TableMetadata t) {
    tables.put(t.getTableName(), t);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Collection<String> getTableNames() {
    return Collections.unmodifiableCollection(tables.keySet());
  }

  public TableMetadata createTableIfNotExists(String name) {
    try {
      return getTableMetadata(name);
    } catch (Exception e) {
      tables.put(name, new TableMetadata(this, name));
      return getTableMetadata(name);
    }
  }

  public TableMetadata createTable(TableMetadata table) {
    if (tables.containsKey(table.getTableName()))
      throw new MolgenisException(
          "create_table_failed",
          "Create table failed",
          "Table with name '"
              + table.getTableName()
              + "'already exists in schema '"
              + getName()
              + "'");
    this.tables.put(table.getTableName(), table);
    return table;
  }

  public TableMetadata getTableMetadata(String name) {
    TableMetadata table = tables.get(name);
    if (table == null)
      throw new MolgenisException(
          "undefined_table",
          "Table not found",
          "Table with name='" + name + "' could not be found");
    return table;
  }

  public void dropTable(String tableId) {
    tables.remove(tableId);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (TableMetadata t : tables.values()) {
      sb.append(t);
    }
    return sb.toString();
  }
}
