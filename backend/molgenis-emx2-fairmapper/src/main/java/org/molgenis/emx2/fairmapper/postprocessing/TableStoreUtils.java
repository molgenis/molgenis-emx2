package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.Optional;
import java.util.stream.StreamSupport;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.tablestore.TableStore;

public class TableStoreUtils {

  private TableStoreUtils() {
    /* This utility class should not be instantiated */
  }

  private static final String SUBJECT_VARIABLE = "_subject_";

  public static Row getRowForSubject(TableStore tableStore, TableMetadata table, String subject) {
    for (TableMetadata tableMetadata : table.getInheritanceTree()) {
      if (!tableStore.containsTable(tableMetadata.getTableName())) {
        continue;
      }

      Optional<Row> match =
          StreamSupport.stream(
                  tableStore.readTable(tableMetadata.getTableName()).spliterator(), false)
              .filter(row -> row.getString(SUBJECT_VARIABLE).equals(subject))
              .findFirst();

      if (match.isPresent()) {
        return match.get();
      }
    }

    throw new MolgenisException(
        "Referencing non-existing row for table: "
            + table.getTableName()
            + ", for subject: "
            + subject);
  }
}
