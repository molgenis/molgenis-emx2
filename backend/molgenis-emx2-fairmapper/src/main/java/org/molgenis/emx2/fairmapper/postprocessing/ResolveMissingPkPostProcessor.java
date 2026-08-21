package org.molgenis.emx2.fairmapper.postprocessing;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fills in reference columns that, right after extraction, only know the row they point to as a
 * subject IRI (stored under {@code _subject_<column>}), not yet as an actual key value. Once the
 * referenced row is available, its key value is copied into the reference column (see {@link
 * Column#getReferences()} for how one reference can expand into several columns).
 *
 * <p>Sometimes two rows depend on each other through composite keys, so neither can be resolved
 * first. Example: table {@code Order} has a reference column {@code customer} pointing at table
 * {@code Customer}, whose key is made of two parts, one of which points back to {@code Order}:
 *
 * <pre>
 *   Order
 *     orderNumber   (key)
 *     customer  ->  Customer
 *
 *   Customer
 *     customerNumber  (key, part 1)
 *     order       ->  Order.orderNumber   (key, part 2)
 * </pre>
 *
 * <p>To fill in {@code customer} on the {@code Order} row, we need {@code Customer.order} to
 * already be filled in. But {@code Customer.order} can only be filled in using this very {@code
 * Order} row. Waiting for {@code Customer} to resolve first would never work, since it's waiting on
 * us too. So instead we just copy {@code Order}'s own {@code orderNumber} straight into {@code
 * Customer.order}. See {@link #pointsBackAtOwnTable} and {@link #completeMutualKey} for where this
 * happens.
 *
 * <p>Tables are resolved in schema order, so a table processed early may need a value that only
 * becomes available once a later table is resolved (or gets this kind of fix-up). {@link #process}
 * therefore keeps repeating passes over every table until a pass makes no more changes, or a
 * maximum number of iterations is reached.
 */
public class ResolveMissingPkPostProcessor implements PostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ResolveMissingPkPostProcessor.class);
  private static final String SUBJECT = "_subject_";
  private static final int MAX_NR_ITERATIONS = 10;

  private final SchemaMetadata schema;
  private boolean changesMade;

  public ResolveMissingPkPostProcessor(SchemaMetadata schema) {
    this.schema = schema;
  }

  @Override
  public void process(InMemoryTableStore tableStore) {
    int nrIterations = 0;

    while (nrIterations < MAX_NR_ITERATIONS) {
      changesMade = false;
      nrIterations++;

      for (String tableName : schema.getTableNames()) {
        List<Column> referenceColumns = getReferenceColumnsForTable(tableName);
        tableStore.processTable(
            tableName,
            (rows, source) ->
                rows.forEachRemaining(row -> resolveRow(tableStore, row, referenceColumns)));
      }

      if (!changesMade) {
        break;
      }
    }
  }

  private List<Column> getReferenceColumnsForTable(String tableName) {
    return schema.getTableMetadata(tableName).getColumns().stream()
        .filter(Column::isReference)
        .toList();
  }

  private void resolveRow(TableStore tableStore, Row row, List<Column> referenceColumns) {
    for (Column column : referenceColumns) {
      if (column.isArray()) {
        resolveArrayReference(tableStore, row, column);
      } else {
        resolveSingleReference(tableStore, row, column);
      }
    }
  }

  private void resolveSingleReference(TableStore tableStore, Row row, Column column) {
    String field = subjectField(column);
    if (!row.notNull(field)) {
      return;
    }

    String subject = row.getString(field);
    Row referencedRow = TableStoreUtils.getRowForSubject(tableStore, column.getRefTable(), subject);

    for (Reference reference : column.getReferences()) {
      if (row.notNull(reference.getColumnName())) {
        continue;
      }

      Optional<Object> value = readAvailableValue(reference, referencedRow);
      if (value.isEmpty() && pointsBackAtOwnTable(column, reference)) {
        // These two rows depend on each other (see the class doc example): the referenced row
        // can't resolve this on its own, so we write the value into both rows right here.
        value = Optional.of(completeMutualKey(row, referencedRow, reference));
      }

      if (value.isPresent()) {
        row.set(reference.getColumnName(), value.get());
        changesMade = true;
      }
    }
  }

  private void resolveArrayReference(TableStore tableStore, Row row, Column column) {
    String field = subjectField(column);
    if (!row.notNull(field)) {
      return;
    }

    TableMetadata refTable = column.getRefTable();
    List<Row> referencedRows =
        Arrays.stream(row.getStringArray(field))
            .map(subject -> TableStoreUtils.getRowForSubject(tableStore, refTable, subject))
            .toList();

    for (Reference reference : column.getReferences()) {
      // The field might already hold an empty array left over from extraction, not a real
      // result. Only treat it as resolved once it has as many values as there are rows.
      if (isFullyResolvedArray(row, reference, referencedRows.size())) {
        continue;
      }

      List<Object> values = new ArrayList<>();
      boolean allResolved = true;
      for (Row referencedRow : referencedRows) {
        Optional<Object> value = readAvailableValue(reference, referencedRow);
        if (value.isEmpty() && pointsBackAtOwnTable(column, reference)) {
          value = Optional.of(completeMutualKey(row, referencedRow, reference));
        }

        if (value.isPresent()) {
          values.add(value.get());
        } else {
          // Not resolvable yet. Leave the array untouched below so a later pass can retry --
          // writing a partial result now would make it look already done. Keep going through
          // the rest of the rows anyway, since other referenced rows here may still need their
          // own missing key part completed via completeMutualKey.
          allResolved = false;
        }
      }

      if (allResolved) {
        row.setRefArray(reference.getColumnName(), values.toArray());
        changesMade = true;
      }
    }
  }

  /** Returns {@code reference}'s value if {@code referencedRow} already has it. Read-only. */
  private static Optional<Object> readAvailableValue(Reference reference, Row referencedRow) {
    if (referencedRow.notNull(reference.getReferencedColumnName())) {
      return Optional.of(referencedRow.getValueMap().get(reference.getReferencedColumnName()));
    }
    return Optional.empty();
  }

  private static String subjectField(Column column) {
    return SUBJECT + column.getName();
  }

  private static boolean isFullyResolvedArray(Row row, Reference reference, int expectedSize) {
    Object[] existing = row.getStringArray(reference.getColumnName());
    return existing != null && Array.getLength(existing) == expectedSize;
  }

  /**
   * True when {@code reference} points back at {@code column}'s own table, like {@code
   * Customer.order} pointing back at {@code Order} in the example above. Resolving it the normal
   * way would mean the two rows wait on each other forever.
   */
  private static boolean pointsBackAtOwnTable(Column column, Reference reference) {
    return column.getTable().getAllInheritNames().contains(reference.getTargetTable());
  }

  /**
   * Writes {@code row}'s own value for {@code reference} into {@code referencedRow}'s matching key
   * column, and returns that value. Mutates {@code referencedRow}. Only call this for the
   * mutual-key case (see {@link #pointsBackAtOwnTable}), where the referenced row cannot fill in
   * that column on its own.
   */
  private Object completeMutualKey(Row row, Row referencedRow, Reference reference) {
    Object ownValue = row.getValueMap().get(reference.getTargetColumn());
    logger.info(
        "Referenced row is missing its own '{}': completing it with '{}' from the row that pointed at it",
        reference.getReferencedColumnName(),
        ownValue);
    referencedRow.set(reference.getReferencedColumnName(), ownValue);
    return ownValue;
  }
}
