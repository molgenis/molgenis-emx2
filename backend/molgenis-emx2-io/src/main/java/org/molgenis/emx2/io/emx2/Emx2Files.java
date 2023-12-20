package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.tablestore.TableAndFileStore;

public class Emx2Files {

  private Emx2Files() {
    // prevent
  }

  public static void outputFiles(TableAndFileStore store, Table table) {
    for (Column c : table.getMetadata().getColumns()) {
      if (c.isFile()) {
        // query the identifiers of this, and then retrieve (slow, but scalable) and write
        List<Row> rows = table.select(s(c.getName())).retrieveRows();
        for (Row r : rows) {
          if (r.notNull(c.getName())) {

            // get the files one by one
            List<Row> fileRows =
                table
                    .select(s(c.getName(), s("id"), s("contents"), s("mimetype"), s("extension")))
                    .where(f(c.getName(), Operator.EQUALS, r.getString(c.getName())))
                    .retrieveRows();

            // only one row
            for (Row f : fileRows) {
              store.writeFile(
                  "_files/"
                      + f.getString(c.getName())
                      + "."
                      + f.getString(c.getName() + "_extension"),
                  f.getBinary(c.getName() + "_contents"));
            }
          }
        }
      }
    }
  }
}
