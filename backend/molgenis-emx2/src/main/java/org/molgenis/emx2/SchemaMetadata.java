package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.OIDC_CALLBACK_PATH;
import static org.molgenis.emx2.Constants.OIDC_LOGIN_PATH;

import java.util.*;
import org.molgenis.emx2.utils.TableSort;

public class SchemaMetadata extends HasSettings<SchemaMetadata> {

  protected Map<String, TableMetadata> tables = new LinkedHashMap<>();
  protected String name;
  // optional
  protected String description;
  // optional
  protected Database database;

  public SchemaMetadata() {}

  public SchemaMetadata(String name) {
    validateSchemaName(name);
    this.name = name;
  }

  public SchemaMetadata(String name, String description) {
    this(name);
    this.description = description;
  }

  public SchemaMetadata(SchemaMetadata schema) {
    this.name = schema.getName();
    this.description = schema.getDescription();
    this.database = schema.getDatabase();
    this.setSettingsWithoutReload(schema.getSettings());
  }

  public SchemaMetadata(Database db, SchemaMetadata schema) {
    this.name = schema.getName();
    this.description = schema.getDescription();
    this.database = db;
    this.setSettingsWithoutReload(schema.getSettings());
  }

  private void validateSchemaName(String name) {
    if (name == null || name.isEmpty())
      throw new MolgenisException("Create schema failed: Schema name was null or empty");
    if (name.equalsIgnoreCase(OIDC_LOGIN_PATH) || name.equalsIgnoreCase(OIDC_CALLBACK_PATH))
      throw new MolgenisException(String.format("Schema name: '%s' is a reserved word", name));
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Set<String> getTableNames() {
    return this.tables.keySet();
  }

  public TableMetadata create(TableMetadata table) {
    if (tables.get(table.getTableName()) != null)
      throw new MolgenisException(
          "Create table failed: Table with name '"
              + table.getTableName()
              + "'already exists in schema '"
              + getName()
              + "'");
    this.tables.put(table.getTableName(), table);
    table.setSchema(this);
    return table;
  }

  public SchemaMetadata create(TableMetadata... tables) {
    for (TableMetadata table : tables) {
      this.create(table);
    }
    return this;
  }

  public TableMetadata getTableMetadata(String name) {
    return tables.get(name);
  }

  public void drop(String tableId) {
    tables.remove(tableId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (TableMetadata t : tables.values()) {
      sb.append(t);
    }
    return sb.toString();
  }

  public List<TableMetadata> getTables() {
    List<TableMetadata> result = new ArrayList<>();
    for (String tableName : getTableNames()) {
      result.add(getTableMetadata(tableName));
    }
    TableSort.sortTableByDependency(result);
    return result;
  }

  public Database getDatabase() {
    return database;
  }

  public void setDatabase(Database database) {
    this.database = database;
  }

  public List<TableMetadata> getTablesIncludingExternal() {
    Map<String, TableMetadata> tables = new LinkedHashMap<>();
    for (String tableName : getTableNames()) {
      tables.put(tableName, getTableMetadata(tableName));
    }
    // add exteral references recursively
    for (String tableName : getTableNames()) {
      addExternalTablesRecursive(tables, getTableMetadata(tableName));
    }

    return new ArrayList<>(tables.values());
  }

  private void addExternalTablesRecursive(
      Map<String, TableMetadata> tables, TableMetadata current) {
    if (current.getInheritedTable() != null && !tables.containsKey(current.getInherit())) {
      tables.put(current.getInherit(), current.getInheritedTable());
      addExternalTablesRecursive(tables, current.getInheritedTable());
    }
    for (Column c : current.getColumns()) {
      if (c.isReference() && !tables.containsKey(c.getRefTableName())) {
        tables.put(c.getRefTableName(), c.getRefTable());
        addExternalTablesRecursive(tables, c.getRefTable());
      }
    }
  }

  public Set<String> getLocales() {
    // sorted on alphabet
    Set<String> result = new TreeSet<>();
    getTables()
        .forEach(
            table -> {
              table
                  .getColumns()
                  .forEach(
                      column -> {
                        result.addAll(column.getColumnLabels().keySet());
                      });
            });
    return result;
  }
}
