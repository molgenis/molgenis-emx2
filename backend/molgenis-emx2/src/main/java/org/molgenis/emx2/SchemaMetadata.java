package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.OIDC_CALLBACK_PATH;
import static org.molgenis.emx2.Constants.OIDC_LOGIN_PATH;

import java.util.*;
import org.molgenis.emx2.utils.TableSort;

public class SchemaMetadata {

  protected Map<String, TableMetadata> tables = new LinkedHashMap<>();
  protected Map<String, String> settings = new LinkedHashMap<>();
  protected String name;
  // optional
  protected String description;
  protected boolean isChangeLogEnabled = false;
  // optional
  protected Database database;
  protected String template;
  protected boolean includeDemoData = false;

  public SchemaMetadata() {}

  public SchemaMetadata(String name) {
    validateSchemaName(name);
    this.name = name;
  }

  public SchemaMetadata(String name, boolean isChangeLogEnabled) {
    this(name);
    this.isChangeLogEnabled = isChangeLogEnabled;
  }

  public SchemaMetadata(String name, String description) {
    this(name);
    this.description = description;
  }

  public SchemaMetadata(String name, String description, boolean isChangeLogEnabled) {
    this(name, description);
    this.isChangeLogEnabled = isChangeLogEnabled;
  }

  public SchemaMetadata(SchemaMetadata schema) {
    this.name = schema.getName();
    this.description = schema.getDescription();
    this.database = schema.getDatabase();
    this.isChangeLogEnabled = schema.isChangeLogEnabled();
    this.setSettings(schema.getSettings());
    for (Setting setting : schema.getSettings()) {
      this.setSetting(setting.key(), setting.value());
    }
  }

  public SchemaMetadata(Database db, SchemaMetadata schema) {
    this.name = schema.getName();
    this.description = schema.getDescription();
    this.isChangeLogEnabled = schema.isChangeLogEnabled();
    this.database = db;
    for (Setting setting : schema.getSettings()) {
      this.setSetting(setting.key(), setting.value());
    }
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

  public SchemaMetadata setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public SchemaMetadata setDescription(String description) {
    this.description = description;
    return this;
  }

  public boolean isChangeLogEnabled() {
    return isChangeLogEnabled;
  }

  public SchemaMetadata setIsChangeLogEnabled(Boolean isChangeLogEnabled) {
    this.isChangeLogEnabled = isChangeLogEnabled;
    return this;
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
    result.addAll(
        this.settings.entrySet().stream()
            .map(entry -> new Setting(entry.getKey(), entry.getValue()))
            .toList());
    return result;
  }

  public SchemaMetadata setSettings(Map<String, String> settingsMap) {
    this.settings.putAll(settingsMap);
    return this;
  }

  public SchemaMetadata setSettings(Collection<Setting> settings) {
    if (settings == null) return this;
    for (Setting setting : settings) {
      this.settings.put(setting.key(), setting.value());
    }
    return this;
  }

  public SchemaMetadata setSetting(String name, String value) {
    this.settings.put(name, value);
    return this;
  }

  public Database getDatabase() {
    return database;
  }

  public void setDatabase(Database database) {
    this.database = database;
  }

  public SchemaMetadata removeSetting(String key) {
    this.settings.remove(key);
    return this;
  }

  public String getSetting(String key) {
    for (Setting s : getSettings()) {
      if (s.key().equals(key)) {
        return s.value();
      }
    }
    return null;
  }

  public String getTemplate() {
    return template;
  }

  public SchemaMetadata setTemplate(String template) {
    this.template = template;
    return this;
  }

  public boolean isIncludeDemoData() {
    return includeDemoData;
  }

  public SchemaMetadata setIncludeDemoData(boolean includeDemoData) {
    this.includeDemoData = includeDemoData;
    return this;
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

  protected Map<String, String> getSettingsAsMap() {
    return Collections.unmodifiableMap(this.settings);
  }
}
