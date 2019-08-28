package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SchemaMetadata {

  private String name;
  protected Map<String, TableMetadata> tables = new LinkedHashMap<>();

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

  public TableMetadata createTableIfNotExists(String name) throws MolgenisException {
    try {
      return getTableMetadata(name);
    } catch (Exception e) {
      tables.put(name, new TableMetadata(this, name));
      return getTableMetadata(name);
    }
  }

  public TableMetadata getTableMetadata(String name) throws MolgenisException {
    TableMetadata table = tables.get(name);
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

  public void dropTable(String tableId) throws MolgenisException {
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
