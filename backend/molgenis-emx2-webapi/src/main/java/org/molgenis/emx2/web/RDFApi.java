package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.TABLE;
import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.get;

import java.io.*;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.semantics.RDFService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class RDFApi {
  private static final Logger logger = LoggerFactory.getLogger(GraphqlApi.class);
  private static MolgenisSessionManager sessionManager;
  public static final String RDF_API_LOCATION = "/api/rdf";

  public static void create(MolgenisSessionManager sm) {
    // ideally, we estimate/calculate the content length and inform the client using
    // response.raw().setContentLengthLong(x) but since the output is streaming and the triples
    // created on-the-fly, there is no way of knowing (or is there?)
    sessionManager = sm;
    get(RDF_API_LOCATION, RDFApi::rdfForDatabase);
    get("/:schema" + RDF_API_LOCATION, RDFApi::rdfForSchema);
    get("/:schema" + RDF_API_LOCATION + "/:table", RDFApi::rdfForTable);
    get("/:schema" + RDF_API_LOCATION + "/:table/:row", RDFApi::rdfForRow);
    get("/:schema" + RDF_API_LOCATION + "/:table/column/:column", RDFApi::rdfForColumn);
  }

  private static int rdfForDatabase(Request request, Response response) throws IOException {
    Collection<String> schemaNames = MolgenisWebservice.getSchemaNames(request);
    String[] schemaNamesArr = schemaNames.toArray(new String[0]);
    Schema[] schemas = new Schema[schemaNames.size()];
    for (int i = 0; i < schemas.length; i++) {
      schemas[i] = (sessionManager.getSession(request).getDatabase().getSchema(schemaNamesArr[i]));
    }
    OutputStream outputStream = response.raw().getOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, null, null, null, schemas);
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForSchema(Request request, Response response) throws IOException {
    Schema schema = getSchemaByIdentifier(request);
    OutputStream outputStream = response.raw().getOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, null, null, null, schema);
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  @NotNull
  private static Schema getSchemaByIdentifier(final Request request) {
    String schemaIdentifier = request.params(SCHEMA);
    if (schemaIdentifier == null) {
      throw new MolgenisException(
          "Schema identifier is unexpectedly null for schema path " + request.params(SCHEMA));
    }
    Schema schema =
        sessionManager.getSession(request).getDatabase().getSchemaByIdentifier(schemaIdentifier);
    if (schema == null) {
      throw new MolgenisException(
          "Schema is unexpectedly null for schema "
              + schemaIdentifier
              + " ("
              + request.params(SCHEMA)
              + ")");
    }
    return schema;
  }

  private static int rdfForTable(Request request, Response response) throws IOException {
    Table table = getTableByIdentifier(request);
    OutputStream outputStream = response.raw().getOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, table, null, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForRow(Request request, Response response) throws IOException {
    Table table = getTableByIdentifier(request);
    String rowId = sanitize(request.params("row"));
    OutputStream outputStream = response.raw().getOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, table, rowId, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForColumn(Request request, Response response) throws IOException {
    Table table = getTableByIdentifier(request);
    String columnName = sanitize(request.params("column"));
    OutputStream outputStream = response.raw().getOutputStream();
    RDFService.describeAsRDF(
        outputStream,
        request,
        response,
        RDF_API_LOCATION,
        table,
        null,
        columnName,
        table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static Table getTableByIdentifier(final Request request) throws MolgenisException {
    Schema schema = getSchemaByIdentifier(request);
    String tableIdentifier = sanitize(request.params(TABLE));
    Table table = schema.getTableByIdentifier(tableIdentifier);
    if (table == null) throw new MolgenisException("Table " + tableIdentifier + " unknown");
    return table;
  }
}
