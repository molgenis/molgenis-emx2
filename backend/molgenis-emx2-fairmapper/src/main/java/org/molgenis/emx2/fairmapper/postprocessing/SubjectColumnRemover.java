package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.TableStore;

/** Removes the {@code _subject_*} bookkeeping columns added during RDF transformation. */
public class SubjectColumnRemover {

  private static final String SUBJECT_PREFIX = "_subject_";

  public static void remove(TableStore tableStore) {
    for (String tableName : tableStore.getTableNames()) {
      removeFromTable(tableStore, tableName);
    }
  }

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
