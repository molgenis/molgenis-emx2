package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.MutationType.*;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;
import static org.molgenis.emx2.sql.SqlTypeUtils.getTypedValue;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.jooq.*;
import org.jooq.Record;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.sql.autoid.IdGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlTable implements Table {

  private final SqlDatabase db;
  private final SqlTableMetadata metadata;
  private final TableListener tableListener;
  private static final Logger logger = LoggerFactory.getLogger(SqlTable.class);
  private static final int DELETE_BATCH_SIZE = 1000;

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

  @Override
  public int insert(Row... rows) {
    return insert(Arrays.asList(rows));
  }

  @Override
  public int insert(Iterable<Row> rows) {
    rowOwnership().validateAndAssignOwnerWhenOmitted(rows);
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
    rowOwnership().validateOwners(rows); // an update keeps the owner the row already has
    try {
      return executeTransaction(db, getSchema().getName(), getName(), rows, UPDATE);
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
    rowOwnership().validateAndAssignOwnerWhenOmitted(rows);
    try {
      return executeTransaction(db, getSchema().getName(), getName(), rows, SAVE);
    } catch (Exception e) {
      throw new SqlMolgenisException("Upsert into table '" + getName() + "' failed", e);
    }
  }

  private RowOwnership rowOwnership() {
    return new RowOwnership(getSchema(), metadata);
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
    final SqlTable t = database.getSchema(schemaName).getTable(tableName);
    if (t.getMetadata().getColumn(MG_TABLECLASS) != null) {
      final SqlTable rootTable = (SqlTable) t.getMetadata().getRootTable().getTable();
      String mgTable = getMgTableClass(t.getMetadata());
      // cascading delete will take care of subclass deletes
      database
          .getJooqWithExtendedTimeout()
          .deleteFrom(rootTable.getJooqTable())
          .where(field(MG_TABLECLASS).equal(mgTable))
          .execute();
    }
    // else in a normal table simply call delete
    else {
      // truncate would be faster, but then we need add code to remove and re-add foreign keys
      database.getJooqWithExtendedTimeout().deleteFrom(t.getJooqTable()).execute();
    }
    String activeUser = database.getActiveUser();
    String msg = MessageFormat.format("{0} truncated table {1}", activeUser, tableName);
    logger.info(msg);
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

    SqlSchema schema = (SqlSchema) db.getSchema(schemaName);
    SqlTable table = schema.getTable(tableName);
    String tableClass = getMgTableClass(table.getMetadata());
    requirePrimaryKey(table);

    db.tx(
        db2 -> {
          SubclassBatcher batcher = new SubclassBatcher(db2, transactionType, count);
          for (Row row : rows) {
            row.setString(
                MG_TABLECLASS,
                resolveTableClass(db, schema, schemaName, tableClass, row, count.get()));
            batcher.add(row);
          }
          batcher.flushAll();

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

  private static void requirePrimaryKey(SqlTable table) {
    if (table.getMetadata().getPrimaryKeys().isEmpty()) {
      throw new MolgenisException(
          "Transaction failed: Table "
              + table.getName()
              + " cannot process row insert/update/delete requests because no primary key is defined");
    }
  }

  /** schema part of a '<schema>.<table>' mg_tableclass value */
  private static String schemaOf(String qualifiedTableName) {
    return qualifiedTableName.split("\\.")[0];
  }

  /**
   * Resolves the fully qualified mg_tableclass of a row: the table's own class when the row does
   * not specify one, otherwise the value provided by the row, qualified with the schema name if
   * needed. Throws if the row points at a table that does not exist.
   */
  private static String resolveTableClass(
      Database db,
      SqlSchema schema,
      String schemaName,
      String defaultTableClass,
      Row row,
      int rowNr) {
    if (!row.notNull(MG_TABLECLASS)) {
      return defaultTableClass;
    }
    String rowTableName = row.getString(MG_TABLECLASS);
    if (!rowTableName.contains(".")) {
      if (schema.getTable(rowTableName) == null) {
        throw new MolgenisException(
            MG_TABLECLASS + " value failed in row " + rowNr + ": found '" + rowTableName + "'");
      }
      return schemaName + "." + rowTableName;
    }
    String[] parts = rowTableName.split("\\.", 2);
    org.molgenis.emx2.Schema rowSchema = db.getSchema(parts[0]);
    if (rowSchema == null || rowSchema.getTable(parts[1]) == null) {
      throw new MolgenisException(
          "invalid value in column '"
              + MG_TABLECLASS
              + "' on row "
              + rowNr
              + ": found '"
              + rowTableName
              + "'");
    }
    return rowTableName;
  }

  /**
   * Collects rows per (sub)class table and flushes them in batches, also whenever the set of
   * columns provided changes because a batch can only be executed for one set of columns.
   */
  private static class SubclassBatcher {
    private static final int BATCH_SIZE = 100;

    private final Database db;
    private final MutationType transactionType;
    private final AtomicInteger count;
    private final Map<String, List<Row>> subclassRows = new LinkedHashMap<>();
    private final Map<String, Set<String>> columnsProvided = new LinkedHashMap<>();

    SubclassBatcher(Database db, MutationType transactionType, AtomicInteger count) {
      this.db = db;
      this.transactionType = transactionType;
      this.count = count;
    }

    void add(Row row) {
      String subclassName = row.getString(MG_TABLECLASS);
      List<Row> batch = subclassRows.computeIfAbsent(subclassName, name -> new ArrayList<>());
      Set<String> columns =
          columnsProvided.computeIfAbsent(
              subclassName, name -> new LinkedHashSet<>(row.getColumnNames()));

      if (columnsProvidedAreDifferent(columns, row) || batch.size() >= BATCH_SIZE) {
        flush(subclassName);
        // reset columns provided
        columns.clear();
        columns.addAll(row.getColumnNames());
      }
      batch.add(row);
    }

    /** execute any remaining batches */
    void flushAll() {
      for (Map.Entry<String, List<Row>> batch : subclassRows.entrySet()) {
        if (!batch.getValue().isEmpty()) {
          flush(batch.getKey());
        }
      }
    }

    private void flush(String subclassName) {
      executeBatch(
          (SqlSchema) db.getSchema(schemaOf(subclassName)),
          transactionType,
          count,
          subclassRows,
          subclassName,
          columnsProvided.get(subclassName));
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
      SqlRowProcessor rowProcessor = new SqlRowProcessor(table.getMetadata().getColumns());
      List<Row> rows = subclassRows.get(subclassName);
      List<Column> primaryKeyColumns =
          Collections.unmodifiableList(table.getMetadata().getPrimaryKeyColumns());

      // Retrieve the current values for the rows to update
      List<Row> rowsToUpdate = table.getRowsByRowKey(rows);
      // Pair the rows to update with the corresponding rows from the database
      List<UpdatePair> updatePairs = pair(rows, rowsToUpdate, primaryKeyColumns);
      // Update the rows from the db with the update values
      List<Row> updatePreview = merge(updatePairs, primaryKeyColumns);
      // Validate
      rowProcessor.validateAndCompute(updatePreview);

      count.set(count.get() + table.updateBatch(table, updatePreview, updateColumns));

    } else if (SAVE.equals(transactionType) || INSERT.equals(transactionType)) {
      List<Column> insertColumns = getInsertColumns(table, columnsProvided);
      List<Row> rows = subclassRows.get(subclassName);
      SqlRowProcessor rowProcessor = new SqlRowProcessor(insertColumns);
      rowProcessor.validateAndCompute(rows);
      count.set(
          count.get()
              + table.insertBatch(table, rows, SAVE.equals(transactionType), insertColumns).size());
    } else {
      throw new MolgenisException(
          "Internal error in executeBatch: transaction type "
              + transactionType
              + " not allowed here");
    }
    // clear the list
    subclassRows.get(subclassName).clear();
  }

  /**
   * Merges each pair into one row: values provided in the request override the values currently in
   * the database, except for the key columns that identify the row. Columns the request doesn't
   * mention keep their current value, so validation and computation see the complete row.
   */
  static List<Row> merge(List<UpdatePair> updatePairs, List<Column> primaryKeyColumns) {
    Set<String> keyColumnNames = getKeyColumnNames(primaryKeyColumns);
    List<Row> result = new ArrayList<>();
    for (UpdatePair updatePair : updatePairs) {
      result.add(
          updatePair.existing() == null
              ? updatePair.row()
              : updatePair.existing().overrideWith(updatePair.row(), keyColumnNames));
    }
    return result;
  }

  /** key columns by their stored names, so composite refs are covered per reference column */
  private static Set<String> getKeyColumnNames(List<Column> keyColumns) {
    Set<String> names = new LinkedHashSet<>();
    for (Column key : keyColumns) {
      if (key.isReference()) {
        key.getReferences().forEach(ref -> names.add(ref.getColumnName()));
      } else {
        names.add(key.getName());
      }
    }
    return names;
  }

  /** a row as provided in the request, together with its current state in the database (if any) */
  record UpdatePair(Row row, Row existing) {}

  /**
   * Pairs each row with the row that currently exists in the database, matched on the values of the
   * given key columns. Order and size of the result follow 'rows'; 'existing' is null when no row
   * with that key was found.
   */
  static List<UpdatePair> pair(
      List<Row> rows, List<Row> rowsToUpdate, List<Column> primaryKeyColumns) {
    if (primaryKeyColumns.isEmpty()) {
      throw new MolgenisException("Cannot pair rows: no key columns provided");
    }
    Map<List<Object>, Row> existingByKey = new LinkedHashMap<>();
    for (Row existing : rowsToUpdate) {
      existingByKey.put(getKeyValues(existing, primaryKeyColumns), existing);
    }
    List<UpdatePair> result = new ArrayList<>();
    for (Row row : rows) {
      result.add(new UpdatePair(row, existingByKey.get(getKeyValues(row, primaryKeyColumns))));
    }
    return result;
  }

  /**
   * Key of a row as a list of typed values, decomposing references into their underlying columns so
   * rows coming from the database compare equal to rows coming from the request.
   */
  private static List<Object> getKeyValues(Row row, List<Column> keyColumns) {
    List<Object> keyValues = new ArrayList<>();
    for (Column key : keyColumns) {
      if (key.isReference()) {
        for (Reference ref : key.getReferences()) {
          keyValues.add(normalizeKeyValue(row.get(ref.getColumnName(), ref.getPrimitiveType())));
        }
      } else {
        keyValues.add(normalizeKeyValue(row.get(key.getName(), key.getPrimitiveColumnType())));
      }
    }
    return keyValues;
  }

  /** arrays don't implement equals/hashCode by value, so use list equality instead */
  private static Object normalizeKeyValue(Object value) {
    if (value instanceof Object[] array) {
      return Arrays.asList(array);
    }
    return value;
  }

  private static List<Column> getInsertColumns(SqlTable table, Set<String> columnsProvided) {
    return table.getMetadata().getColumnsWithoutHeadings().stream()
        .filter(
            c ->
                !c.isRefback()
                    || (c.isReference()
                        && c.getReferences().stream()
                            .anyMatch(r -> columnsProvided.contains(r.getColumnName()))))
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
                            .anyMatch(r -> columnsProvided.contains(r.getColumnName()))
                        : columnsProvided.contains(c.getName())))
        .toList();
  }

  private TableListener getTableListener() {
    return this.tableListener;
  }

  private List<Record> insertBatch(
      SqlTable table, List<Row> rows, boolean updateOnConflict, List<Column> updateColumns) {
    boolean inherit = table.getMetadata().getInheritName() != null;
    if (inherit) {
      SqlTable inheritedTable = table.getInheritedTable();
      List<Record> records =
          inheritedTable.insertBatch(inheritedTable, rows, updateOnConflict, updateColumns);

      List<Column> autoIdColumns =
          inheritedTable.getMetadata().getPrimaryKeyColumns().stream()
              .filter(c -> AUTO_ID.equals(c.getColumnType()))
              .toList();

      // Copy the generated auto id's from the parent table
      for (int i = 0; i < records.size(); i++) {
        copyRecordValuesIntoRows(rows.get(i), records.get(i), autoIdColumns);
      }
    }

    List<Column> columns = getLocalStoredColumns(table, updateColumns);
    if (columns.isEmpty()) {
      return Collections.emptyList();
    }

    List<Field> insertFields = columns.stream().map(Column::getJooqField).toList();
    InsertValuesStepN<org.jooq.Record> step =
        table.getJooq().insertInto(table.getJooqTable(), insertFields.toArray(new Field[0]));

    // add all the rows as steps
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    for (Row row : rows) {
      Map<String, Object> values = getSelectedRowValues(columns, row);
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

    return step.returningResult(table.getMetadata().getPrimaryKeyFields()).fetch();
  }

  private static void copyRecordValuesIntoRows(Row row, Record from, List<Column> toCopy) {
    for (Column column : toCopy) {
      row.set(column.getName(), from.getValue(column.getName()));
    }
  }

  private static String getActiveUser(SqlTable table) {
    String user = table.getSchema().getDatabase().getActiveUser();
    if (user == null) {
      user = ADMIN_USER;
    }
    return user;
  }

  private int updateBatch(SqlTable table, List<Row> rows, List<Column> updateColumns) {
    boolean inherit = table.getMetadata().getInheritName() != null;
    int count = 0;
    if (inherit) {
      SqlTable inheritedTable = table.getInheritedTable();
      count = inheritedTable.updateBatch(inheritedTable, rows, updateColumns);
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
          result.add(ref.getJooqField().eq(row.get(ref.getColumnName(), ref.getPrimitiveType())));
        }
      } else {
        result.add(key.getJooqField().eq(row.get(key)));
      }
    }
    return and(result);
  }

  @Override
  public int delete(Iterable<Row> rows, boolean strict) {
    long start = System.currentTimeMillis();

    AtomicInteger nrDeleted = new AtomicInteger(0);
    AtomicInteger nrRowsToDelete = new AtomicInteger(0);
    try {
      db.tx(
          db2 -> {
            SqlTable table = (SqlTable) db2.getSchema(getSchema().getName()).getTable(getName());

            forEachBatch(
                rows,
                DELETE_BATCH_SIZE,
                batch -> {
                  nrRowsToDelete.addAndGet(batch.size());
                  nrDeleted.addAndGet(deleteBatch(table, batch));
                });

            // finally delete in superclass
            if (table.getMetadata().getInheritName() != null) {
              table.getInheritedTable().delete(rows);
            }

            if (strict) {
              assertAllDeleted(nrRowsToDelete.get(), nrDeleted.get());
            }

            // notify handlers
            if (table.getTableListener() != null) {
              table.getTableListener().preparePostDelete(rows);
            }
          });
    } catch (Exception e) {
      throw new SqlMolgenisException("Delete into table " + getName() + " failed", e);
    }

    log(db.getActiveUser(), getName(), start, nrDeleted, "deleted");
    return nrDeleted.get();
  }

  /** Validate that we deleted exactly the number of rows we intended to delete */
  private void assertAllDeleted(int nrRowsToDelete, int nrDeleted) {
    if (nrDeleted != nrRowsToDelete) {
      throw new MolgenisException(
          "Delete failed: attempted to delete "
              + nrRowsToDelete
              + " rows but only deleted "
              + nrDeleted
              + " row"
              + (nrDeleted == 1 ? "" : "s")
              + ". Some specified rows do not exist in table "
              + getName()
              + ". Transaction rolled back.");
    }
  }

  /** Feeds the rows to action in batches of at most batchSize */
  private static void forEachBatch(Iterable<Row> rows, int batchSize, Consumer<List<Row>> action) {
    List<Row> batch = new ArrayList<>();
    for (Row row : rows) {
      batch.add(row);
      if (batch.size() >= batchSize) {
        action.accept(batch);
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      action.accept(batch);
    }
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

  private static int deleteBatch(SqlTable table, Collection<Row> rows) {
    if (rows.isEmpty()) {
      return 0;
    }
    // in case no primary key is defined we cannot identify the rows to delete
    if (table.getMetadata().getPrimaryKeyFields().isEmpty()) {
      throw new MolgenisException(
          "Delete on table " + table.getName() + " failed: no primary key set");
    }
    Condition whereCondition = table.getByRowKey(rows);
    return table.getJooq().deleteFrom(table.getJooqTable()).where(whereCondition).execute();
  }

  private DSLContext getJooq() {
    return ((SqlDatabase) getSchema().getDatabase()).getJooq();
  }

  private List<Row> getRowsByRowKey(Collection<Row> rows) {
    Condition whereCondition = getByRowKey(rows);
    // select typed fields instead of 'selectFrom(table)': the jooq table is untyped, so values
    // would come back as raw jdbc objects (e.g. a PGInterval that Period.parse cannot read)
    List<Field<?>> fields =
        getMetadata().getMutationColumns().stream().<Field<?>>map(Column::getJooqField).toList();
    return getJooq()
        .select(fields)
        .from(getJooqTable())
        .where(whereCondition)
        .fetch()
        .map(r -> new Row(r.intoMap()));
  }

  private Condition getByRowKey(Collection<Row> rows) {
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
                  .eq(
                      cast(
                          r.get(ref.getColumnName(), ref.getPrimitiveType()), ref.getJooqField())));
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
    if (getMetadata().getImportSchema() != null) {
      return (SqlTable)
          getSchema()
              .getDatabase()
              .getSchema(getMetadata().getImportSchema())
              .getTable(getMetadata().getInheritName());
    } else {
      return (SqlTable) getSchema().getTable(getMetadata().getInheritName());
    }
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
