package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.Type.*;

import java.util.*;

public class TableMetadata {
  public static final String FOREIGN_KEY_ADD_FAILED = "foreign_key_add_failed";
  public static final String FOREIGN_KEY_ADD_FAILED_MESSAGE =
      "Adding of foreign key reference failed";

  private SchemaMetadata schema;

  private String tableName;
  protected Map<String, Column> columns;
  protected List<String[]> uniques;
  protected String[] primaryKey;

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

  public TableMetadata setPrimaryKey(String... columnNames) throws MolgenisException {
    this.primaryKey = columnNames;
    return this;
  }

  public String[] getPrimaryKey() {
    return this.primaryKey;
  }

  public List<Column> getColumns() {
    ArrayList<Column> result = new ArrayList<>();
    result.addAll(columns.values());
    return Collections.unmodifiableList(result);
  }

  public Column getColumn(String name) throws MolgenisException {
    if (columns.containsKey(name)) return columns.get(name);
    throw new MolgenisException(
        "undefined_column",
        "Column could not be found",
        String.format("Column with tableName='%s' could not be found", name));
  }

  public Column addColumn(String name) throws MolgenisException {
    return this.addColumn(name, STRING);
  }

  public Column addColumn(String name, Type type) throws MolgenisException {
    Column c = new Column(this, name, type);
    columns.put(name, c);
    return c;
  }

  public Column addColumn(Column column) throws MolgenisException {
    columns.put(column.getColumnName(), column);
    return column;
  }

  public Column addRef(String name, String toTable) throws MolgenisException {
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

  public Column addRef(String name, String toTable, String toColumn) throws MolgenisException {
    Column c = new Column(this, name, REF).setReference(toTable, toColumn);
    columns.put(name, c);
    return c;
  }

  public Column addRefArray(String name, String toTable, String toColumn) throws MolgenisException {
    Column c = new Column(this, name, REF_ARRAY).setReference(toTable, toColumn);
    columns.put(name, c);
    return c;
  }

  public Column addRefArray(String name, String toTable) throws MolgenisException {
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

  public ReferenceMultiple addRefMultiple(String... name) throws MolgenisException {
    return new ReferenceMultiple(this, REF, name);
  }

  public ReferenceMultiple addRefArrayMultiple(String... name) throws MolgenisException {
    return new ReferenceMultiple(this, REF_ARRAY, name);
  }

  public Column addMref(
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTableName)
      throws MolgenisException {
    Column c =
        new Column(this, name, MREF)
            .setReference(refTable, refColumn)
            .setReverseReference(reverseName, reverseRefColumn)
            .setJoinTable(joinTableName);
    columns.put(name, c);
    return c;
  }

  public void removeColumn(String name) throws MolgenisException {
    columns.remove(name);
  }

  public Collection<String[]> getUniques() {
    return Collections.unmodifiableCollection(uniques);
  }

  public TableMetadata addUnique(String... columnNames) throws MolgenisException {
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
    if (this.getPrimaryKey().length == 0) this.setPrimaryKey(columnNames);
    else uniques.add(columnNames);
    return this;
  }

  public boolean isUnique(String... names) {
    for (String[] el : this.uniques) {
      if (Arrays.equals(names, el)) {
        return true;
      }
    }
    return false;
  }

  public boolean isPrimaryKey(String... names) {
    if (Arrays.equals(names, this.primaryKey)) {
      return true;
    }
    return false;
  }

  public void removeUnique(String... keys) throws MolgenisException {
    for (int i = 0; i < uniques.size(); i++) {
      if (Arrays.equals(uniques.get(i), keys)) {
        uniques.remove(i);
        break;
      }
    }
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

  public void enableRowLevelSecurity() throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  public void enableSearch() {
    throw new UnsupportedOperationException();
  }

  public void clearCache() {
    columns = new LinkedHashMap<>();
    uniques = new ArrayList<>();
    primaryKey = new String[0];
  }

  public void removePrimaryKey(String[] columnNames) {
    this.primaryKey = null;
  }
}
