package org.molgenis.emx2.fairmapper.postprocessing;

import static org.molgenis.emx2.rdf.generators.query.SparqlVariableUtil.SUBJECT_NAME;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;

/**
 * Fills in reference columns that are still empty after extraction. At that point a reference only
 * holds the subject IRI of the row it points to (in {@code _subject_<column>}). This class looks up
 * that row and copies its key value into the reference column. If the referenced table has a key
 * made of several columns, the reference is filled in per key column (see {@link
 * Column#getReferences()}).
 *
 * <p>In strict mode, a reference to a table that is not in the table store throws an exception. In
 * lenient mode it logs a warning and skips it.
 *
 * <p>Sometimes two rows need each other's values. Example: table {@code Order} has a reference
 * column {@code customer} pointing at table {@code Customer}. The key of {@code Customer} has two
 * parts, and one of them points back to {@code Order}:
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
 * <p>To fill in {@code customer} on an {@code Order} row, we need {@code Customer.order}. But
 * {@code Customer.order} is filled in from that same {@code Order} row, so neither side can go
 * first. In that case we copy the {@code orderNumber} of the {@code Order} row directly into {@code
 * Customer.order}. We only do this when the {@code Customer} row points back at this exact {@code
 * Order} row. See {@link #pointsBackAtSelf} and {@link #completeMutualKey}.
 *
 * <p>Tables are processed in schema order, so a table can need a value from a table that comes
 * later. {@link #process} therefore repeats over all tables until nothing changes anymore, or until
 * {@code MAX_NR_ITERATIONS} passes are done.
 */
public class ResolveMissingPkPostProcessor implements PostProcessor {

  private static final Logger logger = LogManager.getLogger(ResolveMissingPkPostProcessor.class);
  private static final int MAX_NR_ITERATIONS = 10;

  private final SchemaMetadata schema;
  private final boolean strict;
  private boolean changesMade;

  private ResolveMissingPkPostProcessor(SchemaMetadata schema, boolean strict) {
    this.schema = schema;
    this.strict = strict;
  }

  public static ResolveMissingPkPostProcessor strictMode(SchemaMetadata schema) {
    return new ResolveMissingPkPostProcessor(schema, true);
  }

  public static ResolveMissingPkPostProcessor lenientMode(SchemaMetadata schema) {
    return new ResolveMissingPkPostProcessor(schema, false);
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

    if (nrIterations >= MAX_NR_ITERATIONS) {
      logger.warn("Max number of iterations exceeded for missing pk post processor");
    }
  }

  private List<Column> getReferenceColumnsForTable(String tableName) {
    return schema.getTableMetadata(tableName).getColumns().stream()
        .filter(Column::isReference)
        .toList();
  }

  private void resolveRow(TableStore tableStore, Row row, List<Column> referenceColumns) {
    for (Column column : referenceColumns) {
      if (!tableStore.containsTable(column.getRefTableName())) {
        if (strict) {
          throw new MolgenisException(
              "Cannot resolve pk post processor for row: "
                  + row
                  + " in table "
                  + column.getTableName());
        }

        logger.warn(
            "Unable to resolve reference for row: {} in table; {}", row, column.getTableName());
        continue;
      }

      if (!row.notEmpty(subjectField(column))) {
        continue;
      }

      if (column.isArray()) {
        resolveArrayReference(tableStore, row, column);
      } else {
        resolveSingleReference(tableStore, row, column);
      }
    }
  }

  private void resolveSingleReference(TableStore tableStore, Row row, Column column) {
    String subject = row.getString(subjectField(column));
    Row referencedRow = TableStoreUtils.getRowForSubject(tableStore, column.getRefTable(), subject);

    for (Reference reference : column.getReferences()) {
      if (row.notEmpty(reference.getColumnName())) {
        continue;
      }

      Optional<Object> value = readAvailableValue(reference, referencedRow);
      if (value.isEmpty() && pointsBackAtSelf(row, referencedRow, column, reference)) {
        // These two rows depend on each other (see the class doc example): the referenced row
        // can't resolve this on its own, so we write the value into both rows right here.
        value = Optional.ofNullable(completeMutualKey(row, referencedRow, reference));
      }

      if (value.isPresent()) {
        row.set(reference.getColumnName(), value.get());
        changesMade = true;
      }
    }
  }

  private void resolveArrayReference(TableStore tableStore, Row row, Column column) {
    TableMetadata refTable = column.getRefTable();
    List<Row> referencedRows =
        Arrays.stream(row.getStringArray(subjectField(column)))
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
        // These two rows depend on each other (see the class doc example): the referenced row
        // can't resolve this on its own, so we write the value into both rows right here.
        if (value.isEmpty() && pointsBackAtSelf(row, referencedRow, column, reference)) {
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
    if (referencedRow.notEmpty(reference.getReferencedColumnName())) {
      return Optional.of(referencedRow.getValueMap().get(reference.getReferencedColumnName()));
    }
    return Optional.empty();
  }

  private static String subjectField(Column column) {
    return SUBJECT_NAME + column.getName();
  }

  private static boolean isFullyResolvedArray(Row row, Reference reference, int expectedSize) {
    Object[] existing = row.getStringArray(reference.getColumnName());
    return existing != null && Array.getLength(existing) == expectedSize;
  }

  /**
   * True when {@code referencedRow} points back at {@code row}, like {@code Customer.order}
   * pointing back at {@code Order} in the class doc example:
   *
   * <ul>
   *   <li>{@code reference} must target {@code row}'s own table (or one it inherits from).
   *   <li>If {@code referencedRow} holds a subject IRI for that back-reference, it must equal
   *       {@code row}'s subject.
   *   <li>If it holds none, we assume it points back at {@code row}.
   * </ul>
   */
  @SuppressWarnings("PMD.SimplifyBooleanReturns") // easier to read
  private static boolean pointsBackAtSelf(
      Row row, Row referencedRow, Column column, Reference reference) {
    if (!column.getTable().getAllInheritNames().contains(reference.getTargetTable())) {
      return false;
    }

    if (referencedRow.notEmpty(SUBJECT_NAME + reference.getReferencedColumnName())) {
      return referencedRow
          .getString(SUBJECT_NAME + reference.getReferencedColumnName())
          .equals(row.getString(SUBJECT_NAME));
    }

    return true;
  }

  /**
   * Writes {@code row}'s own value for {@code reference} into {@code referencedRow}'s matching key
   * column, and returns that value. Mutates {@code referencedRow}. Only call this for the
   * mutual-key case (see {@link #pointsBackAtSelf}), where the referenced row cannot fill in that
   * column on its own.
   */
  private Object completeMutualKey(Row row, Row referencedRow, Reference reference) {
    Object ownValue = row.getValueMap().get(reference.getTargetColumn());
    logger.info(
        "Referenced row is missing its own '{}': completing it with '{}' from the row that pointed at it",
        reference::getReferencedColumnName,
        () -> ownValue);
    referencedRow.set(reference.getReferencedColumnName(), ownValue);
    return ownValue;
  }
}
