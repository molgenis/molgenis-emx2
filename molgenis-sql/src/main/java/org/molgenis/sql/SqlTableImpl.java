package org.molgenis.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.sql.SqlRow.MOLGENISID;
import static org.molgenis.sql.SqlType.MREF;

class SqlTableImpl implements SqlTable {
  private DSLContext sql;
  private SqlDatabaseImpl db;
  private String name;
  private Map<String, SqlColumnImpl> columnMap = new LinkedHashMap<>();
  private Map<String, SqlUnique> uniquesMap = new LinkedHashMap<>();

  SqlTableImpl(SqlDatabaseImpl db, DSLContext sql, String name) throws SqlDatabaseException {
    this.db = db;
    this.sql = sql;
    this.name = name;
    reloadMetaData();
  }

  private void reloadMetaData() throws SqlDatabaseException {
    Table t = sql.meta().getTables(name).get(0);
    reloadColumns(t);
    reloadUniques(t);
    reloadReferences(t);
  }

  private void reloadColumns(Table t) {
    columnMap = new LinkedHashMap<>();
    for (Field field : t.fields()) { //
      columnMap.put(field.getName(), new SqlColumnImpl(sql, this, field));
    }
  }

  private void reloadReferences(Table t) throws SqlDatabaseException {
    for (Object o3 : t.getReferences()) {
      ForeignKey fk = (ForeignKey) o3;
      for (Field field : (List<Field>) fk.getFields()) {
        SqlColumnImpl temp = columnMap.get(field.getName());
        SqlColumnImpl fkey = null;
        String refTableName = fk.getKey().getTable().getName();
        if (refTableName.equals(name)) {
          fkey = new SqlColumnImpl(sql, this, field, this);
        } else {
          fkey = new SqlColumnImpl(sql, this, field, db.getTable(refTableName));
        }
        fkey.setNullable(temp.isNullable());
        columnMap.put(field.getName(), fkey);
      }
    }
  }

  private void reloadMrefs(Table t) {
    // TODO mrefs
  }

  @Override
  public Collection<SqlColumn> getColumns() {
    Collection<SqlColumn> cols = new ArrayList<>();
    cols.addAll(columnMap.values());
    return Collections.unmodifiableCollection(cols);
  }

  @Override
  public SqlColumn getColumn(String name) {
    return columnMap.get(name);
  }

  @Override
  public SqlColumn addColumn(String name, SqlType sqlType) throws SqlDatabaseException {
    if (SqlType.REF.equals(sqlType)) {
      throw new SqlDatabaseException(
          "addColumn(name,REF) not allowed. Use addColumn(name, otherTable) to add foreign key fields");
    }
    DataType type = SqlTypeUtils.typeOf(sqlType);
    Field field = field(name(name), type.nullable(false));
    sql.alterTable(name(this.name)).addColumn(field).execute();
    columnMap.put(name, new SqlColumnImpl(sql, this, field, null));
    return getColumn(name);
  }

  @Override
  public SqlColumn addRef(String name, SqlTable otherTable) {
    Field field = field(name(name), SQLDataType.UUID.nullable(false));

    sql.alterTable(name(this.name)).addColumn(field).execute();
    sql.alterTable(name(this.name))
        .add(
            constraint(name(this.name) + "_" + name(name) + "_FK")
                .foreignKey(name(name))
                .references(name(otherTable.getName()), name(MOLGENISID)))
        .execute();
    sql.createIndex(name(this.name) + "_" + name(name) + "_FKINDEX")
        .on(table(name(this.name)), field)
        .execute();
    columnMap.put(name, new SqlColumnImpl(sql, this, field, otherTable));
    return getColumn(name);
  }

  @Override
  public SqlColumn addMref(String name, SqlTable otherTable, String otherColumn)
      throws SqlDatabaseException {
    SqlColumn check = columnMap.get(name);
    if (check != null && MREF.equals(check.getType()) && otherTable.equals(check.getRefTable())) {
      // todo check
    } else {
      String joinTable = this.getName() + name + "MREF" + otherTable.getName() + otherColumn;
      SqlTable jTable = db.createTable(joinTable);
      jTable.addRef(otherColumn, this);
      jTable.addRef(name, otherTable);
      columnMap.put(name, new SqlColumnImpl(sql, this, name, otherTable, jTable, otherColumn));
      // add reverse link
      otherTable.addMref(otherColumn, this, name);
    }
    return getColumn(name);
  }

  @Override
  public void removeColumn(String name) throws SqlDatabaseException {
    if (MOLGENISID.equals(name))
      throw new SqlDatabaseException(
          "You are not allowed to remove primary key column " + MOLGENISID);
    sql.alterTable(name(this.name)).dropColumn(field(name(name))).execute();
    columnMap.remove(name);
  }

  private void reloadUniques(Table t) {
    uniquesMap = new LinkedHashMap<>();
    for (Index i : (List<Index>) t.getIndexes()) {
      // skip non-unique indexes
      if (i.getUnique()) {
        List<SqlColumn> cols = new ArrayList<>();
        for (SortField sf : i.getFields()) {
          cols.add(getColumn(sf.getName()));
        }
        uniquesMap.put(i.getName(), new SqlUniqueImpl(this, cols));
      }
    }
  }

  @Override
  public Collection<SqlUnique> getUniques() {
    return Collections.unmodifiableCollection(uniquesMap.values());
  }

  private String getUniqueName(String... keys) throws SqlDatabaseException {
    List<String> keyList = Arrays.asList(keys);
    for (Map.Entry<String, SqlUnique> el : this.uniquesMap.entrySet()) {
      if (el.getValue().getColumns().size() == keyList.size()
          && el.getValue().getColumnNames().containsAll(keyList)) {
        return el.getKey();
      }
    }
    throw new SqlDatabaseException(
        "getUniqueName(" + keyList + ") failed: constraint unknown in table " + this.name);
  }

  public SqlUnique addUnique(String... keys) throws SqlDatabaseException {
    List<SqlColumn> uniqueColumns = new ArrayList<>();
    for (String key : keys) {
      SqlColumn col = getColumn(key);
      if (col == null)
        throw new SqlDatabaseException(
            "addUnique(" + keys + ") failed: column '" + key + "' unknown in table " + this.name);
      uniqueColumns.add(col);
    }
    String uniqueName = name + "_" + String.join("_", keys) + "_UNIQUE";
    sql.alterTable(name(name)).add(constraint(name(uniqueName)).unique(keys)).execute();
    uniquesMap.put(uniqueName, new SqlUniqueImpl(this, uniqueColumns));
    return this.uniquesMap.get(getUniqueName(keys));
  }

  @Override
  public boolean isUnique(String... keys) {
    try {
      getUniqueName(keys);
      return true;
    } catch (SqlDatabaseException e) {
      return false;
    }
  }

  @Override
  public void removeUnique(String... keys) throws SqlDatabaseException {
    if (keys.length == 1 && MOLGENISID.equals(keys[0]))
      throw new SqlDatabaseException(
          "You are not allowed to remove unique constraint on primary key column " + MOLGENISID);
    String uniqueName = getUniqueName(keys);
    sql.alterTable(name(this.name)).dropConstraint(name(uniqueName)).execute();
    uniquesMap.remove(uniqueName);
  }

  public String getName() {
    return name;
  }

  @Override
  public void insert(Collection<SqlRow> rows) throws SqlDatabaseException {
    try {
      // get metadata
      List<Field> fields = new ArrayList<>();
      List<String> fieldNames = new ArrayList<>();
      int i = 0;
      for (SqlColumnImpl c : columnMap.values()) {
        if (!MREF.equals(c.getType())) {
          fieldNames.add(c.getName());
          fields.add(c.getJooqField());
        }
        i++;
      }
      InsertValuesStepN step =
          sql.insertInto(table(name(name)), fields.toArray(new Field[fields.size()]));
      for (SqlRow row : rows) {
        step.values(row.values(fieldNames.toArray(new String[fieldNames.size()])));
      }
      step.execute();
      // save the mrefs
      for (SqlColumnImpl c : columnMap.values()) {
        if (MREF.equals(c.getType())) {
          saveUpdatedMrefs(rows, c);
        }
      }
    } catch (DataAccessException e) {
      throw new SqlDatabaseException(e.getCause().getMessage());
    }
  }

  @Override
  public void insert(SqlRow row) throws SqlDatabaseException {
    this.insert(Arrays.asList(row));
  }

  public int update(Collection<SqlRow> rows) throws SqlDatabaseException {
    // keep batchsize smaller to limit memory footprint
    int batchSize = 1000;

    // get metadata
    Field[] fields = new Field[columnMap.size()];
    String[] fieldNames = new String[columnMap.size()];
    int i = 0;
    for (SqlColumnImpl c : columnMap.values()) {
      fieldNames[i] = c.getName();
      fields[i] = c.getJooqField();
      i++;
    }
    // execute in batches
    int count = 0;
    List<SqlRow> batch = new ArrayList<>();
    for (SqlRow row : rows) {
      batch.add(row);
      count++;
      if (count % batchSize == 0) {
        updateBatch(batch, table(name(name)), fields, fieldNames);
        batch.clear();
      }
    }
    updateBatch(batch, table(name(name)), fields, fieldNames);
    return count;
  }

  private void updateBatch(Collection<SqlRow> rows, Table t, Field[] fields, String[] fieldNames) {
    if (!rows.isEmpty()) {
      // create multi-value insert
      InsertValuesStepN step = sql.insertInto(t, fields);
      for (SqlRow row : rows) {
        step.values(row.values(fieldNames));
      }
      // on duplicate key update using same record via "excluded" keyword in postgres
      InsertOnDuplicateSetStep step2 = step.onConflict(field(MOLGENISID)).doUpdate();
      for (int i = 0; i < fieldNames.length; i++) {
        if (!MOLGENISID.equals(fieldNames[i])) {
          step2.set(
              field(name(fieldNames[i])),
              (Object) field(unquotedName("\"excluded\"." + name(fieldNames[i]))));
        }
      }
      step.execute();
    }
  }

  @Override
  public void update(SqlRow row) throws SqlDatabaseException {
    this.update(Arrays.asList(row));
  }

  @Override
  public int delete(Collection<SqlRow> rows) throws SqlDatabaseException {
    // because of expensive table scanning and smaller query string size this batch should be larger
    // than insert/update
    int batchSize = 100000;
    Table t = table(name(name));
    int count = 0;
    List<SqlRow> batch = new ArrayList<>();
    for (SqlRow row : rows) {
      batch.add(row);
      count++;
      if (count % batchSize == 0) {
        deleteBatch(batch, t);
        batch.clear();
      }
    }
    deleteBatch(batch, t);
    return count;
  }

  private void deleteBatch(Collection<SqlRow> rows, Table t) throws SqlDatabaseException {
    if (!rows.isEmpty()) {
      // remove the mrefs first
      for (SqlColumnImpl c : columnMap.values()) {
        if (MREF.equals(c.getType())) {
          this.deleteOldMrefs(rows, c);
        }
      }
      Field field = field(name(MOLGENISID), SQLDataType.UUID);
      List<UUID> idList = new ArrayList<>();
      rows.forEach(row -> idList.add(row.getRowID()));
      sql.deleteFrom(t).where(field.in(idList)).execute();
    }
  }

  @Override
  public void delete(SqlRow row) throws SqlDatabaseException {
    this.delete(Arrays.asList(row));
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("TABLE(").append(name).append("){");
    for (SqlColumn c : getColumns()) {
      builder.append("\n\t").append(c.toString());
    }
    for (SqlUnique u : getUniques()) {
      builder.append("\n\t").append(u.toString());
    }
    builder.append("\n}");
    return builder.toString();
  }

  private void deleteOldMrefs(Collection<SqlRow> rows, SqlColumn column)
      throws SqlDatabaseException {
    SqlTable joinTable = column.getMrefTable();
    List<UUID> oldMrefIds = new ArrayList<>();
    for (SqlRow r : rows) {
      oldMrefIds.add(r.getRowID());
    }
    List<SqlRow> oldMrefs =
        db.query(joinTable.getName())
            .eq(
                joinTable.getName(),
                column.getName(),
                oldMrefIds.toArray(new UUID[oldMrefIds.size()]))
            .retrieve();
    joinTable.delete(oldMrefs);
  }

  private void saveUpdatedMrefs(Collection<SqlRow> rows, SqlColumn column)
      throws SqlDatabaseException {
    String colName = column.getName();
    String joinTable = column.getMrefTable().getName();
    String otherColname = column.getMrefBack();

    List<SqlRow> newMrefs = new ArrayList<>();
    for (SqlRow r : rows) {
      for (UUID uuid : r.getMref(colName)) {
        SqlRow join =
            new SqlRow().setRef(column.getName(), uuid).setRef(otherColname, r.getRowID());
        newMrefs.add(join);
      }
    }
    db.getTable(joinTable).update(newMrefs);
  }
}
