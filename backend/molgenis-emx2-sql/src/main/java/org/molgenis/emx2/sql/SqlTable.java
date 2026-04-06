package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.MutationType.*;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;
import static org.molgenis.emx2.sql.SqlTypeUtils.applyValidationAndComputed;
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
import org.molgenis.emx2.sql.autoid.IdGeneratorService;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlTable implements Table {
  private SqlDatabase db;
  private SqlTableMetadata metadata;
  private TableListener tableListener;
  private static Logger logger = LoggerFactory.getLogger(SqlTable.class);

  SqlTable(SqlDatabase db, SqlTableMetadata metadata, TableListener tableListener) {
    this.db = db;
    this.metadata = metadata;
    this.tableListener = tableListener;
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
                throw new SqlMolgenisException("copyOut failed: ", e);
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
                      Object value = getTypedValue(c, row);
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
                throw new SqlMolgenisException("copyOut failed: ", e);
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
      return executeTransaction(db, getSchema().getName(), getName(), rows, INSERT);
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
      return this.executeTransaction(db, getSchema().getName(), getName(), rows, UPDATE);
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
      return this.executeTransaction(db, getSchema().getName(), getName(), rows, SAVE);
    } catch (Exception e) {
      throw new SqlMolgenisException("Upsert into table '" + getName() + "' failed", e);
    }
  }

  @Override
  public void truncate() {
    db.tx(
        database -> {
          truncateTransaction((SqlDatabase) database, getSchema().getName(), getName());
        });
  }

  // use static to ensure we don't touch 'this' until transaction completed
  private static void truncateTransaction(
      SqlDatabase database, String schemaName, String tableName) {
    SqlTable t = database.getSchema(schemaName).getTable(tableName);
    if (t.getMetadata().getColumn(MG_TABLECLASS) != null) {
      SqlTable rootTable = (SqlTable) t.getMetadata().getRootTable().getTable();
      String mg_table = t.getMgTableClass(t.getMetadata());
      // cascading delete will take care of subclass deletes
      database
          .getJooqWithExtendedTimeout()
          .deleteFrom(rootTable.getJooqTable())
          .where(field(MG_TABLECLASS).equal(mg_table))
          .execute();
    }
    // else in normal table simply call delete
    else {
      // truncate would be faster, but then we need add code to remove and re-add foreign keys
      database.getJooqWithExtendedTimeout().deleteFrom(t.getJooqTable()).execute();
    }
    logger.info(database.getActiveUser() + " truncated table " + tableName);
  }

  private static String getMgTableClass(TableMetadata table) {
    return table.getSchemaName() + "." + table.getTableName();
  }

  private static int executeTransaction(
      Database db,
      String schemaName,
      String tableName,
      Iterable<Row> rows,
      MutationType transactionType) {
    long start = System.currentTimeMillis();
    final AtomicInteger count = new AtomicInteger(0);
    final Map<String, List<Row>> subclassRows = new LinkedHashMap<>();
    final Map<String, Set<String>> columnsProvided = new LinkedHashMap<>();

    SqlSchema schema = (SqlSchema) db.getSchema(schemaName);
    SqlTable table = schema.getTable(tableName);
    Column profileColumn = table.getMetadata().getProfileColumn();
    String discriminatorColumn = profileColumn != null ? profileColumn.getName() : MG_TABLECLASS;
    MutationType batchType = profileColumn != null ? SAVE : transactionType;

    // validate
    if (table.getMetadata().getPrimaryKeys().isEmpty())
      throw new MolgenisException(
          "Transaction failed: Table "
              + table.getName()
              + " cannot process row insert/update/delete requests because no primary key is defined");

    db.tx(
        db2 -> {
          SqlSchema txSchema = (SqlSchema) db2.getSchema(schemaName);
          for (Row row : rows) {
            String[] targets = new String[] {tableName};
            if (row.notNull(discriminatorColumn)) {
              if (profileColumn != null
                  && profileColumn.getColumnType() == ColumnType.VARIANT_ARRAY) {
                String[] arr = row.getStringArray(discriminatorColumn);
                if (arr != null) targets = arr;
              } else {
                targets = new String[] {row.getString(discriminatorColumn)};
              }
            }

            if (profileColumn != null && transactionType != MutationType.INSERT) {
              Set<String> keepSet = new HashSet<>();
              keepSet.add(tableName);
              for (String target : targets) {
                String unqualifiedTarget = target.contains(".") ? target.split("\\.")[1] : target;
                keepSet.add(unqualifiedTarget);
                TableMetadata targetMeta = schema.getMetadata().getTableMetadata(unqualifiedTarget);
                if (targetMeta != null) {
                  for (TableMetadata ancestor : targetMeta.getAllInheritedTables()) {
                    keepSet.add(ancestor.getTableName());
                  }
                }
              }
              List<Row> singleRow = List.of(row);
              for (TableMetadata child : table.getMetadata().getSubclassTables()) {
                if (!keepSet.contains(child.getTableName())) {
                  executeDeleteBatch(txSchema, child, singleRow);
                }
              }
            }

            for (String target : targets) {
              String subclassName =
                  resolveSubclassName(db, schema, schemaName, discriminatorColumn, target, count);

              if (profileColumn != null && row.notNull(profileColumn.getName())) {
                validateProfileValue(table, target, table.getMetadata().getSubclassTables());
              } else if (profileColumn == null) {
                row.setString(MG_TABLECLASS, subclassName);
              }

              if (!subclassRows.containsKey(subclassName)) {
                subclassRows.put(subclassName, new ArrayList<>());
              }
              if (columnsProvided.get(subclassName) == null) {
                columnsProvided.put(subclassName, new LinkedHashSet<>(row.getColumnNames()));
              }
              if (columnsProvidedAreDifferent(columnsProvided.get(subclassName), row)
                  || subclassRows.get(subclassName).size() >= 100) {
                executeBatch(
                    (SqlSchema) db2.getSchema(subclassName.split("\\.")[0]),
                    batchType,
                    count,
                    subclassRows,
                    subclassName,
                    columnsProvided.get(subclassName));
                columnsProvided.get(subclassName).clear();
                columnsProvided.get(subclassName).addAll(row.getColumnNames());
              }
              subclassRows.get(subclassName).add(row);
            }
          }

          // execute any remaining insert/upsert batches
          for (Map.Entry<String, List<Row>> batch : subclassRows.entrySet()) {
            if (!batch.getValue().isEmpty()) {
              executeBatch(
                  (SqlSchema) db2.getSchema(batch.getKey().split("\\.")[0]),
                  batchType,
                  count,
                  subclassRows,
                  batch.getKey(),
                  columnsProvided.get(batch.getKey()));
            }
          }
          // listeners
          if (table.getTableListener() != null) {
            table.getTableListener().preparePostSave(rows);
          }
        });

    log(
        db.getActiveUser(),
        table.getJooqTable().getName(),
        start,
        count,
        transactionType.name().toLowerCase() + "d (incl subclass if applicable)");
    return count.get();
  }

  private static String resolveSubclassName(
      Database db,
      SqlSchema schema,
      String schemaName,
      String discriminatorColumn,
      String target,
      AtomicInteger count) {
    if (!target.contains(".")) {
      if (schema.getTable(target) != null) {
        return schemaName + "." + target;
      } else {
        throw new MolgenisException(
            discriminatorColumn
                + " value failed in row "
                + count.get()
                + ": found '"
                + target
                + "'");
      }
    } else {
      String rowSchemaName = target.split("\\.")[0];
      String rowTableName = target.split("\\.")[1];
      if (db.getSchema(rowSchemaName) == null
          || db.getSchema(rowSchemaName).getTable(rowTableName) == null) {
        throw new MolgenisException(
            "invalid value in column '"
                + discriminatorColumn
                + "' on row "
                + count.get()
                + ": found '"
                + target
                + "'");
      }
      return target;
    }
  }

  private static void validateProfileValue(
      SqlTable parentTable, String profileValue, List<TableMetadata> allChildTables) {
    TableMetadata subtable = parentTable.getMetadata().getSchema().getTableMetadata(profileValue);
    if (subtable == null || subtable.getTableType() == TableType.INTERNAL) {
      throw new MolgenisException(
          "Invalid profile value: '"
              + profileValue
              + "' is not a valid subtable of '"
              + parentTable.getName()
              + "'"
              + (subtable != null
                  ? " ('" + profileValue + "' is a block, not a selectable profile)"
                  : ""));
    }
    boolean isChild =
        allChildTables.stream().anyMatch(c -> c.getTableName().equals(subtable.getTableName()));
    if (!isChild) {
      throw new MolgenisException(
          "Invalid profile value: '"
              + profileValue
              + "' is not a subtable of '"
              + parentTable.getName()
              + "'");
    }
  }

  private static void executeDeleteBatch(
      SqlSchema schema, TableMetadata childMeta, List<Row> rows) {
    if (rows.isEmpty()) return;
    DSLContext jooq = schema.getJooq();
    List<String> pkNames = childMeta.getPrimaryKeys();
    List<Condition> pkeyConditions = new ArrayList<>();
    for (Row row : rows) {
      List<Condition> pkFields = new ArrayList<>();
      for (String pk : pkNames) {
        Object pkValue = row.getValueMap().get(pk);
        pkFields.add(field(name(pk)).eq(pkValue != null ? inline(pkValue) : inline((Object) null)));
      }
      if (!pkFields.isEmpty()) {
        pkeyConditions.add(and(pkFields));
      }
    }
    if (!pkeyConditions.isEmpty()) {
      jooq.deleteFrom(childMeta.getJooqTable()).where(or(pkeyConditions)).execute();
    }
  }

  private static boolean columnsProvidedAreDifferent(Set<String> columnsProvided, Row row) {
    return !columnsProvided.isEmpty() && !columnsProvided.equals(row.getColumnNames());
  }

  private static void executeBatch(
      SqlSchema schema,
      MutationType transactionType,
      AtomicInteger count,
      Map<String, List<Row>> subclassRows,
      String subclassName,
      Set<String> columnsProvided) {

    // execute
    SqlTable table = schema.getTable(subclassName.split("\\.")[1]);
    if (UPDATE.equals(transactionType)) {
      List<Column> updateColumns = getUpdateColumns(table, columnsProvided);
      List<Row> rows =
          applyValidationAndComputed(
              table.getMetadata().getColumns(), subclassRows.get(subclassName));
      count.set(count.get() + table.updateBatch(table, rows, updateColumns));
    } else if (SAVE.equals(transactionType) || INSERT.equals(transactionType)) {
      List<Column> insertColumns = getInsertColumns(table, columnsProvided);
      List<Row> rows = applyValidationAndComputed(insertColumns, subclassRows.get(subclassName));
      count.set(
          count.get()
              + table.insertBatch(table, rows, SAVE.equals(transactionType), insertColumns));
    } else {
      throw new MolgenisException(
          "Internal error in executeBatch: transaction type "
              + transactionType
              + " not allowed here");
    }
    // clear the list
    subclassRows.get(subclassName).clear();
  }

  private static List<Column> getInsertColumns(SqlTable table, Set<String> columnsProvided) {
    return table.getMetadata().getColumnsWithoutHeadings().stream()
        .filter(
            c ->
                !c.isRefback()
                    || (c.isReference()
                        && c.getReferences().stream()
                            .anyMatch(r -> columnsProvided.contains(r.getName()))))
        .toList();
  }

  private static List<Column> getUpdateColumns(SqlTable table, Set<String> columnsProvided) {
    return getInsertColumns(table, columnsProvided).stream()
        .filter(c -> !c.isReadonly() && !c.isPrimaryKey())
        .filter(c -> !c.getName().equals(MG_INSERTEDBY) && !c.getName().equals(MG_INSERTEDON))
        .filter(
            c ->
                AUTO_ID.equals(c.getColumnType())
                    || c.getComputed() != null
                    || (c.isReference()
                        ? c.getReferences().stream()
                            .anyMatch(r -> columnsProvided.contains(r.getName()))
                        : columnsProvided.contains(c.getName())))
        .toList();
  }

  private TableListener getTableListener() {
    return this.tableListener;
  }

  private int insertBatch(
      SqlTable table, List<Row> rows, boolean updateOnConflict, List<Column> updateColumns) {
    boolean inherit = table.getMetadata().getInheritNames() != null;
    int count = 0;
    if (inherit) {
      for (SqlTable inheritedTable : table.getInheritedTables()) {
        // use upsert (updateOnConflict=true) for all parent inserts to handle diamond inheritance
        count = inheritedTable.insertBatch(inheritedTable, rows, true, updateColumns);
      }
    }

    List<Column> columns = getLocalStoredColumns(table, updateColumns);
    if (columns.size() == 0) return count;
    List<Field> insertFields =
        columns.stream().map(c -> c.getJooqField()).collect(Collectors.toList());
    InsertValuesStepN<org.jooq.Record> step =
        table.getJooq().insertInto(table.getJooqTable(), insertFields.toArray(new Field[0]));

    // add all the rows as steps
    LocalDateTime now = LocalDateTime.now();
    for (Row row : rows) {
      // get values
      Map values = getSelectedRowValues(columns, row);
      if (!inherit) {
        values.put(MG_INSERTEDBY, getActiveUser(table));
        values.put(MG_INSERTEDON, now);
        values.put(MG_UPDATEDBY, getActiveUser(table));
        values.put(MG_UPDATEDON, now);
      }
      step.values(values.values());
    }

    // optionally, add conflict clause
    if (updateOnConflict) {
      InsertOnDuplicateSetStep<org.jooq.Record> step2 =
          step.onConflict(table.getMetadata().getPrimaryKeyFields().toArray(new Field[0]))
              .doUpdate();
      // remove mg_table as part of update key
      for (Column column :
          columns.stream()
              .filter(
                  c -> c.getName().equals(MG_TABLECLASS) || !Boolean.TRUE.equals(c.isReadonly()))
              .toList()) {
        step2.set(
            column.getJooqField(),
            (Object) field(unquotedName("excluded.\"" + column.getName() + "\"")));
      }
      if (!inherit) {
        step2.set(field(name(MG_UPDATEDBY)), getActiveUser(table));
        step2.set(field(name(MG_UPDATEDON)), now);
      }
    }

    return step.execute();
  }

  private static String getActiveUser(SqlTable table) {
    String user = table.getSchema().getDatabase().getActiveUser();
    if (user == null) {
      user = ADMIN_USER;
    }
    return user;
  }

  private int updateBatch(SqlTable table, List<Row> rows, List<Column> updateColumns) {
    boolean inherit = table.getMetadata().getInheritNames() != null;
    int count = 0;
    if (inherit) {
      for (SqlTable inheritedTable : table.getInheritedTables()) {
        count = inheritedTable.updateBatch(inheritedTable, rows, updateColumns);
      }
    }

    List<Column> columns = getLocalStoredColumns(table, updateColumns);
    if (columns.size() == 0) return count;
    List<Column> pkeyFields = table.getMetadata().getPrimaryKeyColumns();

    // create batch of updates
    List<UpdateConditionStep> list = new ArrayList();
    LocalDateTime now = LocalDateTime.now();
    for (Row row : rows) {
      Map values = getSelectedRowValues(columns, row);
      if (!inherit) {
        values.put(MG_UPDATEDBY, getActiveUser(table));
        values.put(MG_UPDATEDON, now);
      }

      list.add(
          table
              .getJooq()
              .update(table.getJooqTable())
              .set(values)
              .where(table.getUpdateCondition(row, pkeyFields)));
    }

    return Arrays.stream(table.getJooq().batch(list).execute()).reduce(Integer::sum).getAsInt();
  }

  private static List<Column> getLocalStoredColumns(SqlTable table, List<Column> updateColumns) {
    List<String> updateColumnNames = updateColumns.stream().map(c -> c.getName()).toList();
    List<Column> storedColumns =
        table.getMetadata().getStoredColumns().stream()
            .filter(c -> updateColumnNames.contains(c.getName()))
            .toList();
    List<Column> expandedColumns = table.getMetadata().getExpandedColumns(storedColumns);
    return expandedColumns;
  }

  private Map<String, Object> getSelectedRowValues(List<Column> selection, Row row) {
    Map<String, Object> selectedValues = new LinkedHashMap<>();
    for (Column column : selection) {
      if (AUTO_ID.equals(column.getColumnType())
          && !metadata.getColumn(column.getName()).isReference()
          && row.isNull(column.getName(), column.getPrimitiveColumnType())) {
        selectedValues.put(column.getName(), new IdGeneratorService().generateIdForColumn(column));
      } else {
        selectedValues.put(column.getName(), getTypedValue(column, row));
      }
    }

    return selectedValues;
  }

  private Condition getUpdateCondition(Row row, List<Column> pkeyFields) {
    List<Condition> result = new ArrayList<>();
    for (Column key : pkeyFields) {
      if (key.isReference()) {
        for (Reference ref : key.getReferences()) {
          result.add(ref.getJooqField().eq(row.get(ref.getName(), ref.getPrimitiveType())));
        }
      } else {
        result.add(key.getJooqField().eq(row.get(key)));
      }
    }
    return and(result);
  }

  @Override
  public int delete(Iterable<Row> rows) {
    long start = System.currentTimeMillis();

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.tx(
          db2 -> {
            SqlTable table = (SqlTable) db2.getSchema(getSchema().getName()).getTable(getName());

            // delete in batches
            int batchSize = 1000;
            List<Row> batch = new ArrayList<>();
            for (Row row : rows) {
              batch.add(row);
              count.set(count.get() + 1);
              if (count.get() % batchSize == 0) {
                deleteBatch(table, batch);
                batch.clear();
              }
            }

            // delete remaining elements
            deleteBatch(table, batch);

            // finally delete in superclass tables
            if (table.getMetadata().getInheritNames() != null) {
              for (SqlTable inheritedTable : table.getInheritedTables()) {
                inheritedTable.delete(rows);
              }
            }

            // notify handlers
            if (table.getTableListener() != null) {
              table.getTableListener().preparePostDelete(rows);
            }
          });
    } catch (Exception e) {
      throw new SqlMolgenisException("Delete into table " + getName() + " failed", e);
    }

    log(db.getActiveUser(), getName(), start, count, "deleted");

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

  @Override
  public Query groupBy(SelectColumn columns) {
    return groupBy().select(columns);
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

  private static void deleteBatch(SqlTable table, Collection<Row> rows) {
    if (!rows.isEmpty()) {
      List<String> keyNames =
          table.getMetadata().getPrimaryKeyFields().stream()
              .map(Field::getName)
              .collect(Collectors.toList());

      // in case no primary key is defined, use all columns
      if (keyNames == null) {
        throw new MolgenisException(
            "Delete on table " + table.getName() + " failed: no primary key set");
      }
      Condition whereCondition = table.getWhereConditionForBatchDelete(rows);
      table.getJooq().deleteFrom(table.getJooqTable()).where(whereCondition).execute();
    }
  }

  private DSLContext getJooq() {
    return ((SqlDatabase) getSchema().getDatabase()).getJooq();
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
    if (key.isRef() || key.isRefArray()) {
      for (Reference ref : key.getReferences()) {
        if (!ref.isOverlapping()) {
          columnCondition.add(
              ref.getJooqField()
                  .eq(cast(r.get(ref.getName(), ref.getPrimitiveType()), ref.getJooqField())));
        }
      }
    } else if (key.isRefback()) {
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
  public Query groupBy() {
    return new SqlQuery(
        (SqlSchemaMetadata) this.getMetadata().getSchema(), this.getName() + "_groupBy");
  }

  @Override
  public List<Row> retrieveRows(Query.Option... options) {
    return this.query().retrieveRows(options);
  }

  @Override
  public String getName() {
    return getMetadata().getTableName();
  }

  @Override
  public String getIdentifier() {
    return getMetadata().getIdentifier();
  }

  protected org.jooq.Table<org.jooq.Record> getJooqTable() {
    return table(name(metadata.getSchema().getName(), metadata.getTableName()));
  }

  @Override
  public SqlTable getInheritedTable() {
    if (getMetadata().getInheritNames() == null || getMetadata().getInheritNames().length == 0) {
      return null;
    }
    if (getMetadata().getImportSchema() != null) {
      return (SqlTable)
          getSchema()
              .getDatabase()
              .getSchema(getMetadata().getImportSchema())
              .getTable(getMetadata().getInheritNames()[0]);
    } else {
      return (SqlTable) getSchema().getTable(getMetadata().getInheritNames()[0]);
    }
  }

  // Casts TableMetadata results to SqlTable so insertBatch can recursively insert into parent
  // tables.
  public List<SqlTable> getInheritedTables() {
    List<SqlTable> result = new ArrayList<>();
    if (getMetadata().getInheritNames() != null) {
      for (String name : getMetadata().getInheritNames()) {
        if (getMetadata().getImportSchema() != null) {
          result.add(
              (SqlTable)
                  getSchema()
                      .getDatabase()
                      .getSchema(getMetadata().getImportSchema())
                      .getTable(name));
        } else {
          result.add((SqlTable) getSchema().getTable(name));
        }
      }
    }
    return result;
  }

  private static void log(
      String user, String table, long start, AtomicInteger count, String message) {
    if (user == null) user = "molgenis";
    if (logger.isInfoEnabled()) {
      logger.info(
          "{} {} {} rows into table {} in {}ms",
          user,
          message,
          count.get(),
          table,
          (System.currentTimeMillis() - start));
    }
  }
}
