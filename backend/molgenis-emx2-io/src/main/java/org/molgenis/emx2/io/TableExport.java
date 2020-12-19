package org.molgenis.emx2.io;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

import java.nio.file.Path;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.SelectColumn;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.rowstore.TableStore;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvFile;
import org.molgenis.emx2.io.rowstore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.io.rowstore.TableStoreForXlsxFile;

public class TableExport {

  private TableExport() {
    // hidden
  }

  public static void toZipFile(Path zipFile, Table table) {
    executeExport(new TableStoreForCsvInZipFile(zipFile), table);
  }

  public static void toExcelFile(Path excelFile, Table table) {
    executeExport(new TableStoreForXlsxFile(excelFile), table);
  }

  public static void toCsvFile(Path csvFile, Table table) {
    executeExport(new TableStoreForCsvFile(csvFile), table);
  }

  public static void executeExport(TableStore store, Table table) {
    if (table.getMetadata().getColumn(MG_TABLECLASS) != null) {
      SelectColumn[] select =
          table.getMetadata().getDownloadColumns().stream()
              .map(c -> c.getName())
              .filter(n -> !n.equals(MG_TABLECLASS))
              .map(c -> s(c))
              .toArray(SelectColumn[]::new);
      store.writeTable(
          table.getName(),
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
      store.writeTable(table.getName(), table.retrieveRows());
    }
  }
}
