package org.molgenis.emx2;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;

import java.util.*;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;

public class TableMetadata implements Comparable {

  // if a table extends another table (optional)
  public String inherit = null;
  // to allow indicate that a table should be dropped
  protected boolean drop = false;
  // for refenence to another schema (rare use)
  protected String importSchema = null;
  // description of the table (optional)
  protected String description = null;
  // columns of the table (required)
  protected Map<String, Column> columns = new LinkedHashMap<>();
  // key value map for settings specific to this table (optional)
  protected Map<String, Setting> settings = new LinkedHashMap<>();
  // link to the schema this table is part of (optional)
  private SchemaMetadata schema;
  // name unique within this schema (required)
  protected String tableName;
  // old name, useful for alter table
  private String oldName;

  public String[] getSemantics() {
    return semantics;
  }

  public TableMetadata setSemantics(String... semantics) {
    this.semantics = semantics;
    return this;
  }

  private String[] semantics = null;

  public TableMetadata(String tableName) {
    if (!tableName.matches("[a-zA-Z][a-zA-Z0-9_]*") || tableName.length() > 341) {
      throw new MolgenisException(
          "Invalid table name '"
              + tableName
              + "': Table name must start with a letter , followed by letters, underscores or numbers, i.e. [a-zA-Z][a-zA-Z0-9_]*. Maximum length: 31 characters (so it fits in Excel sheet names)");
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
    this.sync(metadata);
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

  public void sync(TableMetadata metadata) {
    // skip if same object!
    if (this != metadata) {
      clearCache();
      this.tableName = metadata.getTableName();
      this.description = metadata.getDescription();
      this.oldName = metadata.getOldName();
      for (Setting setting : metadata.getSettings()) {
        this.settings.put(setting.getKey(), setting);
      }
      for (Column c : metadata.columns.values()) {
        this.columns.put(c.getName(), new Column(this, c));
      }
      this.inherit = metadata.getInherit();
      this.importSchema = metadata.getImportSchema();
      this.semantics = metadata.getSemantics();
    }
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
    // we want to sort on position,
    // first external schema (because their positions local to that schema)
    // last we attach the 'meta
    Map<String, Column> external = new LinkedHashMap<>(); // external schema has own ordering
    Map<String, Column> internal = new LinkedHashMap<>();
    Map<String, Column> meta = new LinkedHashMap<>();

    if (getInheritedTable() != null) {
      // we create copies so we don't need worry on changes
      for (Column col : getInheritedTable().getColumns()) {
        if (col.getName().startsWith("mg_")) {
          meta.put(col.getName(), new Column(getInheritedTable(), col));
          // sorting of external schema is seperate from internal schema
        } else if (!Objects.equals(col.getTable().getSchemaName(), getSchemaName())) {
          external.put(col.getName(), new Column(getInheritedTable(), col));
        } else {
          internal.put(col.getName(), new Column(getInheritedTable(), col));
        }
      }
    }

    // ignore primary key from child class because that is same as in inheritedTable
    for (Column col : getLocalColumns()) {
      if (!internal.containsKey(col.getName()) && !external.containsKey(col.getName())) {
        if (col.getName().startsWith("mg_")) {
          meta.put(col.getName(), new Column(col.getTable(), col));
          // sorting of external schema is seperate from internal schema
        } else {
          internal.put(col.getName(), new Column(col.getTable(), col));
        }
      }
    }

    // sort by position
    List<Column> externalList = new ArrayList<>(external.values());
    List<Column> internalList = new ArrayList<>(internal.values());
    List<Column> metaList = new ArrayList<>(meta.values());

    Collections.sort(externalList);
    Collections.sort(internalList);
    Collections.sort(metaList);

    List<Column> finalResult = new ArrayList<>();
    finalResult.addAll(externalList);
    finalResult.addAll(internalList);
    finalResult.addAll(metaList);
    return finalResult;
  }

  public List<Column> getColumnsWithoutHeadings() {
    return this.getColumns().stream().filter(c -> !c.isHeading()).toList();
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

  public List<Column> getDownloadColumnNames() {
    return getExpandedColumns(
        getColumns().stream()
            .filter(c -> !c.isRefback())
            .map(c2 -> c2.isFile() ? column(c2.getName()) : c2)
            .collect(Collectors.toList()));
  }

  public List<Column> getMutationColumns() {
    return getExpandedColumns(getStoredColumns());
  }

  /** returns columns including the nested composite key columns, needed to create the table */
  public List<Column> getExpandedColumns(List<Column> columns) {
    Map<String, Column> result =
        new LinkedHashMap<>(); // overlapping references can lead to duplicates
    for (Column c : columns) {
      if (c.isFile()) {
        result.put(c.getName(), new Column(c.getTable(), c.getName()));
        result.put(
            c.getName() + "_contents",
            new Column(c.getTable(), c.getName() + "_contents").setType(FILE));
        result.put(c.getName() + "_mimetype", new Column(c.getTable(), c.getName() + "_mimetype"));
        result.put(
            c.getName() + "_extension", new Column(c.getTable(), c.getName() + "_extension"));
        result.put(
            c.getName() + "_size", new Column(c.getTable(), c.getName() + "_size").setType(INT));
      } else
      // expand composite keys if necessary
      if (c.isReference()) {
        for (Reference ref : c.getReferences()) {
          if (!ref.isOverlapping()) { // only add overlapping once
            result.put(
                ref.getName(),
                new Column(c.getTable(), ref.getName(), true).setType(ref.getPrimitiveType()));
          }
        }
      } else {
        result.put(c.getName(), c);
      }
    }
    return new ArrayList<>(result.values());
  }

  public List<Column> getNonInheritedColumns() {
    if (getInherit() != null) {
      return this.columns.values().stream()
          .filter(c -> !getInheritedTable().getColumnNames().contains(c.getName()))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>(this.columns.values());
    }
  }

  public List<Column> getStoredColumns() {
    return getLocalColumns().stream()
        .filter(c -> !HEADING.equals(c.getColumnType()))
        .collect(Collectors.toList());
  }

  public List<Column> getLocalColumns() {
    Map<String, Column> result = new LinkedHashMap<>();
    // get primary key from parent, always first
    if (getInheritedTable() != null) {
      for (Column pkey : getInheritedTable().getPrimaryKeyColumns()) {
        result.put(pkey.getName(), pkey);
      }
    }

    // get all implemented columns (keep superclass because of type)
    List<Column> columnList =
        new ArrayList<>(
            columns.values().stream()
                .filter(c -> !c.getName().startsWith("mg_"))
                .collect(Collectors.toList()));
    Collections.sort(columnList);

    // add meta behind non-meta
    List<Column> metaList =
        new ArrayList<>(
            columns.values().stream()
                .filter(c -> c.getName().startsWith("mg_"))
                .collect(Collectors.toList()));
    columnList.addAll(metaList);

    for (Column c : columnList) {
      if (!result.containsKey(c.getName())) {
        result.put(c.getName(), c);
      }
    }

    return new ArrayList<>(result.values());
  }

  public List<String> getColumnNames() {
    return getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
  }

  public List<String> getLocalColumnNames() {
    List<String> result = new ArrayList<>();
    for (Column c : getLocalColumns()) {
      result.add(c.getName());
    }
    return result;
  }

  public List<String> getNonInheritedColumnNames() {
    // skip inherited
    if (getInherit() != null) {
      TableMetadata inheritedTable = getInheritedTable();
      return getColumnNames().stream()
          .filter(c -> inheritedTable.getColumn(c) == null)
          .collect(Collectors.toList());
    }
    return getColumnNames();
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
        if (getSchema().getDatabase().getSchema(getImportSchema()).getTable(inherit) == null) {
          throw new MolgenisException(
              "Cannot find table '" + inherit + "' for inheritance of table.");
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
    settings = new LinkedHashMap<>();
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
    for (Column c : getStoredColumns()) {
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
    for (Column c : getStoredColumns()) {
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
    for (Column c : this.getStoredColumns()) {
      if (c.getKey() == key) {
        c.removeKey();
      }
    }
  }

  public Column getLocalColumn(String name) {
    return columns.get(name);
  }

  public org.jooq.Table<Record> getJooqTable() {
    return DSL.table(name(getSchemaName(), getTableName()));
  }

  public List<Field> getPrimaryKeyFields() {
    return getKeyFields(1);
  }

  public List<Setting> getSettings() {
    return settings.values().stream().collect(Collectors.toList());
  }

  public TableMetadata setSettings(List<Setting> settings) {
    if (settings == null) return this;
    for (Setting setting : settings) {
      this.settings.put(setting.getKey(), setting);
    }
    return this;
  }

  public void removeSetting(String key) {
    this.settings.remove(key);
  }

  public String getImportSchema() {
    return importSchema;
  }

  public TableMetadata setImportSchema(String importSchema) {
    this.importSchema = importSchema;
    return this;
  }

  public TableMetadata setSetting(String key, String value) {
    this.setSettings(List.of(new Setting(key, value)));
    return this;
  }

  public String getOldName() {
    return oldName;
  }

  public TableMetadata setOldName(String oldName) {
    this.oldName = oldName;
    return this;
  }

  public boolean isDrop() {
    return drop;
  }

  public void drop() {
    this.drop = true;
  }

  public TableMetadata alterName(String name) {
    this.tableName = name;
    return this;
  }

  @Override
  public int compareTo(Object o) {
    if (o instanceof TableMetadata) {
      return getTableName().compareTo(((TableMetadata) o).getTableName());
    }
    return 0;
  }

  public Table getTable() {
    return getSchema().getDatabase().getSchema(this.getSchemaName()).getTable(getTableName());
  }

  public List<Column> getColumnsWithoutMetadata() {
    return getColumns().stream()
        .filter(c -> !c.getName().startsWith("mg_"))
        .collect(Collectors.toList());
  }
}
