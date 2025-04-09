package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Constants.MG_DRAFT;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableAndFileStore;
import org.molgenis.emx2.io.tablestore.TableStore;

public class Emx2Tables {
  private Emx2Tables() {
    // hidden
  }

  public static void outputTableWithSystemColumns(TableStore store, Table table) {
    Emx2Tables.outputTable(store, table, true);
  }

  public static void outputTable(TableStore store, Table table) {
    Emx2Tables.outputTable(store, table, false);
  }

  private static void outputTable(TableStore store, Table table, boolean includeSystemColumns) {
    TableMetadata metadata = table.getMetadata();
    List<String> downloadColumnNames =
        table.getMetadata().getDownloadColumnNames().stream()
            .filter(
                c ->
                    // note that refs have . in the name, these we also allow
                    c.getName().contains(".")
                        // we skip file output unless supported by the format, currently csv.zip and
                        // directory
                        // get original metadata because download columns are format string instead
                        // of file
                        || isFileType(c, metadata)
                        || store instanceof TableAndFileStore)
            .map(Column::getName)
            .filter(name -> !name.startsWith("mg_") || includeSystemColumns)
            .toList();

    Query query = table.query();
    // exclude drafts when system columns are not included
    if (!includeSystemColumns) {
      query.where(f(MG_DRAFT, Operator.NOT_EQUALS, true));
    }
    if (table.getMetadata().getColumnNames().contains(MG_TABLECLASS)) {
      query.where(
          f(MG_TABLECLASS, Operator.EQUALS, table.getSchema().getName() + "." + table.getName()));
    }
    store.writeTable(table.getName(), downloadColumnNames, query.retrieveRows());

    // in case of zip file we include the attached files
    if (store instanceof TableAndFileStore tableAndFileStore) {
      Emx2Files.outputFiles(tableAndFileStore, table);
    }
  }

  private static boolean isFileType(Column c, TableMetadata metadata) {
    if (metadata.getColumn(c.getName()) == null && c.getName().endsWith("_filename")) {
      return true;
    } else return !metadata.getColumn(c.getName()).isFile();
  }
}
