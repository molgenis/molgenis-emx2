package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.TableStore;

/**
 * Strips internal `_subject_` columns from every table in a {@link TableStore}.
 *
 * <p>Earlier pipeline steps carry the RDF subject IRI of a row, and of resources related to it, in
 * columns prefixed with {@code _subject_} (e.g. {@code _subject_}, {@code _subject_publisher}), so
 * that this information remains available for intermediate processing. These columns are internal
 * bookkeeping and are not part of the target schema, so this post-processor removes them from every
 * row of every table before the data is uploaded.
 *
 * <p>A table whose first row has no {@code _subject_}-prefixed columns is left untouched.
 *
 * <p><b>Note:</b> this step should run last in the post-processing pipeline. Other post-processors
 * (e.g. those matching or linking rows across tables) may still rely on the {@code _subject_}
 * columns, so removing them earlier would break those steps.
 */
public class SubjectColumnCleaner implements PostProcessor {

  private static final String SUBJECT_PREFIX = "_subject_";

  @Override
  public void process(TableStore tableStore) {
    for (String tableName : tableStore.getTableNames()) {
      removeFromTable(tableStore, tableName);
    }
  }

  /** Removes all {@code _subject_}-prefixed columns from every row of {@code tableName}. */
  private static void removeFromTable(TableStore tableStore, String tableName) {
    Iterator<Row> rows = tableStore.readTable(tableName).iterator();
    if (!rows.hasNext()) {
      return;
    }

    Row first = rows.next();
    Set<String> subjectColumns =
        first.getColumnNames().stream()
            .filter(name -> name.startsWith(SUBJECT_PREFIX))
            .collect(Collectors.toSet());

    if (subjectColumns.isEmpty()) {
      return;
    }

    tableStore.processTable(
        tableName,
        (iterator, source) -> iterator.forEachRemaining(row -> subjectColumns.forEach(row::clear)));
  }
}
