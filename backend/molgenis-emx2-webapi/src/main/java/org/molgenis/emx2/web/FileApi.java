package org.molgenis.emx2.web;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.get;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import spark.Request;
import spark.Response;

public class FileApi {
  public static void create() {
    /// :schema/api/file/:table/:column/:id
    get("/:schema/api/file/:table/:column/:id", FileApi::getFile);
  }

  public static String getFile(Request request, Response response) throws IOException {
    String tableName = request.params("table");
    String columnName = request.params("column");
    String id = request.params("id");
    Schema schema = getSchema(request);
    Table t = schema.getTable(tableName);
    if (t == null) {
      throw new MolgenisException(
          "Download failed: Table '" + tableName + "' not found in schema " + schema.getName());
    }
    List<Row> result =
        t.query()
            .select(s(columnName, s("contents"), s("mimetype"), s("extension")))
            .where(f(columnName, f("id", EQUALS, id)))
            .retrieveRows();
    if (result.size() != 1) {
      throw new MolgenisException(
          "Download failed: file id '" + id + "' not found in table " + tableName);
    }
    String ext = result.get(0).getString(columnName + "_extension");
    String mimetype = result.get(0).getString(columnName + "_mimetype");
    byte[] contents = result.get(0).getBinary(columnName + "_contents");
    response
        .raw()
        .setHeader(
            "Content-Disposition",
            "attachment; filename=" + tableName + "-" + columnName + "-" + id + "." + ext);
    response.raw().setContentType(mimetype);
    try (OutputStream out = response.raw().getOutputStream()) {
      out.write(contents); // autoclosing
      out.flush();
    }
    return "";
  }
}
