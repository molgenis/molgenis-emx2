package org.molgenis.emx2;

import java.util.*;

public class TableMetadata {

  private SchemaMetadata schema;
  private String tableName;
  protected Map<String, Column> columns = new LinkedHashMap<>();
  private List<String[]> uniques = new ArrayList<>();
  protected String primaryKey = null;
  protected String inherit = null;

  public static TableMetadata table(String tableName) {
    return new TableMetadata(tableName);
  }

  public TableMetadata(String tableName) {
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
    for (String[] unique : metadata.getUniques()) {
      this.uniques.add(unique);
    }
    for (Column c : metadata.getLocalColumns()) {
      this.columns.put(c.getName(), new Column(this, c));
    }
    if (metadata.getPrimaryKey() != null) {
      this.primaryKey = metadata.getPrimaryKey();
      getColumn(metadata.getPrimaryKey()).pkey(true);
    }
    this.inherit = metadata.getInherit();
  }

  public String getTableName() {
    return tableName;
  }

  public SchemaMetadata getSchema() {
    return schema;
  }

  public String getPrimaryKey() {
    if (getInheritedTable() != null) {
      return getInheritedTable().getPrimaryKey();
    }
    return this.primaryKey;
  }

  public TableMetadata setPrimaryKey(String columnName) {
    if (getColumn(columnName) == null) {
      throw new MolgenisException(
          "Set primary key failed",
          "'Column '" + columnName + "' unknown in table '" + getTableName() + "'");
    }
    // reset old
    if (this.primaryKey != null) {
      this.columns.get(this.primaryKey).pkey(false);
    }
    // set new
    this.primaryKey = columnName;
    this.columns.get(columnName).pkey(true);
    return this;
  }

  public List<Column> getColumns() {
    ArrayList<Column> result = new ArrayList<>();
    if (inherit != null) {

      // we create copies so we don't need worry on changes
      for (Column col : getInheritedTable().getColumns()) {
        result.add(new Column(this, col));
      }

      // ignore primary key from child class because that is same as in inheritedTable
      for (Column c : getLocalColumns()) {
        if (getPrimaryKey() == null || getPrimaryKey().equals(c.getName())) {
          result.add(new Column(this, c));
        }
      }
    } else {
      return getLocalColumns();
    }
    return Collections.unmodifiableList(result);
  }

  public List<Column> getLocalColumns() {
    ArrayList<Column> result = new ArrayList<>();
    // copy to prevent side effects
    for (Column c : columns.values()) {
      result.add(new Column(this, c));
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
      if (c != null) return new Column(this, c);
    }
    return null;
  }

  public TableMetadata addColumn(Column column) {
    columns.put(column.getName(), new Column(this, column));
    if (column.isPrimaryKey()) {
      this.setPrimaryKey(column.getName());
    }
    column.setTable(this);
    return this;
  }

  public TableMetadata alter(Column column) {
    columns.put(column.getName(), new Column(this, column));
    if (column.isPrimaryKey()) this.setPrimaryKey(column.getName());
    column.setTable(this);
    return this;
  }

  public void removeColumn(String name) {
    if (name.equals(primaryKey))
      throw new MolgenisException("Remove column failed", "Column is primary key");
    columns.remove(name);
  }

  public Collection<String[]> getUniques() {
    return Collections.unmodifiableCollection(uniques);
  }

  public TableMetadata addUnique(String... columnNames) {
    if (isUnique(columnNames)) return this; // idempotent, we silently ignore
    for (String name : columnNames) {
      if (getColumn(name) == null)
        throw new MolgenisException(
            "Add unique failed",
            "Column with name '" + name + "' does not exist in table '" + getTableName() + "'");
    }
    uniques.add(columnNames);
    return this;
  }

  public boolean isUnique(String... names) {
    for (String[] el : this.uniques) {
      if (equalContents(el, names)) {
        return true;
      }
    }

    if (inherit != null) return getInheritedTable().isUnique(names);
    return false;
  }

  public void removeUnique(String... keys) {
    for (int i = 0; i < uniques.size(); i++) {
      if (equalContents(uniques.get(i), keys)) {
        uniques.remove(i);
        break;
      }
    }
    // will not delete from inheritedTable
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
    for (String[] u : getUniques()) {
      builder.append("\n\t").append(Arrays.toString(u));
    }
    builder.append("\n}");
    return builder.toString();
  }

  public void clearCache() {
    columns = new LinkedHashMap<>();
    uniques = new ArrayList<>();
    primaryKey = null;
    inherit = null;
  }

  private boolean equalContents(String[] a, String[] b) {
    if (a == b) return true;
    ArrayList<String> one = new ArrayList<>(Arrays.asList(a));
    ArrayList<String> two = new ArrayList<>(Arrays.asList(b));
    return one.containsAll(two) && two.containsAll(one) && one.size() == two.size();
  }

  public Column getPrimaryKeyColumn() {
    Column result = getColumn(getPrimaryKey());
    if (result == null) {
      throw new MolgenisException(
          "Primary key error",
          "Primary key '" + getPrimaryKey() + "' does not exist in table '" + getTableName() + "");
    }
    return result;
  }

  public boolean exists() {
    return !getColumns().isEmpty();
  }
}
