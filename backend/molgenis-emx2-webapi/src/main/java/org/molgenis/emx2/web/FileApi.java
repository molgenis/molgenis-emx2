package org.molgenis.emx2.web;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.molgenis.emx2.*;

public class FileApi {
  public static void create(Javalin app) {
    app.get("/{schema}/api/file/{table}/{column}/{id}", FileApi::getFile);
  }

  public static String getFile(Context ctx) throws IOException {
    String tableName = ctx.pathParam("table");
    String columnName = ctx.pathParam("column");
    String id = ctx.pathParam("id");
    Schema schema = getSchema(ctx);
    Table t = schema.getTable(tableName);
    if (t == null) {
      throw new MolgenisException(
          "Download failed: Table '" + tableName + "' not found in schema " + schema.getName());
    }
    Column c = t.getMetadata().getColumn(columnName);
    if (c == null) {
      throw new MolgenisException(
          "Download failed: Column '"
              + columnName
              + "' not found in table "
              + schema.getName()
              + "."
              + tableName);
    }

    List<Row> result =
        t.query()
            // select key
            .select(
                t.getMetadata().getPrimaryKeyFields().stream()
                    .map(f -> s(f.getName()))
                    .toArray(SelectColumn[]::new))
            // select file details
            .select(s(columnName, s("contents"), s("mimetype"), s("filename"), s("extension")))
            .where(f(columnName, f("id", EQUALS, id)))
            .retrieveRows();
    if (result.size() != 1) {
      throw new MolgenisException(
          "Download failed: file id '" + id + "' not found in table " + tableName);
    }
    addFileColumnToResponse(ctx, columnName, result.get(0));
    return "";
  }

  public static void addFileColumnToResponse(Context ctx, String columnName, Row row)
      throws IOException {
    String fileName = row.getString(columnName + "_filename");
    String extension = row.getString(columnName + "_extension");
    String mimetype = row.getString(columnName + "_mimetype");
    byte[] contents = row.getBinary(columnName + "_contents");
    ctx.header(
        "Content-Disposition",
        "attachment; filename=" + (fileName != null ? fileName : columnName + "." + extension));
    ctx.contentType(mimetype);
    try (OutputStream out = ctx.outputStream()) {
      out.write(contents); // autoclosing
      out.flush();
    }
  }
}
