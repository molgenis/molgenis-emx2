package org.molgenis.emx2.web;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jsoniter.JsonIterator;
import com.jsoniter.ValueType;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.TypeLiteral;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.MolgenisException;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.*;
import static spark.Spark.get;

public class JsonApi {
  private static ObjectWriter writer;

  private JsonApi() {
    // hide constructor
  }

  public static void create() {

    // database level operations
    final String databasePath = "/api/json"; // NOSONAR
    get(databasePath, JsonApi::getSchemas);
    post(databasePath, JsonApi::postSchemas);

    // schema level operations
    final String schemaPath = "/api/json/:schema"; // NOSONAR
    get(schemaPath, JsonApi::getTables);
    delete(schemaPath, JsonApi::deleteTables);

    // table level operations
    final String tablePath = "/api/json/:schema/:table"; // NOSONAR
    get(tablePath, JsonApi::getRows);
    post(tablePath, JsonApi::postRows);
    delete(tablePath, JsonApi::deleteRows);
  }

  private static String getSchemas(Request request, Response response) {
    Map<String, String> schemas = new LinkedHashMap<>();
    for (String schemaName : getAuthenticatedDatabase(request).getSchemaNames()) {
      schemas.put(schemaName, request.url() + "/" + schemaName);
    }
    response.status(200);
    response.type(ACCEPT_JSON);
    return JsonStream.serialize(schemas);
  }

  private static String postSchemas(Request request, Response response) {
    Row row = jsonToRow(request.body());
    getAuthenticatedDatabase(request).createSchema(row.getString("name"));
    response.status(200);
    response.type(ACCEPT_JSON);
    return "Create schema success";
  }

  private static String getTables(Request request, Response response) throws IOException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    String json = schemaToJson(schema.getMetadata());
    response.type(ACCEPT_JSON);
    response.status(200);
    return json;
  }

  private static String deleteTables(Request request, Response response) {
    getAuthenticatedDatabase(request).dropSchema(request.params(SCHEMA));
    response.status(200);
    response.type(ACCEPT_JSON);
    return "Delete schema success";
  }

  private static String getRows(Request request, Response response) throws IOException {
    response.type(ACCEPT_JSON);
    response.status(200);
    return rowsToJson(getTable(request).retrieve());
  }

  private static String postRows(Request request, Response response) {
    int count = getTable(request).insert(jsonToRows(request.body()));
    response.status(200);
    response.type(ACCEPT_JSON);
    return "" + count;
  }

  private static String deleteRows(Request request, Response response) {
    int count = getTable(request).delete(jsonToRows(request.body()));
    response.status(200);
    response.type(ACCEPT_JSON);
    return "" + count;
  }

  public static List<Row> jsonToRows(String json) {
    ArrayList<Row> rows = new ArrayList<>();

    List<Map<String, Object>> data =
        JsonIterator.deserialize(json, new TypeLiteral<ArrayList<Map<String, Object>>>() {});

    for (Map<String, Object> values : data) {
      rows.add(new Row(values));
    }

    return rows;
  }

  static String rowsToJson(Iterable<Row> rows) throws JsonProcessingException {
    List<Map<String, Object>> values = new ArrayList<>();
    for (Row r : rows) {
      Map<String, Object> map = r.getValueMap();
      preprocessRow(map);
      values.add(r.getValueMap());
    }
    return getWriter().writeValueAsString(values);
  }

  public static Row jsonToRow(String json) {
    Map<String, Object> map =
        JsonIterator.deserialize(json, new TypeLiteral<Map<String, Object>>() {});
    return new Row(map);
  }

  public static Row jsonToRow(TableMetadata t, Any json) {
    Row r = new Row();
    for (Column c : t.getColumns()) {
      try {
        switch (c.getColumnType()) {
          case INT:
            r.setInt(c.getName(), json.get(c.getName()).toInt());
            break;
          case DECIMAL:
            r.setDecimal(c.getName(), json.get(c.getName()).toDouble());
            break;
          case STRING:
            if (ValueType.STRING.equals(json.get(c.getName()).valueType())) {
              r.setString(c.getName(), json.get(c.getName()).toString());
              break;
            } else throw new IllegalArgumentException();
          default:
            throw new UnsupportedOperationException(
                "data type " + c.getColumnType() + " not yet implemented");
        }

      } catch (Exception e) {
        throw new MolgenisException(
            String.format(
                "Malformed json: expected '%s' to be of type '%s' but found '%s'. Total object: %s",
                c.getName(), c.getColumnType(), json.get(c.getName()).valueType(), json),
            e);
      }
    }
    return r;
  }

  private static void preprocessRow(Map<String, Object> map) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof UUID)
        map.put(entry.getKey(), ((UUID) entry.getValue()).toString());
    }
  }

  static String schemaToJson(SchemaMetadata schema) throws IOException {
    org.molgenis.emx2.web.json.Schema s = new org.molgenis.emx2.web.json.Schema(schema);
    StringWriter out = new StringWriter();
    getWriter().writeValue(out, s);
    return out.toString();
  }

  static SchemaMetadata jsonToSchema(String json) throws IOException {
    org.molgenis.emx2.web.json.Schema s =
        new ObjectMapper().readValue(json, org.molgenis.emx2.web.json.Schema.class);
    return s.getSchemaMetadata();
  }

  public static ObjectWriter getWriter() {

    if (writer == null) {
      DefaultPrettyPrinter printer =
          new DefaultPrettyPrinter()
              .withArrayIndenter(new DefaultIndenter("  ", "\n"))
              .withObjectIndenter(new DefaultIndenter("  ", "\n"));
      writer =
          new ObjectMapper()
              .addMixIn(SchemaMetadata.class, MixinForJsonIgnore.class)
              .addMixIn(TableMetadata.class, MixinForJsonIgnore.class)
              .addMixIn(Column.class, MixinForJsonIgnore.class)
              .addMixIn(SqlDatabase.class, MixinForJsonIgnore.class)
              .addMixIn(DSLContext.class, MixinForJsonIgnore.class)
              .setSerializationInclusion(JsonInclude.Include.NON_NULL)
              .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
              .writer(printer);
    }
    return writer;
  }

  @JsonIgnoreType
  private static class MixinForJsonIgnore {}
}
