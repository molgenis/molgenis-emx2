package org.molgenis.metadata;

import com.jsoniter.annotation.JsonIgnore;
import org.molgenis.MolgenisException;

import static org.molgenis.metadata.Type.*;

import java.util.*;

public class TableMetadata {
  public static final String INVALID_FOREIGN_KEY = "invalid_foreign_key";
  public static final String ADDING_OF_FOREIGN_KEY_REFERENCE_FAILED =
      "Adding of foreign key reference failed";

  @JsonIgnore private SchemaMetadata schema;

  private String name;
  protected Map<String, ColumnMetadata> columns;
  protected List<String[]> uniques;
  protected String[] primaryKey;

  public TableMetadata(SchemaMetadata schema, String name) {
    this.clearCache();
    this.schema = schema;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @JsonIgnore
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

  public List<ColumnMetadata> getColumns() {
    ArrayList<ColumnMetadata> result = new ArrayList<>();
    result.addAll(columns.values());
    return Collections.unmodifiableList(result);
  }

  public ColumnMetadata getColumn(String name) throws MolgenisException {
    if (columns.containsKey(name)) return columns.get(name);
    throw new MolgenisException(
        "undefined_column",
        "Column could not be found",
        String.format("Column with name='%s' could not be found", name));
  }

  public ColumnMetadata addColumn(String name) throws MolgenisException {
    return this.addColumn(name, STRING);
  }

  public ColumnMetadata addColumn(String name, Type type) throws MolgenisException {
    ColumnMetadata c = new ColumnMetadata(this, name, type);
    columns.put(name, c);
    return c;
  }

  public ColumnMetadata addColumn(ColumnMetadata column) throws MolgenisException {
    columns.put(column.getName(), column);
    return column;
  }

  public ColumnMetadata addRef(String name, String toTable) throws MolgenisException {
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

  public ColumnMetadata addRef(String name, String toTable, String toColumn)
      throws MolgenisException {
    ColumnMetadata c = new ColumnMetadata(this, name, REF).setReference(toTable, toColumn);
    columns.put(name, c);
    return c;
  }

  public ColumnMetadata addRefArray(String name, String toTable, String toColumn)
      throws MolgenisException {
    ColumnMetadata c = new ColumnMetadata(this, name, REF_ARRAY).setReference(toTable, toColumn);
    columns.put(name, c);
    return c;
  }

  public ColumnMetadata addRefArray(String name, String toTable) throws MolgenisException {
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

  public ReferenceMultiple addRefMultiple(String... name) throws MolgenisException {
    return new ReferenceMultiple(this, REF, name);
  }

  public ReferenceMultiple addRefArrayMultiple(String... name) throws MolgenisException {
    return new ReferenceMultiple(this, REF_ARRAY, name);
  }

  public ColumnMetadata addMref(
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTableName)
      throws MolgenisException {
    ColumnMetadata c =
        new ColumnMetadata(this, name, MREF)
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
      ColumnMetadata c = getColumn(columnName);
      if (c == null)
        throw new MolgenisException(
            "invalid_unique",
            "Add or update of unique constraint failed",
            "Addition of unique failed because column '"
                + columnName
                + "' is not known in table "
                + getName());
    }
    uniques.add(columnNames);
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

  public void removeUnique(String... keys) throws MolgenisException {
    if (keys.length == 1 && MOLGENISID.equals(keys[0]))
      throw new MolgenisException(
          "invalid_unique",
          "Removal of unique failed",
          "You are not allowed to remove unique constraint on system column '" + MOLGENISID + "");
    for (int i = 0; i < uniques.size(); i++) {
      if (Arrays.equals(uniques.get(i), keys)) {
        uniques.remove(i);
      }
    }
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("TABLE(").append(getName()).append("){");
    for (ColumnMetadata c : getColumns()) {
      builder.append("\n\t").append(c.toString());
    }
    for (String[] u : getUniques()) {
      builder.append("\n\t").append(u.toString());
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
}
