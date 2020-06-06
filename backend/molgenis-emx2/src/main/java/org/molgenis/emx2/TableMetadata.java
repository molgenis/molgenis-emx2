package org.molgenis.emx2;

import java.util.*;

public class TableMetadata {

  private SchemaMetadata schema;
  private String tableName;
  protected String inherit = null;
  protected String description = null;
  protected Map<String, Column> columns = new LinkedHashMap<>();
  private String[] primaryKey = null;
  private List<String[]> uniques = new ArrayList<>();

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
    this.primaryKey = metadata.getPrimaryKey();
    for (String[] unique : metadata.getUniques()) {
      this.uniques.add(unique);
    }
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

  public String[] getPrimaryKey() {
    if (getInheritedTable() != null) {
      return getInheritedTable().getPrimaryKey();
    }
    return primaryKey;
  }

  public TableMetadata pkey(String... columnName) {
    if (columnName != null) {
      for (String name : columnName) {
        if (getColumn(name) == null) {
          throw new MolgenisException(
              "Set primary key failed",
              "'Column '" + name + "' unknown in table '" + getTableName() + "'");
        }
      }
    }
    this.primaryKey = columnName;
    return this;
  }

  public List<Column> getColumns() {
    ArrayList<Column> result = new ArrayList<>();
    if (inherit != null) {
      // we create copies so we don't need worry on changes
      for (Column col : getInheritedTable().getColumns()) {
        result.add(new Column(col.getTable(), col));
      }

      // ignore primary key from child class because that is same as in inheritedTable
      for (Column c : getLocalColumns()) {
        if (getInherit() == null || !getPrimaryKey().equals(c.getName())) {
          result.add(new Column(c.getTable(), c));
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

  public TableMetadata add(Column... column) {
    for (Column c : column) {
      columns.put(c.getName(), new Column(this, c));
      c.setTable(this);
    }
    return this;
  }

  public TableMetadata alterColumn(Column column) {
    return this.alterColumn(column.getName(), column);
  }

  public TableMetadata alterColumn(String name, Column column) {
    // add the new
    columns.put(column.getName(), new Column(this, column));
    if (this.primaryKey != null && Arrays.asList(this.getPrimaryKey()).contains(name)) {
      for (int idx = 0; idx < this.primaryKey.length; idx++) {
        if (this.primaryKey[idx].equals(name.trim())) {
          this.primaryKey[idx] = column.getName();
        }
      }
    }
    column.setTable(this);

    // if changed, update any unique constraints involving this column
    if (!column.getName().equals(name)) {
      for (String[] unique : getUniques()) {
        List<String> uniqueList = Arrays.asList(unique);
        if (uniqueList.contains(name)) {
          this.removeUnique(unique);
          uniqueList.set(uniqueList.indexOf(name), column.getName());
          this.addUnique(uniqueList.toArray(new String[uniqueList.size()]));
        }
      }

      // update any refs involving this column
      for (TableMetadata tm : getSchema().getTables()) {
        for (Column c : tm.getColumns()) {
          if (c.getRefTableName() != null
              && c.getRefTableName().equals(this.getTableName())
              && c.getRefColumnName().equals(name)) {
            c.refColumn(column.getName());
          }
        }
      }
      columns.remove(name);
    }
    return this;
  }

  public void dropColumn(String name) {
    if (name.equals(getPrimaryKey()))
      throw new MolgenisException("Remove column failed", "Column is primary key");
    if (columns.get(name) == null)
      throw new MolgenisException("Remove column failed", "Column '" + name + "' unknown");
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

  public boolean isPrimaryKey(String... names) {
    if (equalContents(this.primaryKey, names)) {
      return true;
    }

    if (inherit != null) return getInheritedTable().isPrimaryKey(names);
    return false;
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
    for (String[] u : getUniques()) {
      builder.append("\n\t").append(Arrays.toString(u));
    }
    builder.append("\n}");
    return builder.toString();
  }

  public void clearCache() {
    columns = new LinkedHashMap<>();
    uniques = new ArrayList<>();
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

  public void removePrimaryKey(String name) {}
}
