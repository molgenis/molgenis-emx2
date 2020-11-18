package org.molgenis.emx2;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.FILE;
import static org.molgenis.emx2.ColumnType.INT;

import java.util.*;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;

public class TableMetadata {

  // if a table extends another table (optional)
  protected String inherit = null;
  // use for enabling inherit to go accross schema's (optional)
  protected String importSchema = null;
  // description of the table (optional)
  protected String description = null;
  // columns of the table (required)
  protected Map<String, Column> columns = new LinkedHashMap<>();
  // key value map for settings specific to this table (optional)
  protected Map<String, String> settings = new LinkedHashMap<>();
  // link to the schema this table is part of (optional)
  private SchemaMetadata schema;
  // name unique within this schema (required)
  private String tableName;

  public TableMetadata(String tableName) {
    if (!tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*") || tableName.length() > 341) {
      throw new MolgenisException(
          "Invalid table name '"
              + tableName
              + "': Table name must start with a letter or underscore, followed by letters, underscores or numbers. Maximum length: 31 characters (so it fits in Excel sheet names)");
    }
    this.tableName = tableName;
  }

  public TableMetadata(SchemaMetadata schema, String tableName) {
    this(tableName);
    this.schema = schema;
  }

  public TableMetadata(SchemaMetadata schema, TableMetadata metadata) {
    this.clearCache();
    this.schema = schema;
    this.copy(metadata);
  }

  public static TableMetadata table(String tableName) {
    return new TableMetadata(tableName);
  }

  public static TableMetadata table(String tableName, Column... columns) {
    TableMetadata tm = new TableMetadata(tableName);
    for (Column c : columns) {
      tm.add(c);
    }
    return tm;
  }

  protected void copy(TableMetadata metadata) {
    clearCache();
    this.tableName = metadata.getTableName();
    this.settings = metadata.getSettings();
    for (Column c : metadata.columns.values()) {
      this.columns.put(c.getName(), new Column(this, c));
    }
    this.inherit = metadata.getInherit();
    this.importSchema = metadata.getImportSchema();
  }

  public String getTableName() {
    return tableName;
  }

  public SchemaMetadata getSchema() {
    return schema;
  }

  public void setSchema(SchemaMetadata schemaMetadata) {
    this.schema = schemaMetadata;
  }

  public List<Column> getColumns() {
    Map<String, Column> result = new LinkedHashMap<>();
    if (getInheritedTable() != null) {
      // we create copies so we don't need worry on changes
      for (Column col : getInheritedTable().getColumns()) {
        result.put(col.getName(), new Column(getInheritedTable(), col));
      }
    }

    // ignore primary key from child class because that is same as in inheritedTable
    for (Column col : getLocalColumns()) {
      if (!result.containsKey(col.getName())) {
        result.put(col.getName(), new Column(col.getTable(), col));
      }
    }

    return new ArrayList<>(result.values());
  }

  public List<String> getPrimaryKeys() {
    List<String> primaryKey = new ArrayList<>();
    for (Column c : getColumns()) {
      if (c.getKey() == 1) {
        primaryKey.add(c.getName());
      }
    }
    return primaryKey;
  }

  public List<Column> getMutationColumns() {
    Map<String, Column> result =
        new LinkedHashMap<>(); // overlapping references can lead to duplicates
    for (Column c : getLocalColumns()) {
      if (FILE.equals(c.getColumnType())) {
        result.put(
            c.getName() + "_contents",
            new Column(c.getTable(), c.getName() + "_contents").setType(FILE));
        result.put(c.getName() + "_mimetype", new Column(c.getTable(), c.getName() + "_mimetype"));
        result.put(
            c.getName() + "_extension", new Column(c.getTable(), c.getName() + "_extension"));
        result.put(
            c.getName() + "_size", new Column(c.getTable(), c.getName() + "_size").setType(INT));
      } else if (c.isReference()) {
        for (Reference ref : c.getReferences()) {
          if (ref.isOverlapping()) {
            result.put(
                ref.getName(),
                new Column(c.getTable(), ref.getName())
                    .setType(ref.getOverlapping().getPrimitiveType()));
          } else {
            result.put(
                ref.getName(),
                new Column(c.getTable(), ref.getName()).setType(ref.getPrimitiveType()));
          }
        }
      } else {
        result.put(c.getName(), c);
      }
    }
    return new ArrayList<>(result.values());
  }

  public List<Column> getLocalColumns() {
    Map<String, Column> result = new LinkedHashMap<>();
    // get primary key from parent
    if (getInheritedTable() != null) {
      for (Column pkey : getInheritedTable().getPrimaryKeyColumns()) {
        result.put(pkey.getName(), pkey);
      }
    }
    // get all implemented columns (keep superclass because of type)
    List<Column> columnList = new ArrayList<>(columns.values());
    columnList.sort(Comparator.comparing(Column::getPosition));
    for (Column c : columnList) {
      if (!result.containsKey(c.getName())) {
        result.put(c.getName(), c);
      }
    }
    return new ArrayList<>(result.values());
  }

  public List<String> getColumnNames() {
    List<String> result = new ArrayList<>();
    if (inherit != null) {
      result.addAll(getInheritedTable().getColumnNames());
    }
    result.addAll(getLocalColumnNames());
    return result;
  }

  public List<String> getLocalColumnNames() {
    List<String> result = new ArrayList<>();
    for (Column c : getLocalColumns()) {
      result.add(c.getName());
    }
    return result;
  }

  public Column getColumn(String name) {
    if (columns.containsKey(name)) return new Column(this, columns.get(name));
    if (inherit != null) {
      Column c = getInheritedTable().getColumn(name);
      if (c != null) return new Column(c.getTable(), c);
    }
    return null;
  }

  public TableMetadata add(Column... column) {
    for (Column c : column) {
      if (c.getPosition() == null) {
        c.setPosition(columns.size());
      }
      columns.put(c.getName(), new Column(this, c));
      c.setTable(this);
    }
    return this;
  }

  public TableMetadata alterColumn(Column column) {
    return this.alterColumn(column.getName(), column);
  }

  public TableMetadata alterColumn(String name, Column column) {
    // retain position
    if (column.getPosition() == null) {
      column.setPosition(columns.get(name).getPosition());
    }
    // remove the old
    columns.remove(name);
    // add the new
    columns.put(column.getName(), new Column(this, column));
    column.setTable(this);
    return this;
  }

  public void dropColumn(String name) {
    if (Arrays.asList(getPrimaryKeys()).contains(name))
      throw new MolgenisException("Remove column failed: Column is primary key");
    if (columns.get(name) == null)
      throw new MolgenisException("Remove column failed: Column '" + name + "' unknown");
    columns.remove(name);
  }

  public String getInherit() {
    return this.inherit;
  }

  public TableMetadata setInherit(String otherTable) {
    this.inherit = otherTable;
    return this;
  }

  public TableMetadata getInheritedTable() {
    if (inherit != null && getSchema() != null) {
      if (getImportSchema() != null) {
        if (getSchema().getDatabase().getSchema(getImportSchema()) == null) {
          throw new MolgenisException(
              "Cannot find schema '"
                  + getImportSchema()
                  + " for inheritance of table '"
                  + inherit
                  + "'");
        }
        return getSchema()
            .getDatabase()
            .getSchema(getImportSchema())
            .getTable(inherit)
            .getMetadata();
      } else {
        return getSchema().getTableMetadata(inherit);
      }
    }
    return null;
  }

  public String getDescription() {
    return description;
  }

  public TableMetadata setDescription(String description) {
    this.description = description;
    return this;
  }

  public void enableRowLevelSecurity() {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    String name = getTableName();
    if (getInherit() != null) {
      if (getImportSchema() != null) {
        name += " extends " + getImportSchema() + "." + getInherit();
      } else {
        name += " extends " + getInherit();
      }
    }
    builder.append("TABLE(").append(name).append("){");
    for (Column c : getColumns()) {
      builder.append("\n\t").append(c.toString());
    }
    builder.append("\n}");
    return builder.toString();
  }

  public void clearCache() {
    columns = new LinkedHashMap<>();
    inherit = null;
    importSchema = null;
  }

  public boolean exists() {
    return !getColumns().isEmpty();
  }

  public TableMetadata removeInherit() {
    this.inherit = null;
    return this;
  }

  public String getSchemaName() {
    return getSchema().getName();
  }

  public List<Column> getPrimaryKeyColumns() {
    return getKey(1);
  }

  public List<Column> getKey(int key) {
    List<Column> keyColumns = new ArrayList<>();
    for (Column c : getLocalColumns()) {
      if (c.getKey() == key) {
        keyColumns.add(c);
      }
    }
    return keyColumns;
  }

  public List<Field> getKeyFields(int key) {
    // references might be overlapping so need to deduplicate via this map
    Map<String, Field<?>> result = new LinkedHashMap<>();
    for (Column c : getKey(key)) {
      if (c.isReference()) {
        for (Reference ref : c.getReferences()) {
          result.put(ref.getName(), ref.getJooqField());
        }
      } else {
        result.put(c.getName(), c.getJooqField());
      }
    }
    return new ArrayList<>(result.values());
  }

  public Map<Integer, List<String>> getKeys() {
    Map<Integer, List<String>> keys = new LinkedHashMap<>();
    for (Column c : getLocalColumns()) {
      if (c.getKey() > 0) {
        if (keys.get(c.getKey()) == null) {
          keys.put(c.getKey(), new ArrayList<>());
        }
        keys.get(c.getKey()).add(c.getName());
      }
    }
    return keys;
  }

  public void removeKey(int key) {
    for (Column c : this.getLocalColumns()) {
      if (c.getKey() == key) {
        c.removeKey();
      }
    }
  }

  protected Column getLocalColumn(String name) {
    return columns.get(name);
  }

  public org.jooq.Table<Record> getJooqTable() {
    return DSL.table(name(getSchemaName(), getTableName()));
  }

  public List<Field> getPrimaryKeyFields() {
    return getKeyFields(1);
  }

  public Map<String, String> getSettings() {
    return settings;
  }

  public TableMetadata setSettings(Map<String, String> settings) {
    if (settings == null) return this;
    this.settings =
        settings.entrySet().stream()
            .filter(e -> e.getValue() != null && e.getValue().trim().length() > 0)
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue)); // strip null and "" values
    return this;
  }

  public String getImportSchema() {
    return importSchema;
  }

  public TableMetadata setImportSchema(String importSchema) {
    this.importSchema = importSchema;
    return this;
  }
}
