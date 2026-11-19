package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.ACCEPT_PARQUET;
import static org.molgenis.emx2.web.DownloadApiUtils.*;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ImportTableTask;
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
    app.post("/{schema}/api/parquet/{table}", ParquetApi::tableUpdate);
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

  private static void tableUpdate(Context ctx) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);

    try (TempFile tmp = new TempFile(table.getName() + PARQUET_EXTENSION)) {
      Files.copy(ctx.bodyInputStream(), tmp.get());
      TableStore store = new TableStoreForParquet(tmp.get().getParent());
      ImportTableTask task = new ImportTableTask(store, table, false, UpdateMode.DEFAULT_MODE);
      task.run();

      ctx.status(200);
      ctx.contentType(ContentType.TEXT_PLAIN);
      ctx.result(String.valueOf(task.getTotal()));
    }
  }
}
