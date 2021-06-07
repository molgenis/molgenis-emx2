package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.MutationType.*;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN;
import static org.molgenis.emx2.sql.SqlTypeUtils.getTypedValue;

import java.io.StringReader;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jooq.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SqlTable implements Table {
  private SqlDatabase db;
  private SqlTableMetadata metadata;
  private static Logger logger = LoggerFactory.getLogger(SqlTable.class);

  SqlTable(SqlDatabase db, SqlTableMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public org.molgenis.emx2.Schema getSchema() {
    return new SqlSchema(db, (SqlSchemaMetadata) metadata.getSchema());
  }

  @Override
  public SqlTableMetadata getMetadata() {
    return metadata;
  }

  public void copyOut(Writer writer) {
    db.getJooq()
        .connection(
            connection -> {
              try {
                CopyManager cm = new CopyManager(connection.unwrap(BaseConnection.class));
                String selectQuery =
                    "select "
                        + this.getMetadata().getLocalColumnNames().stream()
                            .map(c -> "\"" + c + "\"")
                            .collect(Collectors.joining(","))
                        + " from \""
                        + getSchema().getMetadata().getName()
                        + "\".\""
                        + getName()
                        + "\"";
                cm.copyOut(
                    "COPY (" + selectQuery + " ) TO STDOUT WITH (FORMAT CSV,HEADER )", writer);
              } catch (Exception e) {
                throw new MolgenisException("copyOut failed: ", e);
              }
            });
  }

  public void copyIn(Iterable<Row> rows) {
    db.getJooq()
        .connection(
            connection -> {
              try {
                CopyManager cm = new CopyManager(connection.unwrap(BaseConnection.class));

                // must be batched
                StringBuilder tmp = new StringBuilder();
                tmp.append(
                    this.getMetadata().getLocalColumnNames().stream()
                            .map(c -> "\"" + c + "\"")
                            .collect(Collectors.joining(","))
                        + "\n");
                for (Row row : rows) {
                  StringBuilder line = new StringBuilder();
                  for (Column c : this.getMetadata().getStoredColumns()) {
                    if (!row.containsName(c.getName())) {
                      line.append(",");
                    } else {
                      Object value = getTypedValue(row, c);
                      line.append(value + ",");
                    }
                  }
                  tmp.append(line.substring(0, line.length() - 1) + "\n");
                }

                String tableName =
                    "\"" + getSchema().getMetadata().getName() + "\".\"" + getName() + "\"";

                String columnNames =
                    "("
                        + this.getMetadata().getLocalColumnNames().stream()
                            .map(c -> "\"" + c + "\"")
                            .collect(Collectors.joining(","))
                        + ")";
                String sql = "COPY " + tableName + columnNames + " FROM STDIN (FORMAT CSV,HEADER )";
                cm.copyIn(sql, new StringReader(tmp.toString()));
              } catch (Exception e) {
                throw new MolgenisException("copyOut failed: ", e);
              }
            });
  }

  @Override
  public int insert(Row... rows) {
    return insert(Arrays.asList(rows));
  }

  @Override
  public int insert(Iterable<Row> rows) {
    try {
      return executeTransaction(rows, INSERT);
    } catch (Exception e) {
      throw new SqlMolgenisException("Update into table '" + getName() + "' failed.", e);
    }
  }

  @Override
  public int update(Row... rows) {
    return update(Arrays.asList(rows));
  }

  @Override
  public int update(Iterable<Row> rows) {
    try {
      return this.executeTransaction(rows, UPDATE);
    } catch (Exception e) {
      throw new SqlMolgenisException("Update into table '" + getName() + "' failed.", e);
    }
  }

  @Override
  public int save(Row... rows) {
    return save(Arrays.asList(rows));
  }

  @Override
  public int save(Iterable<Row> rows) {
    try {
      return this.executeTransaction(rows, SAVE);
    } catch (Exception e) {
      throw new SqlMolgenisException("Upsert into table '" + getName() + "' failed.", e);
    }
  }

  @Override
  public void truncate() {
    // if part of inheritance tree then only delete the relevant part
    if (getMetadata().getLocalColumn(MG_TABLECLASS) != null) {
      this.truncate(getMgTableClass(this.getMetadata()));
    }
    // in normal table it is a real truncate
    else {
      db.getJooq().truncate(getJooqTable()).execute();
    }
    // in case inherited we must also truncate parent
    if (getMetadata().getInherit() != null) {
      getInheritedTable().truncate(getMgTableClass(this.getMetadata()));
    }
  }

  private void truncate(String mg_table) {
    if (getMetadata().getInherit() != null) {
      getInheritedTable().truncate(mg_table);
    }
    db.getJooq().deleteFrom(getJooqTable()).where(field(MG_TABLECLASS).equal(mg_table)).execute();
  }

  private String getMgTableClass(TableMetadata table) {
    return table.getSchemaName() + "." + table.getTableName();
  }

  private int executeTransaction(Iterable<Row> rows, MutationType transactionType) {
    long start = System.currentTimeMillis();
    final AtomicInteger count = new AtomicInteger(0);
    final Map<String, List<Row>> subclassRows = new LinkedHashMap<>();
    final Map<String, Set<String>> columnsProvided = new LinkedHashMap<>();

    String tableClass = getMgTableClass(getMetadata());

    // validate
    if (getMetadata().getPrimaryKeys().isEmpty())
      throw new MolgenisException(
          "Transaction failed: Table "
              + getName()
              + " cannot process row insert/update/delete requests because no primary key is defined");

    db.tx(
        db2 -> {
          for (Row row : rows) {

            // set table class if not set, and see for first time
            if (row.notNull(MG_TABLECLASS)
                && !subclassRows.containsKey(row.getString(MG_TABLECLASS))) {

              // validate
              String tableName = row.getString(MG_TABLECLASS);
              if (!tableName.contains(".")) {
                if (this.getSchema().getTable(tableName) != null) {
                  row.setString(MG_TABLECLASS, getSchema().getName() + "." + tableName);
                } else {
                  throw new MolgenisException(
                      MG_TABLECLASS
                          + " value failed in row "
                          + count.get()
                          + ": found '"
                          + tableName
                          + "'");
                }
              } else {
                String schemaName = tableName.split("\\.")[0];
                String tableName2 = tableName.split("\\.")[1];
                if (this.getSchema().getDatabase().getSchema(schemaName) == null
                    || this.getSchema().getDatabase().getSchema(schemaName).getTable(tableName2)
                        == null) {
                  throw new MolgenisException(
                      "invalid value in column '"
                          + MG_TABLECLASS
                          + "' on row "
                          + count.get()
                          + ": found '"
                          + tableName
                          + "'");
                }
              }
            } else {
              row.set(MG_TABLECLASS, tableClass);
            }

            // create batches for each table class
            String subclassName = row.getString(MG_TABLECLASS);
            if (!subclassRows.containsKey(subclassName)) {
              subclassRows.put(subclassName, new ArrayList<>());
            }

            // check columns provided didn't change
            if (columnsProvided.get(subclassName) == null) {
              columnsProvided.put(subclassName, new LinkedHashSet<>(row.getColumnNames()));
            }

            // execute batch if 1000 rows, or columns provided changes
            if (columnsProvidedAreDifferent(columnsProvided.get(subclassName), row)
                || subclassRows.get(subclassName).size() >= 1000) {
              executeBatch(
                  transactionType,
                  count,
                  subclassRows,
                  subclassName,
                  columnsProvided.get(subclassName));
              // reset columns provided
              columnsProvided.get(subclassName).clear();
              columnsProvided.get(subclassName).addAll(row.getColumnNames());
            }

            // add to batch list, and execute if batch is large enough
            subclassRows.get(subclassName).add(row);
          }

          // execute any remaining batches
          for (Map.Entry<String, List<Row>> batch : subclassRows.entrySet()) {
            if (batch.getValue().size() > 0) {
              executeBatch(
                  transactionType,
                  count,
                  subclassRows,
                  batch.getKey(),
                  columnsProvided.get(batch.getKey()));
            }
          }
        });

    log(start, count, transactionType.name().toLowerCase() + "d (incl subclass if applicable)");
    return count.get();
  }

  private void checkRequired(Row row, Collection<Column> columns) {
    for (Column c : columns) {
      if (c.isRequired() && row.isNull(c.getName(), c.getColumnType())) {
        throw new MolgenisException("column '" + c.getName() + "' is required in " + row);
      }
    }
  }

  private boolean columnsProvidedAreDifferent(Set<String> columnsProvided, Row row) {
    if (columnsProvided.size() == 0 || columnsProvided.equals(row.getColumnNames())) {
      return false;
    } else {
      return true;
    }
  }

  private void executeBatch(
      MutationType transactionType,
      AtomicInteger count,
      Map<String, List<Row>> subclassRows,
      String subclassName,
      Set<String> columnsProvided) {

    // execute
    SqlTable table = (SqlTable) getSchema().getTable(subclassName.split("\\.")[1]);
    if (UPDATE.equals(transactionType)) {
      count.set(count.get() + table.updateBatch(subclassRows.get(subclassName), columnsProvided));
    } else if (SAVE.equals(transactionType)) {
      count.set(
          count.get() + table.insertBatch(subclassRows.get(subclassName), true, columnsProvided));
    } else if (INSERT.equals(transactionType)) {
      count.set(
          count.get() + table.insertBatch(subclassRows.get(subclassName), false, columnsProvided));
    } else {
      throw new MolgenisException(
          "Internal error in executeBatch: transaction type "
              + transactionType
              + " not allowed here");
    }
    // clear the list
    subclassRows.get(subclassName).clear();
  }

  private int insertBatch(List<Row> rows, boolean updateOnConflict, Set<String> updateColumns) {
    boolean inherit = getMetadata().getInherit() != null;
    if (inherit) {
      getInheritedTable().insertBatch(rows, updateOnConflict, updateColumns);
    }

    // get metadata
    Set<Column> columns = getColumnsToBeUpdated(updateColumns);
    List<Column> allColumns = getMetadata().getMutationColumns();
    List<Field> insertFields =
        columns.stream().map(c -> c.getJooqField()).collect(Collectors.toList());
    if (!inherit) {
      insertFields.add(field(name(MG_INSERTEDBY)));
      insertFields.add(field(name(MG_INSERTEDON)));
      insertFields.add(field(name(MG_UPDATEDBY)));
      insertFields.add(field(name(MG_UPDATEDON)));
    }

    // define the insert step
    InsertValuesStepN<org.jooq.Record> step =
        db.getJooq().insertInto(getJooqTable(), insertFields.toArray(new Field[0]));

    // add all the rows as steps
    String user = getSchema().getDatabase().getActiveUser();
    if (user == null) {
      user = ADMIN;
    }
    LocalDateTime now = LocalDateTime.now();
    for (Row row : rows) {
      // when insert, we should include all columns, not only 'updateColumns'
      if (!row.isDraft()) {
        checkRequired(row, allColumns);
      }
      // get values
      Map values = SqlTypeUtils.getValuesAsMap(row, columns);
      if (!inherit) {
        values.put(MG_INSERTEDBY, user);
        values.put(MG_INSERTEDON, now);
        values.put(MG_UPDATEDBY, user);
        values.put(MG_UPDATEDON, now);
      }
      step.values(values.values());
    }

    // optionally, add conflict clause
    if (updateOnConflict) {
      InsertOnDuplicateSetStep<org.jooq.Record> step2 =
          step.onConflict(getMetadata().getPrimaryKeyFields().toArray(new Field[0])).doUpdate();
      for (Column column : columns) {
        step2.set(
            column.getJooqField(),
            (Object) field(unquotedName("excluded.\"" + column.getName() + "\"")));
      }
      if (!inherit) {
        step2.set(field(name(MG_UPDATEDBY)), user);
        step2.set(field(name(MG_UPDATEDON)), now);
      }
    }

    return step.execute();
  }

  private Set<Column> getColumnsToBeUpdated(Set<String> updateColumns) {
    return getMetadata().getMutationColumns().stream()
        .filter(
            c ->
                !(c.getName().equals(MG_INSERTEDBY)
                        || c.getName().equals(MG_INSERTEDON)
                        || c.getName().equals(MG_UPDATEDBY)
                        || c.getName().equals(MG_UPDATEDON))
                    && (c.getComputed() != null
                        || updateColumns.size() == 0
                        || updateColumns.contains(c.getName())))
        .collect(Collectors.toSet());
  }

  private int updateBatch(List<Row> rows, Set<String> updateColumns) {
    boolean inherit = getMetadata().getInherit() != null;
    if (inherit) {
      getInheritedTable().updateBatch(rows, updateColumns);
    }

    // get metadata
    Set<Column> columns = getColumnsToBeUpdated(updateColumns);
    List<Column> pkeyFields = getMetadata().getPrimaryKeyColumns();

    // create batch of updates
    List<UpdateConditionStep> list = new ArrayList();
    String user = getSchema().getDatabase().getActiveUser();
    if (user == null) {
      user = ADMIN;
    }
    LocalDateTime now = LocalDateTime.now();
    for (Row row : rows) {
      Map values = SqlTypeUtils.getValuesAsMap(row, columns);
      if (!inherit) {
        values.put(MG_UPDATEDBY, user);
        values.put(MG_UPDATEDON, now);
      }

      if (!row.isDraft()) {
        checkRequired(row, columns);
      }

      list.add(
          db.getJooq()
              .update(getJooqTable())
              .set(values)
              .where(getUpdateCondition(row, pkeyFields)));
    }

    return Arrays.stream(db.getJooq().batch(list).execute()).reduce(Integer::sum).getAsInt();
  }

  private Condition getUpdateCondition(Row row, List<Column> pkeyFields) {
    List<Condition> result = new ArrayList<>();
    for (Column key : pkeyFields) {
      result.add(key.getJooqField().eq(row.get(key)));
    }
    return and(result);
  }

  @Override
  public int delete(Iterable<Row> rows) {
    long start = System.currentTimeMillis();

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.tx(
          config -> {
            // first update superclass
            if (getMetadata().getInherit() != null) {
              getInheritedTable().delete(rows);
            }

            // because of expensive jTable scanning and smaller queryOld string size this batch
            // should be
            // larger
            // than insert/update
            int batchSize = 100000;
            List<Row> batch = new ArrayList<>();
            for (Row row : rows) {
              batch.add(row);
              count.set(count.get() + 1);
              if (count.get() % batchSize == 0) {
                deleteBatch(batch);
                batch.clear();
              }
            }
            deleteBatch(batch);
          });
    } catch (Exception e) {
      throw new SqlMolgenisException("Delete into table " + getName() + " failed.   ", e);
    }

    log(start, count, "deleted");

    return count.get();
  }

  @Override
  public Query select(SelectColumn... columns) {
    return query().select(columns);
  }

  @Override
  public Query agg(SelectColumn columns) {
    return agg().select(columns);
  }

  public Query where(Filter... filters) {
    return query().where(filters);
  }

  // @Override
  public Query search(String terms) {
    return query().search(terms);
  }

  @Override
  public int delete(Row... rows) {
    return delete(Arrays.asList(rows));
  }

  private void deleteBatch(Collection<Row> rows) {
    if (!rows.isEmpty()) {
      List<String> keyNames =
          getMetadata().getPrimaryKeyFields().stream()
              .map(Field::getName)
              .collect(Collectors.toList());

      // in case no primary key is defined, use all columns
      if (keyNames == null) {
        throw new MolgenisException("Delete on table " + getName() + " failed: no primary key set");
      }
      Condition whereCondition = getWhereConditionForBatchDelete(rows);
      db.getJooq().deleteFrom(getJooqTable()).where(whereCondition).execute();
    }
  }

  private Condition getWhereConditionForBatchDelete(Collection<Row> rows) {
    List<Condition> conditions = new ArrayList<>();
    for (Row r : rows) {
      List<Condition> rowCondition = new ArrayList<>();
      if (getMetadata().getPrimaryKeys().isEmpty()) {
        // when no key, use all columns as id
        for (Column keyPart : getMetadata().getStoredColumns()) {
          rowCondition.add(getColumnCondition(r, keyPart));
        }
      } else {
        for (Column keyPart : getMetadata().getPrimaryKeyColumns()) {
          rowCondition.add(getColumnCondition(r, keyPart));
        }
      }
      conditions.add(and(rowCondition));
    }
    return or(conditions);
  }

  private Condition getColumnCondition(Row r, Column key) {
    List<Condition> columnCondition = new ArrayList<>();
    if (REF.equals(key.getColumnType()) || REF_ARRAY.equals(key.getColumnType())
    //       || MREF.equals(key.getColumnType())
    ) {
      for (Reference ref : key.getReferences()) {
        if (!ref.isOverlapping()) {
          columnCondition.add(
              ref.getJooqField()
                  .eq(cast(r.get(ref.getName(), ref.getPrimitiveType()), ref.getJooqField())));
        }
      }
    } else if (REFBACK.equals(key.getColumnType())) {
      // do nothing
    } else {
      columnCondition.add(
          key.getJooqField()
              .eq(cast(r.get(key.getName(), key.getColumnType()), key.getJooqField())));
    }
    return and(columnCondition);
  }

  @Override
  public Query query() {
    return new SqlQuery((SqlSchemaMetadata) this.getMetadata().getSchema(), this.getName());
  }

  @Override
  public Query agg() {
    return new SqlQuery(
        (SqlSchemaMetadata) this.getMetadata().getSchema(), this.getName() + "_agg");
  }

  @Override
  public List<Row> retrieveRows() {
    return this.query().retrieveRows();
  }

  @Override
  public String getName() {
    return getMetadata().getTableName();
  }

  protected org.jooq.Table<org.jooq.Record> getJooqTable() {
    return table(name(metadata.getSchema().getName(), metadata.getTableName()));
  }

  private SqlTable getInheritedTable() {
    if (getMetadata().getImportSchema() != null) {
      return (SqlTable)
          getSchema()
              .getDatabase()
              .getSchema(getMetadata().getImportSchema())
              .getTable(getMetadata().getInherit());
    } else {
      return (SqlTable) getSchema().getTable(getMetadata().getInherit());
    }
  }

  private void log(long start, AtomicInteger count, String message) {
    String user = db.getActiveUser();
    if (user == null) user = "molgenis";
    if (logger.isInfoEnabled()) {
      logger.info(
          "{} {} {} rows into table {} in {}ms",
          user,
          message,
          count.get(),
          getJooqTable(),
          (System.currentTimeMillis() - start));
    }
  }
}
