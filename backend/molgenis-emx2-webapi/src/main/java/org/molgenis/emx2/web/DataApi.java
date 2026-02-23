package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.*;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.molgenis.emx2.MolgenisException;

public class DataApi {

  private DataApi() {}

  public static void create(Javalin app) {
    final String apiPath = "/{schema}/api/data/";

    app.get(apiPath + "_schema", DataApi::handleGetSchema);
    app.post(apiPath + "_schema", DataApi::handlePostSchema);
    app.delete(apiPath + "_schema", DataApi::handleDeleteSchema);
    app.get(apiPath + "_data", DataApi::handleGetData);
    app.post(apiPath + "_data", DataApi::handlePostData);
    app.get(apiPath + "_all", DataApi::handleGetAll);
    app.post(apiPath + "_all", DataApi::handlePostAll);
    app.get(apiPath + "_members", DataApi::handleGetMembers);
    app.get(apiPath + "_settings", DataApi::handleGetSettings);
    app.get(apiPath + "_changelog", DataApi::handleGetChangelog);
    app.get(
        apiPath + "_context",
        ctx -> {
          String schema = ctx.pathParam("schema");
          ctx.req()
              .getRequestDispatcher("/" + schema + "/api/jsonld/_context")
              .forward(ctx.req(), ctx.res());
        });
    app.get(apiPath + "{table}", DataApi::handleGetTable);
    app.post(apiPath + "{table}", DataApi::handlePostTable);
    app.delete(apiPath + "{table}", DataApi::handleDeleteTable);
    app.get(apiPath + "{table}/*", DataApi::handleGetRow);
    app.put(apiPath + "{table}/*", DataApi::handlePutRow);
    app.delete(apiPath + "{table}/*", DataApi::handleDeleteRow);
  }

  private static Format determineFormat(Context ctx) {
    String accept = ctx.header("Accept");
    if (accept == null || accept.isEmpty()) {
      return Format.JSON;
    }

    String[] acceptTypes = accept.split(",");
    for (String acceptType : acceptTypes) {
      String trimmed = acceptType.split(";")[0].trim();

      if (trimmed.equals(ACCEPT_CSV)) {
        return Format.CSV;
      } else if (trimmed.equals(ACCEPT_EXCEL)) {
        return Format.EXCEL;
      } else if (trimmed.equals(ACCEPT_ZIP)) {
        return Format.ZIP;
      } else if (trimmed.equals(ACCEPT_JSONLD)) {
        return Format.JSONLD;
      } else if (trimmed.equals(ACCEPT_TTL)) {
        return Format.TTL;
      } else if (trimmed.equals(ACCEPT_YAML)) {
        return Format.YAML;
      } else if (trimmed.equals(ACCEPT_JSON)) {
        return Format.JSON;
      }
    }

    return Format.JSON;
  }

  private enum Format {
    JSON,
    YAML,
    JSONLD,
    TTL,
    CSV,
    EXCEL,
    ZIP
  }

  private static void handleGetSchema(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case CSV -> internalPath = "/" + schema + "/api/csv/_schema";
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_schema";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_schema";
      case JSONLD -> internalPath = "/" + schema + "/api/jsonld/_schema";
      case TTL -> internalPath = "/" + schema + "/api/ttl/_schema";
      case YAML -> internalPath = "/" + schema + "/api/yaml/_schema";
      case JSON -> internalPath = "/" + schema + "/api/json/_schema";
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handlePostSchema(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case CSV -> internalPath = "/" + schema + "/api/csv/_schema";
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_schema";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_schema";
      case JSON -> internalPath = "/" + schema + "/api/json/_schema";
      case YAML -> internalPath = "/" + schema + "/api/yaml/_schema";
      case JSONLD -> internalPath = "/" + schema + "/api/jsonld/_schema";
      default ->
          throw new MolgenisException("POST _schema not supported for " + format + " format");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleDeleteSchema(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case CSV -> internalPath = "/" + schema + "/api/csv/_schema";
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_schema";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_schema";
      case JSON -> internalPath = "/" + schema + "/api/json/_schema";
      case YAML -> internalPath = "/" + schema + "/api/yaml/_schema";
      case JSONLD -> internalPath = "/" + schema + "/api/jsonld/_schema";
      default ->
          throw new MolgenisException("DELETE _schema not supported for " + format + " format");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleGetData(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_data";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_data";
      case JSON -> internalPath = "/" + schema + "/api/json/_data";
      case YAML -> internalPath = "/" + schema + "/api/yaml/_data";
      case JSONLD -> internalPath = "/" + schema + "/api/jsonld/_data";
      case TTL -> internalPath = "/" + schema + "/api/ttl/_data";
      default -> throw new MolgenisException("GET _data not supported for " + format + " format");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handlePostData(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_data";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_data";
      default -> throw new MolgenisException("POST _data only supports Excel and ZIP formats");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleGetAll(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_all";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_all";
      case JSON -> internalPath = "/" + schema + "/api/json/_all";
      case YAML -> internalPath = "/" + schema + "/api/yaml/_all";
      case JSONLD -> internalPath = "/" + schema + "/api/jsonld/_all";
      case TTL -> internalPath = "/" + schema + "/api/ttl/_all";
      default -> throw new MolgenisException("GET _all not supported for " + format + " format");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handlePostAll(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_all";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_all";
      default -> throw new MolgenisException("POST _all only supports Excel and ZIP formats");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleGetMembers(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case CSV -> internalPath = "/" + schema + "/api/csv/_members";
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_members";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_members";
      case JSON -> internalPath = "/" + schema + "/api/json/_members";
      case YAML -> internalPath = "/" + schema + "/api/yaml/_members";
      default ->
          throw new MolgenisException("GET _members not supported for " + format + " format");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleGetSettings(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case CSV -> internalPath = "/" + schema + "/api/csv/_settings";
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_settings";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_settings";
      case JSON -> internalPath = "/" + schema + "/api/json/_settings";
      case YAML -> internalPath = "/" + schema + "/api/yaml/_settings";
      default ->
          throw new MolgenisException("GET _settings not supported for " + format + " format");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleGetChangelog(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String internalPath = null;

    switch (format) {
      case CSV -> internalPath = "/" + schema + "/api/csv/_changelog";
      case EXCEL -> internalPath = "/" + schema + "/api/excel/_changelog";
      case ZIP -> internalPath = "/" + schema + "/api/zip/_changelog";
      case JSON -> internalPath = "/" + schema + "/api/json/_changelog";
      case YAML -> internalPath = "/" + schema + "/api/yaml/_changelog";
      default ->
          throw new MolgenisException("GET _changelog not supported for " + format + " format");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleGetTable(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String table = ctx.pathParam("table");
    String internalPath = null;

    switch (format) {
      case CSV -> internalPath = "/" + schema + "/api/csv/" + table;
      case EXCEL -> internalPath = "/" + schema + "/api/excel/" + table;
      case ZIP -> internalPath = "/" + schema + "/api/zip/" + table;
      case JSON -> internalPath = "/" + schema + "/api/json/" + table;
      case YAML -> internalPath = "/" + schema + "/api/yaml/" + table;
      case JSONLD -> internalPath = "/" + schema + "/api/jsonld/" + table;
      case TTL -> internalPath = "/" + schema + "/api/ttl/" + table;
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handlePostTable(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String table = ctx.pathParam("table");
    String internalPath = null;

    switch (format) {
      case CSV -> internalPath = "/" + schema + "/api/csv/" + table;
      case EXCEL -> internalPath = "/" + schema + "/api/excel/" + table;
      case ZIP -> internalPath = "/" + schema + "/api/zip/" + table;
      case JSON -> internalPath = "/" + schema + "/api/json/" + table;
      case YAML -> internalPath = "/" + schema + "/api/yaml/" + table;
      case JSONLD -> internalPath = "/" + schema + "/api/jsonld/" + table;
      default -> throw new MolgenisException("POST table not supported for " + format + " format");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleDeleteTable(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String table = ctx.pathParam("table");
    String internalPath = null;

    switch (format) {
      case CSV -> internalPath = "/" + schema + "/api/csv/" + table;
      case EXCEL -> internalPath = "/" + schema + "/api/excel/" + table;
      case ZIP -> internalPath = "/" + schema + "/api/zip/" + table;
      case JSON -> internalPath = "/" + schema + "/api/json/" + table;
      case YAML -> internalPath = "/" + schema + "/api/yaml/" + table;
      case JSONLD -> internalPath = "/" + schema + "/api/jsonld/" + table;
      default ->
          throw new MolgenisException("DELETE table not supported for " + format + " format");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleGetRow(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String table = ctx.pathParam("table");
    String path = ctx.path();
    String rowId = path.substring(path.indexOf(table) + table.length() + 1);
    String internalPath = null;

    switch (format) {
      case JSON -> internalPath = "/" + schema + "/api/json/" + table + "/" + rowId;
      case YAML -> internalPath = "/" + schema + "/api/yaml/" + table + "/" + rowId;
      default -> throw new MolgenisException("GET row only supports JSON and YAML formats");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handlePutRow(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String table = ctx.pathParam("table");
    String path = ctx.path();
    String rowId = path.substring(path.indexOf(table) + table.length() + 1);
    String internalPath = null;

    switch (format) {
      case JSON -> internalPath = "/" + schema + "/api/json/" + table + "/" + rowId;
      case YAML -> internalPath = "/" + schema + "/api/yaml/" + table + "/" + rowId;
      default -> throw new MolgenisException("PUT row only supports JSON and YAML formats");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }

  private static void handleDeleteRow(Context ctx) throws Exception {
    Format format = determineFormat(ctx);
    String schema = ctx.pathParam("schema");
    String table = ctx.pathParam("table");
    String path = ctx.path();
    String rowId = path.substring(path.indexOf(table) + table.length() + 1);
    String internalPath = null;

    switch (format) {
      case JSON -> internalPath = "/" + schema + "/api/json/" + table + "/" + rowId;
      case YAML -> internalPath = "/" + schema + "/api/yaml/" + table + "/" + rowId;
      default -> throw new MolgenisException("DELETE row only supports JSON and YAML formats");
    }

    ctx.req().getRequestDispatcher(internalPath).forward(ctx.req(), ctx.res());
  }
}
