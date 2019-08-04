package org.molgenis.beans;

import org.molgenis.*;

import static org.molgenis.Type.MREF;
import static org.molgenis.Type.REF;
import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.REF_ARRAY;

import java.util.*;

public class TableBean extends IdentifiableBean implements Table {
  private String name;
  private Schema schema;
  private String extend;
  protected Map<String, Column> columns = new LinkedHashMap<>();
  protected Map<String, Unique> uniques = new LinkedHashMap<>();

  public TableBean(Schema schema, String name) {
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
  public Collection<Column> getColumns() {
    return Collections.unmodifiableCollection(columns.values());
  }

  @Override
  public Column getColumn(String name) throws MolgenisException {
    if (columns.containsKey(name)) return columns.get(name);
    throw new MolgenisException(String.format("Column '%s' unknown", name));
  }

  @Override
  public Column addColumn(String name, Type type) throws MolgenisException {
    Column c = new ColumnBean(this, name, type, false);
    columns.put(name, c);
    return c;
  }

  @Override
  public Column addRef(String name, String refTable) throws MolgenisException {
    return this.addRef(name, refTable, MOLGENISID);
  }

  @Override
  public Column addRef(String name, String otherTable, String otherColumn)
      throws MolgenisException {
    Column c = new ColumnBean(this, name, REF, otherTable, otherColumn, false);
    columns.put(name, c);
    return c;
  }

  @Override
  public Column addRefArray(String name, String otherTable, String otherColumn)
      throws MolgenisException {
    Column c = new ColumnBean(this, name, REF_ARRAY, otherTable, otherColumn, false);
    columns.put(name, c);
    return c;
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
        new ColumnBean(
            this, name, MREF, refTable, refColumn, reverseName, reverseRefColumn, joinTableName);
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
  public Unique addUnique(String... names) throws MolgenisException {
    List<Column> cols = new ArrayList<>();
    for (String name : names) {
      Column c = getColumn(name);
      if (c == null) throw new MolgenisException("Unique unknown: " + names);
      cols.add(c);
    }
    String uniqueName = name + "_" + String.join("_", names) + "_UNIQUE";
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
        "getUniqueName(" + keyList + ") failed: constraint unknown in table " + this.name);
  }

  @Override
  public void removeUnique(String... keys) throws MolgenisException {
    if (keys.length == 1 && MOLGENISID.equals(keys[0]))
      throw new MolgenisException(
          "You are not allowed to remove unique constraint on primary key path " + MOLGENISID);
    String uniqueName = getUniqueName(keys);
    uniques.remove(uniqueName);
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
  public String getExtend() {
    return extend;
  }

  @Override
  public void setExtend(String extend) {
    this.extend = extend;
  }

  @Override
  public int insert(Collection<Row> rows) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int insert(Row... row) throws MolgenisException {
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
  public void enableSearch() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void enableRowLevelSecurity() throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Query query() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Select select(String... path) {
    return query().select(path);
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSchemaName() {
    return getSchema().getName();
  }
}
