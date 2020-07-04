package org.molgenis.emx2;

import java.util.*;

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
    for (Column c : metadata.getLocalColumns()) {
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
    ArrayList<Column> result = new ArrayList<>();
    if (inherit != null) {
      // we create copies so we don't need worry on changes
      for (Column col : getInheritedTable().getColumns()) {
        result.add(new Column(col.getTable(), col));
      }

      // ignore primary key from child class because that is same as in inheritedTable
      for (Column col : getLocalColumns()) {
        if (getInherit() == null || getInheritedTable().getColumn(col.getName()) == null) {
          result.add(new Column(col.getTable(), col));
        }
      }
    } else {
      return getLocalColumns();
    }
    return Collections.unmodifiableList(result);
  }

  public List<String> getPrimaryKeys() {
    List<String> primaryKey = new ArrayList<>();
    for (Column c : columns.values()) {
      if (c.getKey() == 1) {
        primaryKey.add(c.getName());
      }
    }
    if (primaryKey.size() == 0) return null;
    return primaryKey;
  }

  public List<Column> getLocalColumns() {
    ArrayList<Column> result = new ArrayList<>();
    // copy to prevent side effects
    for (Column c : columns.values()) {
      result.add(new Column(c.getTable(), c));
    }
    return result;
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
    builder.append("TABLE(").append(getTableName()).append("){");
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

  private boolean equalContents(String[] a, String[] b) {
    if (a == null && b != null) return false;
    if (a != null && b == null) return false;
    if (a == b) return true;
    ArrayList<String> one = new ArrayList<>(Arrays.asList(a));
    ArrayList<String> two = new ArrayList<>(Arrays.asList(b));
    return one.containsAll(two) && two.containsAll(one) && one.size() == two.size();
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
    for (Column c : getColumns()) {
      if (c.getKey() == key) {
        keyColumns.add(c);
      }
    }
    return keyColumns;
  }

  public List<String> getKeyNames(int key) {
    List<String> result = new ArrayList<>();
    for (Column c : getKey(key)) {
      result.add(c.getName());
    }
    return result;
  }

  public Map<Integer, List<String>> getKeys() {
    Map<Integer, List<String>> keys = new LinkedHashMap<>();
    for (Column c : columns.values()) {
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
    for (Column c : this.columns.values()) {
      if (c.getKey() == key) {
        c.removeKey();
      }
    }
  }

  protected Column getLocalColumn(String name) {
    return columns.get(name);
  }
}
