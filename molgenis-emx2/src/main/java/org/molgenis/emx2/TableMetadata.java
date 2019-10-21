package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.ColumnType.*;

import java.util.*;

public class TableMetadata {

  private SchemaMetadata schema;

  private String tableName = null;
  private Map<String, Column> columns = new LinkedHashMap<>();
  private List<String[]> uniques = new ArrayList<>();
  private String[] primaryKey = new String[0];
  private String inherits = null;

  public TableMetadata(String tableName) {
    this.tableName = tableName;
  }

  // can only be used by members in same package
  protected TableMetadata(SchemaMetadata schema, String tableName) {
    this.clearCache();
    this.schema = schema;
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }

  public SchemaMetadata getSchema() {
    return schema;
  }

  public TableMetadata setPrimaryKey(String... columnNames) {
    this.primaryKey = columnNames;
    return this;
  }

  public String[] getPrimaryKey() {
    if (this.inherits != null) return getInheritedTable().getPrimaryKey();
    return this.primaryKey;
  }

  public List<Column> getLocalColumns() {
    ArrayList<Column> result = new ArrayList<>();
    result.addAll(columns.values());
    return result;
  }

  public Set<String> getLocalColumnNames() {
    return columns.keySet();
  }

  public List<Column> getColumns() {
    ArrayList<Column> result = new ArrayList<>();
    if (inherits != null) {
      result.addAll(getInheritedTable().getColumns());

      // ignore primary key from child class because that is same as in inheritedTable
      List<String> primaryKeyList = Arrays.asList(getPrimaryKey());
      for (Column c : this.columns.values()) {
        if (!primaryKeyList.contains(c.getColumnName())) result.add(c);
      }
    } else {
      result.addAll(columns.values());
    }
    return Collections.unmodifiableList(result);
  }

  public Collection<String> getColumnNames() {
    Set<String> result = new HashSet<>();
    if (inherits != null) {
      result.addAll(getInheritedTable().getColumnNames());
    }
    result.addAll(getLocalColumnNames());
    return result;
  }

  public Column getColumn(String name) {
    if (columns.containsKey(name)) return columns.get(name);
    if (inherits != null) {
      return getInheritedTable().getColumn(name);
    }
    return null;
  }

  public Column addColumn(Column column) {
    column.setTable(this);
    columns.put(column.getColumnName(), column);
    return column;
  }

  public Column addColumn(String name) {
    return this.addColumn(name, STRING);
  }

  public Column addColumn(String name, ColumnType columnType) {
    Column c = new Column(this, name, columnType);
    columns.put(name, c);
    return c;
  }

  public Column addRef(String name, String toTable) {
    return this.addRef(name, toTable, null);
  }

  public Column addRef(String name, String toTable, String toColumn) {
    Column c = new Column(this, name, REF).setReference(toTable, toColumn);
    this.addColumn(c);
    return c;
  }

  public Column addRefArray(String name, String toTable, String toColumn) {
    Column c = new Column(this, name, REF_ARRAY).setReference(toTable, toColumn);
    this.addColumn(c);
    return c;
  }

  public Column addRefArray(String name, String toTable) {
    return this.addRefArray(name, toTable, getPrimaryKey()[0]);
  }

  public ReferenceMultiple addRefMultiple(String... name) {
    return new ReferenceMultiple(this, REF, name);
  }

  public ReferenceMultiple addRefArrayMultiple(String... name) {
    return new ReferenceMultiple(this, REF_ARRAY, name);
  }

  public Column addMref(
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTableName) {
    Column c =
        new Column(this, name, MREF)
            .setReference(refTable, refColumn)
            .setReverseReference(reverseName, reverseRefColumn)
            .setMrefJoinTable(joinTableName);
    this.addColumn(c);
    return c;
  }

  public void removeColumn(String name) {
    columns.remove(name);
  }

  public Collection<String[]> getUniques() {
    return Collections.unmodifiableCollection(uniques);
  }

  public TableMetadata addUnique(String... columnNames) {
    if (isUnique(columnNames)) return this; // idempotent, we silently ignore
    uniques.add(columnNames);
    return this;
  }

  public boolean isUnique(String... names) {
    for (String[] el : this.uniques) {
      if (equalContents(el, names)) {
        return true;
      }
    }
    if (inherits != null) return getInheritedTable().isUnique(names);
    return false;
  }

  private boolean equalContents(String[] a, String[] b) {
    ArrayList one = new ArrayList<>(Arrays.asList(a));
    Collections.sort(one);
    ArrayList<String> two = new ArrayList<>(Arrays.asList(b));
    Collections.sort(two);
    return one.containsAll(two) && two.containsAll(one) && one.size() == two.size();
  }

  public boolean isPrimaryKey(String... names) {
    return equalContents(names, this.getPrimaryKey());
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

  public TableMetadata inherits(String otherTable) {
    this.inherits = otherTable;
    return this;
  }

  public String getInherits() {
    return this.inherits;
  }

  public TableMetadata getInheritedTable() {
    return getSchema().getTableMetadata(inherits);
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

  public void enableRowLevelSecurity() {
    throw new UnsupportedOperationException();
    // todo decide if RLS is default on
  }

  public void clearCache() {
    columns = new LinkedHashMap<>();
    uniques = new ArrayList<>();
    primaryKey = new String[0];
    inherits = null;
  }
}
