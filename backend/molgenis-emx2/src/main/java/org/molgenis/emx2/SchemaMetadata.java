package org.molgenis.emx2;

import java.util.*;
import org.molgenis.emx2.utils.TableSort;

public class SchemaMetadata {

  protected Map<String, TableMetadata> tables = new LinkedHashMap<>();
  protected Map<String, Setting> settings = new LinkedHashMap<>();
  private String name;
  // optional
  private Database database;

  public SchemaMetadata() {}

  public SchemaMetadata(String name) {
    if (name == null || name.isEmpty())
      throw new MolgenisException("Create schema failed: Schema name was null or empty");
    this.name = name;
  }

  public SchemaMetadata(SchemaMetadata schema) {
    this.name = schema.getName();
    this.database = schema.getDatabase();
    this.setSettings(schema.getSettings());
    for (Setting setting : schema.getSettings()) {
      this.setSetting(setting.getKey(), setting.getValue());
    }
  }

  public SchemaMetadata(Database db, SchemaMetadata schema) {
    this.name = schema.getName();
    this.database = db;
    for (Setting setting : schema.getSettings()) {
      this.setSetting(setting.getKey(), setting.getValue());
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public List<Setting> getSettings() {
    List<Setting> result = new ArrayList<>();
    result.addAll(this.settings.values());
    return result;
  }

  public SchemaMetadata setSettings(Collection<Setting> settings) {
    if (settings == null) return this;
    for (Setting setting : settings) {
      this.settings.put(setting.getKey(), new Setting(setting));
    }
    return this;
  }

  public SchemaMetadata setSetting(String name, String value) {
    this.settings.put(name, new Setting(name, value));
    return this;
  }

  public Database getDatabase() {
    return database;
  }

  public void setDatabase(Database database) {
    this.database = database;
  }

  public void removeSetting(String key) {
    this.settings.remove(key);
  }
}
