package org.molgenis.emx2.sql;

import org.jooq.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;

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

  public int insert(Iterable<Row> rows) {
    long start = System.currentTimeMillis();

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.tx(
          db2 -> {

            // first update superclass
            if (getMetadata().getInherit() != null) {
              getSchema().getTable(getMetadata().getInherit()).insert(rows);
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
              step.values(SqlTypeUtils.getValuesAsCollection(row, columns));
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
    long start = System.currentTimeMillis();

    if (getMetadata().getPrimaryKeys() == null)
      throw new MolgenisException(
          "Update failed",
          "Table "
              + getName()
              + " cannot process row update requests because no primary key is defined");

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.tx(
          db2 -> { // first update superclass
            if (getMetadata().getInherit() != null) {
              getSchema().getTable(getMetadata().getInherit()).update(rows);
            }

            // keep batchsize smaller to limit memory footprint
            int batchSize = 1000;

            // execute in batches (batch by size or because columns set change)
            TableMetadata tableMetadata = getMetadata();
            List<Row> batch = new ArrayList<>();
            List<String> fieldNames = new ArrayList<>();
            List<Column> columns = new ArrayList<>();
            List<Field<?>> fields = new ArrayList<>();
            for (Row row : rows) {

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
                    batch, getJooqTable(), fieldNames, columns, fields, getPrimaryKeyFields());
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
            updateBatch(batch, getJooqTable(), fieldNames, columns, fields, getPrimaryKeyFields());
          });
    } catch (Exception e) {
      throw new SqlMolgenisException("Update into table '" + getName() + "' failed.", e);
    }

    log(start, count, "updated");

    return count.get();
  }

  private void updateBatch(
      Collection<Row> rows,
      org.jooq.Table<Record> t,
      List<String> fieldNames,
      List<Column> columns,
      List<Field<?>> fields,
      List<Field<?>> keyField) {

    if (!rows.isEmpty()) {

      // createColumn multi-value insert
      InsertValuesStepN<Record> step =
          db.getJooq().insertInto(t, fields.toArray(new Field[fields.size()]));

      for (Row row : rows) {
        step.values(SqlTypeUtils.getValuesAsCollection(row, columns));
      }

      // on duplicate key update using same record via "excluded" keyword in postgres
      InsertOnDuplicateSetStep<Record> step2 = step.onConflict(keyField).doUpdate();
      for (String name : fieldNames) {
        step2 =
            step2.set(field(name(name)), (Object) field(unquotedName("excluded.\"" + name + "\"")));
      }
      step.execute();
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
      List<String> keyNames = getMetadata().getPrimaryKeys();

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
        columnCondition.add(
            ref.getJooqField()
                .eq(cast(r.get(key.getName(), ref.getColumnType()), ref.getJooqField())));
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
  public List<Row> getRows() {
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
