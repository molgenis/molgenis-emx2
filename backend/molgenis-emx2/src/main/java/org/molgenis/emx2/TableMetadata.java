package org.molgenis.emx2;

import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.*;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.*;

public class TableMetadata {

  private SchemaMetadata schema;
  private String tableName;
  protected String inherit = null;
  protected String description = null;
  protected Map<String, Column> columns = new LinkedHashMap<>();

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

  public TableMetadata(String tableName) {
    if (!tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
      throw new MolgenisException(
          "Invalid table name '" + tableName + "'",
          "Table name must start with a letter or underscore, followed by letters, underscores or numbers");
    }
    this.tableName = tableName;
  }

  public TableMetadata(SchemaMetadata schema, String tableName) {
    this(tableName);
    this.schema = schema;
  }

  protected TableMetadata(SchemaMetadata schema, TableMetadata metadata) {
    this.clearCache();
    this.schema = schema;
    this.copy(metadata);
  }

  protected void copy(TableMetadata metadata) {
    clearCache();
    this.tableName = metadata.getTableName();
    for (Column c : metadata.columns.values()) {
      this.columns.put(c.getName(), new Column(this, c));
    }
    this.inherit = metadata.getInherit();
  }

  public String getTableName() {
    return tableName;
  }

  public SchemaMetadata getSchema() {
    return schema;
  }

  public List<Column> getColumns() {
    Map<String, Column> result = new LinkedHashMap<>();
    if (inherit != null) {
      // we create copies so we don't need worry on changes
      for (Column col : getInheritedTable().getColumns()) {
        result.put(col.getName(), new Column(col.getTable(), col));
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
    if (primaryKey.isEmpty()) return null;
    return primaryKey;
  }

  public List<Column> getMutationColumns() {
    ArrayList<Column> result = new ArrayList<>();
    for (Column c : getLocalColumns()) {
      if (REF.equals(c.getColumnType())
          || REF_ARRAY.equals(c.getColumnType())
          || REFBACK.equals(c.getColumnType())
          || MREF.equals(c.getColumnType())) {
        for (Reference ref : c.getRefColumns()) {
          result.add(new Column(ref.getName()).type(ref.getColumnType()).setTable(this));
        }
      } else {
        result.add(c);
      }
    }
    return result;
  }

  public List<Column> getLocalColumns() {
    Map<String, Column> result = new LinkedHashMap<>();
    // get primary key from parent
    if (getInherit() != null) {
      for (Column pkey : getInheritedTable().getPrimaryKeyColumns()) {
        result.put(pkey.getName(), pkey);
      }
    }
    // get all implemented columns (keep superclass because of type)
    for (Column c : columns.values()) {
      if (!result.containsKey(c.getName())) {
        result.put(c.getName(), c);
      }
    }
    return new ArrayList<>(result.values());
  }

  public Collection<String> getColumnNames() {
    Set<String> result = new HashSet<>();
    if (inherit != null) {
      result.addAll(getInheritedTable().getColumnNames());
    }
    result.addAll(getLocalColumnNames());
    return result;
  }

  public Set<String> getLocalColumnNames() {
    return columns.keySet();
  }

  public Column getColumn(String name) {
    if (columns.containsKey(name)) return new Column(this, columns.get(name));
    if (inherit != null) {
      Column c = getInheritedTable().getColumn(name);
      if (c != null) return new Column(c.getTable(), c);
    }
    return null;
  }

  public TableMetadata add(Column column) {
    columns.put(column.getName(), new Column(this, column));
    column.setTable(this);
    return this;
  }

  public TableMetadata alterColumn(Column column) {
    return this.alterColumn(column.getName(), column);
  }

  public TableMetadata alterColumn(String name, Column column) {
    // remove the old
    columns.remove(name);
    // add the new
    columns.put(column.getName(), new Column(this, column));
    column.setTable(this);
    return this;
  }

  public void dropColumn(String name) {
    if (Arrays.asList(getPrimaryKeys()).contains(name))
      throw new MolgenisException("Remove column failed", "Column is primary key");
    if (columns.get(name) == null)
      throw new MolgenisException("Remove column failed", "Column '" + name + "' unknown");
    columns.remove(name);
  }

  public TableMetadata setInherit(String otherTable) {
    this.inherit = otherTable;
    return this;
  }

  public String getInherit() {
    return this.inherit;
  }

  public TableMetadata getInheritedTable() {
    if (inherit != null && getSchema() != null) {
      return getSchema().getTableMetadata(inherit);
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
    // todo decide if RLS is default on
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    String tableName = getTableName();
    if (getInherit() != null) {
      tableName += " extends " + getInherit();
    }
    builder.append("TABLE(").append(tableName).append("){");
    for (Column c : getColumns()) {
      builder.append("\n\t").append(c.toString());
    }
    builder.append("\n}");
    return builder.toString();
  }

  public void clearCache() {
    columns = new LinkedHashMap<>();
    inherit = null;
  }

  public boolean exists() {
    return !getColumns().isEmpty();
  }

  public TableMetadata removeInherit() {
    this.inherit = null;
    return this;
  }

  public void setSchema(SchemaMetadata schemaMetadata) {
    this.schema = schemaMetadata;
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

  public List<String> getKeyNames(int key) {
    List<String> result = new ArrayList<>();
    for (Column c : getKey(key)) {
      if (c.isReference()) {
        for (Reference ref : c.getRefColumns()) {
          result.add(ref.getName());
        }
      } else {
        result.add(c.getName());
      }
    }
    return result;
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

  public org.jooq.Table getJooqTable() {
    return DSL.table(name(getSchemaName(), getTableName()));
  }

  public List<Field<?>> getPrimaryKeyFields() {
    List<Field<?>> result = new ArrayList<>();
    for (Column c : getPrimaryKeyColumns()) {
      if (c.isReference()) {
        for (Reference r : c.getRefColumns()) {
          result.add(field(name(r.getName()), r.getJooqType()));
        }
      } else {
        result.add(field(name(c.getName()), c.getJooqType()));
      }
    }
    return result;
  }
}
