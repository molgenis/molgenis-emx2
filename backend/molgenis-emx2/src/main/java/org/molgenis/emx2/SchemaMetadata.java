package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.OIDC_CALLBACK_PATH;
import static org.molgenis.emx2.Constants.OIDC_LOGIN_PATH;

import java.util.*;
import org.molgenis.emx2.utils.TableSort;

public class SchemaMetadata extends HasSettings<SchemaMetadata> {

  private static final String SCHEMA_NAME_MESSAGE =
      "': Schema name must start with a letter or underscore, followed by letters, underscores, or numbers, i.e. [a-zA-Z][a-zA-Z0-9_]*. Maximum length: 31 characters (so it fits in Excel sheet names)";
  protected Map<String, TableMetadata> tables = new LinkedHashMap<>();
  protected String name;
  // optional
  protected String description;
  // optional
  protected Database database;

  public SchemaMetadata() {}

  public SchemaMetadata(String name) {
    this.name = validateSchemaName(name);
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

  private String validateSchemaName(String name) {
    if (name == null || name.isEmpty())
      throw new MolgenisException("Create schema failed: Schema name was null or empty");
    if (!name.matches("[a-zA-Z_-][a-zA-Z0-9_-]*")) {
      throw new MolgenisException("Invalid schema name '" + name + SCHEMA_NAME_MESSAGE);
    }
    if (name.equalsIgnoreCase(OIDC_LOGIN_PATH) || name.equalsIgnoreCase(OIDC_CALLBACK_PATH))
      throw new MolgenisException(String.format("Schema name: '%s' is a reserved word", name));
    return name;
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
    if (current.getInheritedTable() != null) {
      String scopeTableName = current.getInherit();
      if (!current.getInheritedTable().getSchemaName().equals(getName())) {
        scopeTableName = current.getInheritedTable().getSchemaName() + "_" + scopeTableName;
      }
      if (!tables.containsKey(scopeTableName)) {
        tables.put(scopeTableName, current.getInheritedTable());
        addExternalTablesRecursive(tables, current.getInheritedTable());
      }
    }
    for (Column c : current.getColumns()) {
      if (c.isReference()) {
        String scopeTableName = c.getRefTableName();
        if (!getName().equals(c.getRefSchema())) {
          scopeTableName = c.getRefSchema() + "_" + scopeTableName;
        }
        if (!tables.containsKey(scopeTableName)) {
          tables.put(scopeTableName, c.getRefTable());
          addExternalTablesRecursive(tables, c.getRefTable());
        }
      }
    }
  }

  public Set<String> getLocales() {
    // sorted on alphabet
    Set<String> result = new TreeSet<>();
    getTables()
        .forEach(
            table -> {
              result.addAll(table.getLabels().keySet());
              result.addAll(table.getDescriptions().keySet());
              table
                  .getColumns()
                  .forEach(
                      column -> {
                        result.addAll(column.getLabels().keySet());
                        result.addAll(column.getDescriptions().keySet());
                      });
            });
    return result;
  }
}
