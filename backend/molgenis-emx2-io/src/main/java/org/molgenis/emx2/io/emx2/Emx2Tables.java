package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesDirectory;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Emx2Tables {
  private static final Logger logger = LoggerFactory.getLogger(Emx2Tables.class.getName());

  private Emx2Tables() {
    // hidden
  }

  public static void outputTable(TableStore store, Table table) {
    TableMetadata metadata = table.getMetadata();
    List<String> downloadColumnNames =
        table.getMetadata().getDownloadColumnNames().stream()
            .map(Column::getName)
            .filter(n -> !n.startsWith("mg_"))
            // we skip file output unless supported by the format, currently csv.zip and directory
            .filter(
                n ->
                    !metadata.getColumn(n).isFile()
                        || store instanceof TableStoreForCsvInZipFile
                        || store instanceof TableStoreForCsvFilesDirectory)
            .toList();
    SelectColumn[] select =
        downloadColumnNames.stream().map(c -> s(c)).toArray(SelectColumn[]::new);

    if (table.getMetadata().getColumnNames().contains(MG_TABLECLASS)) {
      store.writeTable(
          table.getName(),
          downloadColumnNames,
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
      store.writeTable(table.getName(), downloadColumnNames, table.select(select).retrieveRows());
    }

    // in case of zip file we include the attached files
    if (store instanceof TableStoreForCsvInZipFile
        || store instanceof TableStoreForCsvFilesDirectory) {
      Emx2Files.outputFiles(store, table);
    }
  }
}
