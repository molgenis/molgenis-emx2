package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ColumnLabelNameMapper;
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
                        || !metadata.getColumn(c.getName()).isFile()
                        || store instanceof TableAndFileStore)
            .map(Column::getName)
            .filter(n -> !n.startsWith("mg_") || includeSystemColumns)
            .toList();
    SelectColumn[] select =
        downloadColumnNames.stream().map(SelectColumn::s).toArray(SelectColumn[]::new);

    if (table.getMetadata().getColumnNames().contains(MG_TABLECLASS)) {
      store.writeTable(
          table.getName(),
          downloadColumnNames,
          new ColumnLabelNameMapper(table.getMetadata()),
          table
              .query()
              .select(select)
              .where(
                  f(
                      MG_TABLECLASS,
                      Operator.EQUALS,
                      table.getSchema().getName() + "." + table.getName()))
              .retrieveRows());
    } else {
      store.writeTable(
          table.getName(),
          downloadColumnNames,
          new ColumnLabelNameMapper(table.getMetadata()),
          table.select(select).retrieveRows());
    }

    // in case of zip file we include the attached files
    if (store instanceof TableAndFileStore tableAndFileStore) {
      Emx2Files.outputFiles(tableAndFileStore, table);
    }
  }
}
