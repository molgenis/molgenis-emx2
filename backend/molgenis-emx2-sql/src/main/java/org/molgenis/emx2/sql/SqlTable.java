package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.Record;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.*;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.sql.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.sql.SqlTypeUtils.getTypedValue;

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
                StringBuffer tmp = new StringBuffer();
                tmp.append(
                    this.getMetadata().getLocalColumnNames().stream()
                            .map(c -> "\"" + c + "\"")
                            .collect(Collectors.joining(","))
                        + "\n");
                for (Row row : rows) {
                  StringBuffer line = new StringBuffer();
                  for (Column c : this.getMetadata().getLocalColumns()) {
                    if (!row.containsName(c.getName())) {
                      line.append(",");
                    } else {
                      Object value = getTypedValue(row, c);
                      line.append(value + ",");
                    }
                  }
                  tmp.append(line.toString().substring(0, line.length() - 1) + "\n");
                }

                String tableName =
                    "\"" + getSchema().getMetadata().getName() + "\".\"" + getName() + "\"";
                // System.out.println(tmp.toString());

                String columnNames =
                    "("
                        + this.getMetadata().getLocalColumnNames().stream()
                            .map(c -> "\"" + c + "\"")
                            .collect(Collectors.joining(","))
                        + ")";
                String sql = "COPY " + tableName + columnNames + " FROM STDIN (FORMAT CSV,HEADER )";
                System.out.println(sql);
                cm.copyIn(sql, new StringReader(tmp.toString()));
              } catch (Exception e) {
                throw new MolgenisException("copyOut failed: ", e);
              }
            });
  }

  public int insert(Iterable<Row> rows) {
    return this.insert(rows, getMetadata().getTableName());
  }

  private int insert(Iterable<Row> rows, String mgTableClass) {
    long start = System.currentTimeMillis();

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.tx(
          db2 -> {

            // first update superclass
            if (getMetadata().getInherit() != null) {
              ((SqlTable) getSchema().getTable(getMetadata().getInherit()))
                  .insert(rows, getMetadata().getTableName());
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

            // keep batchsize smaller to limit memory footprint
            int batchSize = 1000;
            InsertValuesStepN<Record> step =
                db.getJooq().insertInto(getJooqTable(), fields.toArray(new Field[fields.size()]));
            int i = 0;
            for (Row row : rows) {
              row.set(MG_TABLECLASS, mgTableClass);

              step.values(SqlTypeUtils.getValuesAsCollection(row, columns));
              // step.values(row.getValueMap());
              i++;
              // execute batch
              if (i % batchSize == 0) {
                count.set(count.get() + step.execute());
                step =
                    db.getJooq()
                        .insertInto(getJooqTable(), fields.toArray(new Field[fields.size()]));
              }
            }
            // execute remaining
            if (i % batchSize != 0) {
              count.set(count.get() + step.execute());
            }
          });
    } catch (Exception e) {
      throw new SqlMolgenisException("Insert into table '" + getName() + "' failed.", e);
    }

    log(start, count, "inserted");

    return count.get();
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
    return this.update(rows, getMetadata().getTableName());
  }

  private int update(Iterable<Row> rows, String mgTableClass) {
    long start = System.currentTimeMillis();

    if (getMetadata().getPrimaryKeys() == null)
      throw new MolgenisException(
          "Update failed: Table "
              + getName()
              + " cannot process row update requests because no primary key is defined");

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.tx(
          db2 -> { // first update superclass
            if (getMetadata().getInherit() != null) {
              try {
                ((SqlTable) getSchema().getTable(getMetadata().getInherit()))
                    .update(rows, mgTableClass);
              } catch (MolgenisException e) {
                throw new MolgenisException(
                    "Update of table '" + getName() + "' failed", e.getMessage());
              }
            }

            // keep batchsize smaller to limit memory footprint
            int batchSize = 10000;

            // execute in batches (batch by size or because columns set change)
            TableMetadata tableMetadata = getMetadata();
            List<Row> batch = new ArrayList<>();
            List<String> fieldNames = new ArrayList<>();
            List<Column> columns = new ArrayList<>();
            List<Field<?>> fields = new ArrayList<>();
            for (Row row : rows) {
              row.set(MG_TABLECLASS, mgTableClass);

              // to compare if columns change between rows
              Collection<String> rowFields = new ArrayList<>();
              for (Column c : tableMetadata.getMutationColumns()) {
                if (c != null && row.containsName(c.getName())) {
                  rowFields.add(c.getName());
                }
              }

              // execute the batch if batchSize is reached
              // or when rowFields differ from previous FieldNames
              if (!batch.isEmpty()
                  && (count.get() % batchSize == 0
                      || !(fieldNames.containsAll(rowFields)
                          && rowFields.containsAll(fieldNames)))) {
                updateBatch(
                    batch,
                    getJooqTable(),
                    fieldNames,
                    columns,
                    fields,
                    getPrimaryKeyFields(),
                    mgTableClass);
                batch.clear();
                fieldNames.clear();
                fields.clear();
                columns.clear();
              }

              // add field metadata if first row of this batch
              if (fieldNames.isEmpty()) {
                for (Column c : tableMetadata.getMutationColumns()) {
                  if (rowFields.contains(c.getName())) {
                    fields.add(c.getJooqField());
                    columns.add(c);
                    fieldNames.add(c.getName());
                  }
                }
              }

              // else simply keep on adding rows to the batch
              batch.add(row);
              count.set(count.get() + 1);
            }

            // execute the remaining batch, if any
            updateBatch(
                batch,
                getJooqTable(),
                fieldNames,
                columns,
                fields,
                getPrimaryKeyFields(),
                mgTableClass);
          });
    } catch (Exception e) {
      throw new SqlMolgenisException("Update into table '" + getName() + "' failed.", e);
    }

    return count.get();
  }

  private void updateBatch(
      Collection<Row> rows,
      org.jooq.Table<Record> t,
      List<String> fieldNames,
      List<Column> columns,
      List<Field<?>> fields,
      List<Field<?>> keyField,
      String mgTableClass) {
    long start = System.currentTimeMillis();

    if (!rows.isEmpty()) {

      // createColumn multi-value insert
      InsertValuesStepN<Record> step =
          db.getJooq().insertInto(t, fields.toArray(new Field[fields.size()]));

      for (Row row : rows) {
        // ignore total null rows
        Collection<Object> values = SqlTypeUtils.getValuesAsCollection(row, columns);
        boolean hasNotNull = values.stream().anyMatch(v -> v != null && !mgTableClass.equals(v));
        if (hasNotNull) {
          step.values(values);
        } else {
          logger.debug("skipping empty row: " + row);
        }
      }

      // on duplicate key update using same record via "excluded" keyword in postgres
      InsertOnDuplicateSetStep<Record> step2 = step.onConflict(keyField).doUpdate();
      for (String name : fieldNames) {
        step2 =
            step2.set(field(name(name)), (Object) field(unquotedName("excluded.\"" + name + "\"")));
      }
      step.execute();
      log(start, new AtomicInteger(rows.size()), "updated ");
    }
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
              getSchema().getTable(getMetadata().getInherit()).delete(rows);
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
              .map(f -> f.getName())
              .collect(Collectors.toList());

      // in case no primary key is defined, use all columns
      if (keyNames == null) {
        throw new MolgenisException(
            "Delete on table " + getName() + " failed: no primary key set", "");
      }
      Condition whereCondition = getWhereConditionForBatchDelete(rows);
      db.getJooq().deleteFrom(getJooqTable()).where(whereCondition).execute();
    }
  }

  private Condition getWhereConditionForBatchDelete(Collection<Row> rows) {
    List<Condition> conditions = new ArrayList<>();
    for (Row r : rows) {
      List<Condition> rowCondition = new ArrayList<>();
      if (getMetadata().getPrimaryKeys() == null) {
        // when no key, use all columns as id
        for (Column keyPart : getMetadata().getLocalColumns()) {
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
    if (REF.equals(key.getColumnType())
        || REF_ARRAY.equals(key.getColumnType())
        || MREF.equals(key.getColumnType())) {
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

  private List<Field<?>> getPrimaryKeyFields() {
    return getMetadata().getPrimaryKeyFields();
  }

  protected org.jooq.Table<Record> getJooqTable() {
    return table(name(metadata.getSchema().getName(), metadata.getTableName()));
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
