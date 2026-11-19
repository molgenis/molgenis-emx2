package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.ACCEPT_PARQUET;
import static org.molgenis.emx2.web.DownloadApiUtils.*;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.TempFile;
import org.molgenis.emx2.io.emx2.DownloadColumns;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForParquet;
import org.molgenis.emx2.sql.row.resolvers.ResolveComputedValue;

public final class ParquetApi {

  private static final String PARQUET_EXTENSION = ".parquet";

  private ParquetApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    app.get("/{schema}/api/parquet/{table}", ParquetApi::tableRetrieve);
  }

  private static void tableRetrieve(Context ctx) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    List<Column> columns = table.getMetadata().getColumns();
    Database database = table.getSchema().getDatabase();

    Query query = requestedTableQuery(ctx, table);
    List<String> downloadColumns =
        DownloadColumns.resolve(
            table.getMetadata(), requestedColumns(ctx), includeSystemColumns(ctx));

    try (TempFile tmp = new TempFile(table.getName() + PARQUET_EXTENSION)) {
      TableStore store = new TableStoreForParquet(tmp.get().getParent());
      store.writeTableStreaming(
          table.getName(),
          downloadColumns,
          consumer ->
              query.streamRows(
                  row -> {
                    ResolveComputedValue.apply(database, columns, List.of(row));
                    consumer.accept(row);
                  }));

      tmp.streamTo(ctx.outputStream());
    }

    ctx.contentType(ACCEPT_PARQUET);
    ctx.header("Content-Disposition", "attachment; filename=" + filename(table));
  }

  private static String filename(Table table) {
    return table.getSchema().getMetadata().getName() + "_" + table.getName() + PARQUET_EXTENSION;
  }
}
