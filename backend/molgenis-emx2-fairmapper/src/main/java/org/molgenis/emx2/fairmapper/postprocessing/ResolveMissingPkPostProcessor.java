package org.molgenis.emx2.fairmapper.postprocessing;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fills in reference columns whose target primary key was not directly available when a row was
 * extracted, only a subject IRI (stored under {@code _subject_<column>}) identifying the row it
 * refers to elsewhere in the table store. For every physical column a reference expands into (see
 * {@link Column#getReferences()}), the value is copied over from the referenced row once it is
 * known.
 *
 * <p>Some references are circular: row A points at row B, and part of B's own primary key is itself
 * a reference back to A. When B is still missing that part, it is backfilled directly from A's own
 * value, since A already carries it.
 *
 * <p>Because tables are resolved in schema order, a table processed early may depend on information
 * that only becomes available once a later table resolves itself (or performs such a backfill).
 * {@link #process} therefore keeps repeating full passes over every table until one pass makes no
 * further changes.
 */
public class ResolveMissingPkPostProcessor implements PostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ResolveMissingPkPostProcessor.class);
  private static final String SUBJECT = "_subject_";

  private final SchemaMetadata schema;
  private boolean progressMadeThisPass;

  public ResolveMissingPkPostProcessor(SchemaMetadata schema) {
    this.schema = schema;
  }

  @Override
  public void process(InMemoryTableStore tableStore) {
    boolean keepGoing;
    do {
      progressMadeThisPass = false;
      for (String tableName : schema.getTableNames()) {
        List<Column> referenceColumns =
            schema.getTableMetadata(tableName).getColumns().stream()
                .filter(Column::isReference)
                .toList();

        tableStore.processTable(
            tableName,
            (rows, source) ->
                rows.forEachRemaining(row -> resolveRow(tableStore, row, referenceColumns)));
      }
      keepGoing = progressMadeThisPass;
    } while (keepGoing);
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
    String subject = row.getString(SUBJECT + column.getName());
    if (subject == null) {
      return;
    }

    Row referencedRow =
        column.getRefTable().getInheritanceTree().stream()
            .flatMap(t -> findRowBySubject(tableStore, t.getTableName(), subject).stream())
            .findFirst()
            .orElseThrow(
                () ->
                    new MolgenisException(
                        "Cannot find reference for missing PK for column: " + column.getName()));

    for (Reference reference : column.getReferences()) {
      if (row.notNull(reference.getColumnName())) {
        continue;
      }

      if (referencedRow.notNull(reference.getReferencedColumnName())) {
        Object value = referencedRow.getValueMap().get(reference.getReferencedColumnName());
        row.set(reference.getColumnName(), value);
        progressMadeThisPass = true;
      } else if (pointsBackAtOwnTable(column, reference)) {
        // The referenced row is missing this part of its own key, and that part is itself a
        // reference back to this row's table: this row already carries the value the referenced
        // row is missing, so use it to complete both sides.
        Object ownValue = row.getValueMap().get(reference.getTargetColumn());
        backfill(referencedRow, reference, ownValue);
        row.set(reference.getColumnName(), ownValue);
        progressMadeThisPass = true;
      }
    }
  }

  private void resolveArrayReference(TableStore tableStore, Row row, Column column) {
    String subjectField = SUBJECT + column.getName();
    if (!row.notNull(subjectField)) {
      return;
    }

    List<Row> referencedRows =
        Arrays.stream(row.getStringArray(subjectField))
            .map(
                subject ->
                    column.getRefTable().getInheritanceTree().stream()
                        .flatMap(
                            t -> findRowBySubject(tableStore, t.getTableName(), subject).stream())
                        .findFirst()
                        .orElseThrow(
                            () ->
                                new MolgenisException(
                                    "Cannot find reference for missing PK for column: "
                                        + column.getName())))
            .toList();

    for (Reference reference : column.getReferences()) {
      // A field can already carry an array that is merely an empty placeholder rather than a
      // genuinely resolved value (e.g. left over from extraction), so only treat it as resolved
      // once its length actually matches the number of rows it should have come from.
      if (isFullyResolvedArray(row, reference, referencedRows.size())) {
        continue;
      }

      List<Object> values = new ArrayList<>();
      boolean allResolved = true;
      for (Row referencedRow : referencedRows) {
        if (referencedRow.notNull(reference.getReferencedColumnName())) {
          values.add(referencedRow.getValueMap().get(reference.getReferencedColumnName()));
        } else if (pointsBackAtOwnTable(column, reference)) {
          Object ownValue = row.getValueMap().get(reference.getTargetColumn());
          backfill(referencedRow, reference, ownValue);
          values.add(ownValue);
        } else {
          // Not resolvable yet; leave the whole array untouched so a later pass can retry once
          // this referenced row has resolved its own missing part, instead of writing a partial
          // result that would then look "already resolved".
          allResolved = false;
        }
      }

      if (allResolved) {
        row.setRefArray(reference.getColumnName(), values.toArray());
        progressMadeThisPass = true;
      }
    }
  }

  private static boolean isFullyResolvedArray(Row row, Reference reference, int expectedSize) {
    Object[] existing = row.getStringArray(reference.getColumnName());
    return existing != null && Array.getLength(existing) == expectedSize;
  }

  /**
   * True when {@code reference} is part of a composite key on the table {@code column} points at,
   * and that particular part is a reference back to {@code column}'s own table - i.e. resolving it
   * would otherwise require the two rows to wait on each other forever.
   */
  private static boolean pointsBackAtOwnTable(Column column, Reference reference) {
    return column.getTable().getAllInheritNames().contains(reference.getTargetTable());
  }

  private static void backfill(Row referencedRow, Reference reference, Object value) {
    logger.info(
        "Referenced row is missing its own '{}': backfilling it with '{}' from the row that "
            + "pointed at it",
        reference.getReferencedColumnName(),
        value);
    referencedRow.set(reference.getReferencedColumnName(), value);
  }

  private static Optional<Row> findRowBySubject(
      TableStore tableStore, String tableName, String subject) {
    return StreamSupport.stream(tableStore.readTable(tableName).spliterator(), false)
        .filter(candidate -> subject.equals(candidate.getString(SUBJECT)))
        .findFirst();
  }
}
