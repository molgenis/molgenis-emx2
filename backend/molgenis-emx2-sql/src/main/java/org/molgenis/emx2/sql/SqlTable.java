package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.sql.SqlTypeUtils.getTypedValue;

import java.io.StringReader;
import java.io.Writer;
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

  public int insert(Iterable<Row> rows) {
    return executeTransaction(rows, false);
  }

  @Override
  public int insert(Row... rows) {
    return insert(Arrays.asList(rows));
  }

  @Override
  public int update(Row... rows) {
    return update(Arrays.asList(rows));
  }

  @Override
  public int update(Iterable<Row> rows) {
    return this.executeTransaction(rows, true);
  }

  private String getMgTableClass(TableMetadata table) {
    return table.getSchemaName() + "." + table.getTableName();
  }

  private int executeTransaction(Iterable<Row> rows, boolean isUpdate) {
    long start = System.currentTimeMillis();
    final AtomicInteger count = new AtomicInteger(0);
    final Map<String, List<Row>> subclassRows = new LinkedHashMap<>();
    String tableClass = getMgTableClass(getMetadata());

    // validate
    if (getMetadata().getPrimaryKeys().isEmpty())
      throw new MolgenisException(
          "Transaction failed: Table "
              + getName()
              + " cannot process row insert/update/delete requests because no primary key is defined");

    try {
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

              // add to batch list, and execute if batch is large enough
              subclassRows.get(subclassName).add(row);

              if (subclassRows.get(subclassName).size() >= 1000) {
                // execute
                SqlTable table = (SqlTable) getSchema().getTable(subclassName.split("\\.")[1]);
                if (isUpdate) {
                  count.set(count.get() + table.updateBatch(subclassRows.get(subclassName)));
                } else {
                  count.set(count.get() + table.insertBatch(subclassRows.get(subclassName)));
                }
                // clear the list
                subclassRows.get(subclassName).clear();
              }
            }

            // execute any remaining batches
            for (Map.Entry<String, List<Row>> batch : subclassRows.entrySet()) {
              // execute
              String subclassName = batch.getKey().split("\\.")[1];
              SqlTable table = (SqlTable) getSchema().getTable(subclassName);
              if (batch.getValue().size() > 0) {
                if (isUpdate) {
                  count.set(count.get() + table.updateBatch(batch.getValue()));
                } else {
                  count.set(count.get() + table.insertBatch(batch.getValue()));
                }
              }
            }
          });
    } catch (Exception e) {
      if (isUpdate) {
        throw new SqlMolgenisException("Update into table '" + getName() + "' failed.", e);
      } else {
        throw new SqlMolgenisException("Insert into table '" + getName() + "' failed.", e);
      }
    }

    log(
        start,
        count,
        isUpdate
            ? "updated (incl subclass if applicable)"
            : "inserted (incl subclass if applicable)");
    return count.get();
  }

  private int insertBatch(List<Row> rows) {
    if (getMetadata().getInherit() != null) {
      getInheritedTable().insertBatch(rows);
    }

    // get metadata
    List<String> fieldNames = new ArrayList<>();
    List<Column> columns = new ArrayList<>();
    List<Field<?>> fields = new ArrayList<>();
    for (Column c : getMetadata().getMutationColumns()) {
      fieldNames.add(c.getName());
      columns.add(c);
      fields.add(c.getJooqField());
    }

    // define the insert step
    InsertValuesStepN<org.jooq.Record> step =
        db.getJooq().insertInto(getJooqTable(), fields.toArray(new Field[fields.size()]));

    // add all the rows as steps
    for (Row row : rows) {
      step.values(SqlTypeUtils.getValuesAsCollection(row, columns));
    }
    return step.execute();
  }

  private int updateBatch(Iterable<Row> rows) {
    if (getMetadata().getInherit() != null) {
      getInheritedTable().updateBatch(rows);
    }

    // execute in batches (batch by size or because columns set change)
    TableMetadata tableMetadata = getMetadata();
    List<Row> batch = new ArrayList<>();
    List<String> fieldNames = new ArrayList<>();
    List<Column> columns = new ArrayList<>();
    List<Field> fields = new ArrayList<>();

    AtomicInteger count = new AtomicInteger(0);
    List<Column> mutationColumns = tableMetadata.getMutationColumns();
    for (Row row : rows) {
      // to compare if columns change between rows
      Collection<String> rowFields = new ArrayList<>();
      for (Column c : mutationColumns) {
        if (c != null && row.containsName(c.getName())) {
          rowFields.add(c.getName());
        }
      }

      // execute when rowFields differ from previous FieldNames
      if (batch.size() > 0
          && !(fieldNames.containsAll(rowFields) && rowFields.containsAll(fieldNames))) {
        count.set(
            count.get()
                + updateBatch(
                    batch, getJooqTable(), fieldNames, columns, fields, getPrimaryKeyFields()));
        batch.clear();
        fieldNames.clear();
        fields.clear();
        columns.clear();
      }

      // add field metadata if first row of this batch
      if (fieldNames.isEmpty()) {
        for (Column c : mutationColumns) {
          if (rowFields.contains(c.getName())) {
            fields.add(c.getJooqField());
            columns.add(c);
            fieldNames.add(c.getName());
          }
        }
      }
      batch.add(row);
    }

    // execute the remaining batch, if any
    if (batch.size() > 0) {
      return count.get()
          + updateBatch(batch, getJooqTable(), fieldNames, columns, fields, getPrimaryKeyFields());
    } else {
      return count.get();
    }
  }

  private int updateBatch(
      Collection<Row> rows,
      org.jooq.Table<org.jooq.Record> t,
      List<String> fieldNames,
      List<Column> columns,
      List<Field> fields,
      List<Field> keyField) {
    if (!rows.isEmpty()) {
      long start = System.currentTimeMillis();

      // createColumn multi-value insert
      InsertValuesStepN<org.jooq.Record> step =
          db.getJooq().insertInto(t, fields.toArray(new Field[fields.size()]));

      for (Row row : rows) {
        // ignore total null rows
        Collection<Object> values = SqlTypeUtils.getValuesAsCollection(row, columns);
        boolean hasNotNull =
            values.stream().anyMatch(v -> v != null && !row.getString(MG_TABLECLASS).equals(v));
        if (hasNotNull) {
          step.values(values);
        } else {
          logger.debug("skipping empty row: {0}", row);
        }
      }

      // on duplicate key update using same record via "excluded" keyword in postgres
      InsertOnDuplicateSetStep<org.jooq.Record> step2 =
          step.onConflict(keyField.toArray(new Field<?>[keyField.size()])).doUpdate();
      for (String name : fieldNames) {
        step2.set(field(name(name)), (Object) field(unquotedName("excluded.\"" + name + "\"")));
      }
      int result = step.execute();
      this.log(start, new AtomicInteger(result), "updated");
      return result;
    }
    return 0;
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

  private List<Field> getPrimaryKeyFields() {
    return getMetadata().getPrimaryKeyFields();
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
