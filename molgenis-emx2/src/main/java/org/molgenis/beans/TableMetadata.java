package org.molgenis.beans;

import org.molgenis.*;

import static org.molgenis.Type.*;

import java.util.*;

public class TableMetadata implements Table {
  public static final String INVALID_FOREIGN_KEY = "invalid_foreign_key";
  public static final String ADDING_OF_FOREIGN_KEY_REFERENCE_FAILED =
      "Adding of foreign key reference failed";
  private String name;
  private Schema schema;
  protected Map<String, Column> columns = new LinkedHashMap<>();
  protected Map<String, Unique> uniques = new LinkedHashMap<>();
  protected String[] primaryKey = new String[0];

  public TableMetadata(Schema schema, String name) {
    super();
    this.schema = schema;
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Schema getSchema() {
    return schema;
  }

  @Override
  public Table setPrimaryKey(String... columnNames) throws MolgenisException {
    this.primaryKey = columnNames;
    return this;
  }

  @Override
  public String[] getPrimaryKey() {
    return this.primaryKey;
  }

  @Override
  public List<Column> getColumns() {
    ArrayList<Column> result = new ArrayList<>();
    result.addAll(columns.values());
    return Collections.unmodifiableList(result);
  }

  @Override
  public Column getColumn(String name) throws MolgenisException {
    if (columns.containsKey(name)) return columns.get(name);
    throw new MolgenisException(
        "undefined_column",
        "Column could not be found",
        String.format("Column with name='%s' could not be found", name));
  }

  @Override
  public Column addColumn(String name) throws MolgenisException {
    return this.addColumn(name, STRING);
  }

  @Override
  public Column addColumn(String name, Type type) throws MolgenisException {
    Column c = new ColumnMetadata(this, name, type);
    columns.put(name, c);
    return c;
  }

  @Override
  public Column addColumn(Column column) throws MolgenisException {
    columns.put(column.getName(), column);
    return column;
  }

  @Override
  public Column addRef(String name, String toTable) throws MolgenisException {
    String[] primaryKeys = getPrimaryKey();
    if (primaryKeys.length != 1)
      throw new MolgenisException(
          INVALID_FOREIGN_KEY,
          ADDING_OF_FOREIGN_KEY_REFERENCE_FAILED,
          "Adding of foreign key reference with name='"
              + name
              + "' and default toColumn failed because no suitable primary key is defined. Add primary key or use explicit toColumn.");
    return this.addRef(name, toTable, primaryKeys[0]);
  }

  @Override
  public Column addRef(String name, String toTable, String toColumn) throws MolgenisException {
    Column c = new ColumnMetadata(this, name, REF).setReference(toTable, toColumn);
    columns.put(name, c);
    return c;
  }

  @Override
  public Column addRefArray(String name, String toTable, String toColumn) throws MolgenisException {
    Column c = new ColumnMetadata(this, name, REF_ARRAY).setReference(toTable, toColumn);
    columns.put(name, c);
    return c;
  }

  @Override
  public Column addRefArray(String name, String toTable) throws MolgenisException {
    String[] primaryKeys = getPrimaryKey();
    if (primaryKeys.length != 1)
      throw new MolgenisException(
          INVALID_FOREIGN_KEY,
          ADDING_OF_FOREIGN_KEY_REFERENCE_FAILED,
          "Adding of array reference name='"
              + name
              + "' failed because no suitable primary key defined. Add primary key or use explicit toColumn.");
    return this.addRefArray(name, toTable, primaryKeys[0]);
  }

  @Override
  public ReferenceMultiple addRefMultiple(String... name) throws MolgenisException {
    return new ReferenceMultipleBean(this, REF, name);
  }

  @Override
  public ReferenceMultiple addRefArrayMultiple(String... name) throws MolgenisException {
    return new ReferenceMultipleBean(this, REF_ARRAY, name);
  }

  @Override
  public Column addMref(
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTableName)
      throws MolgenisException {
    Column c =
        new ColumnMetadata(this, name, MREF)
            .setReference(refTable, refColumn)
            .setReverseReference(reverseName, reverseRefColumn)
            .setJoinTable(joinTableName);
    columns.put(name, c);
    return c;
  }

  @Override
  public void removeColumn(String name) throws MolgenisException {
    columns.remove(name);
  }

  @Override
  public Collection<Unique> getUniques() {
    return Collections.unmodifiableCollection(uniques.values());
  }

  @Override
  public Unique addUnique(String... columnNames) throws MolgenisException {
    List<Column> cols = new ArrayList<>();
    for (String columnName : columnNames) {
      Column c = getColumn(columnName);
      if (c == null)
        throw new MolgenisException(
            "invalid_unique",
            "Add or update of isUnique constraint failed",
            "Addition of isUnique failed because column '"
                + columnName
                + "' is not known in table "
                + getName());
      cols.add(c);
    }
    String uniqueName = name + "_" + String.join("_", columnNames) + "_UNIQUE";
    Unique u = new UniqueBean(this, cols);
    uniques.put(uniqueName, u);
    return u;
  }

  @Override
  public boolean isUnique(String... names) {
    try {
      getUniqueName(names);
      return true;
    } catch (MolgenisException e) {
      return false;
    }
  }

  public String getUniqueName(String... keys) throws MolgenisException {
    List<String> keyList = Arrays.asList(keys);
    for (Map.Entry<String, Unique> el : this.uniques.entrySet()) {
      if (el.getValue().getColumnNames().size() == keyList.size()
          && el.getValue().getColumnNames().containsAll(keyList)) {
        return el.getKey();
      }
    }
    throw new MolgenisException(
        "invalid_unique",
        "GetUniqueName failed",
        "Unique constraint with columns " + keyList + " is not known in table " + this.name);
  }

  @Override
  public void removeUnique(String... keys) throws MolgenisException {
    if (keys.length == 1 && MOLGENISID.equals(keys[0]))
      throw new MolgenisException(
          "invalid_unique",
          "Removal of isUnique failed",
          "You are not allowed to remove isUnique constraint on system column '" + MOLGENISID + "");
    String uniqueName = getUniqueName(keys);
    uniques.remove(uniqueName);
  }

  @Override
  public int insert(Row... row) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int insert(Collection<Row> rows) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int update(Row... row) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int update(Collection<Row> rows) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(Row... row) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(Collection<Row> rows) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteByPrimaryKey(Object... name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Select select(String... path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Where where(String... path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Query query() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <E> List<E> retrieve(String columnName, Class<E> klazz) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void enableSearch() {

    throw new UnsupportedOperationException();
  }

  @Override
  public void enableRowLevelSecurity() throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("TABLE(").append(getName()).append("){");
    for (Column c : getColumns()) {
      builder.append("\n\t").append(c.toString());
    }
    for (Unique u : getUniques()) {
      builder.append("\n\t").append(u.toString());
    }
    builder.append("\n}");
    return builder.toString();
  }

  @Override
  public String getSchemaName() {
    return getSchema().getName();
  }
}
