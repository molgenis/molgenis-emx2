package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.ColumnType.*;

import java.util.*;

public class TableMetadata {
  public static final String FOREIGN_KEY_ADD_FAILED = "foreign_key_add_failed";
  public static final String FOREIGN_KEY_ADD_FAILED_MESSAGE =
      "Adding of foreign key reference failed";

  private SchemaMetadata schema;

  private String tableName = null;
  private Map<String, Column> columns = new LinkedHashMap<>();
  private List<String[]> uniques = new ArrayList<>();
  private String[] primaryKey = new String[0];
  private String inherits = null;

  public TableMetadata(SchemaMetadata schema, String tableName) {
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
    if (columns.get(column.getColumnName()) != null) {
      throw new MolgenisException(
          "invalid_column",
          "Invalid column",
          String.format(
              "Column with columnName='%s' already exist in table '%s'",
              column.getColumnName(), getTableName()));
    }
    if (inherits != null && getInheritedTable().getColumn(column.getColumnName()) != null) {
      throw new MolgenisException(
          "invalid_column",
          "Invalid column",
          String.format(
              "Column with columnName='%s' already exist in table '%s' because it got inherited from table '%s'",
              column.getColumnName(), getTableName(), inherits));
    }
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
    String[] primaryKeys = getPrimaryKey();
    if (primaryKeys.length != 1)
      throw new MolgenisException(
          FOREIGN_KEY_ADD_FAILED,
          FOREIGN_KEY_ADD_FAILED_MESSAGE,
          "Adding of foreign key reference "
              + name
              + "from table '"
              + getTableName()
              + "' to table '"
              + toTable
              + "' failed: '"
              + toTable
              + "' has no suitable primary key/unique defined.");
    return this.addRef(name, toTable, primaryKeys[0]);
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
    String[] primaryKeys = getPrimaryKey();
    if (primaryKeys.length != 1)
      throw new MolgenisException(
          FOREIGN_KEY_ADD_FAILED,
          FOREIGN_KEY_ADD_FAILED_MESSAGE,
          "Adding of array reference tableName='"
              + name
              + "' failed because no suitable primary key defined. Add primary key or use explicit toColumn.");
    return this.addRefArray(name, toTable, primaryKeys[0]);
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
    if (getColumn(name) == null) {
      throw new MolgenisException(
          "remove_column_failed",
          "Remove column failed",
          String.format(
              "Column with columnName='%s' doesn't exist in table '%s'", name, getTableName()));
    }
    columns.remove(name);
  }

  public Collection<String[]> getUniques() {
    return Collections.unmodifiableCollection(uniques);
  }

  public TableMetadata addUnique(String... columnNames) {
    for (String columnName : columnNames) {
      Column c = getColumn(columnName);
      if (c == null)
        throw new MolgenisException(
            "invalid_unique",
            "Add or update of unique constraint failed",
            "Addition of unique failed because column '"
                + columnName
                + "' is not known in table "
                + getTableName());
    }
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
    return equalContents(names, this.primaryKey);
  }

  public void removeUnique(String... keys) {
    for (int i = 0; i < uniques.size(); i++) {
      if (equalContents(uniques.get(i), keys)) {
        uniques.remove(i);
        break;
      }
    }
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
  }

  public void clearCache() {
    columns = new LinkedHashMap<>();
    uniques = new ArrayList<>();
    primaryKey = new String[0];
    inherits = null;
  }
}
