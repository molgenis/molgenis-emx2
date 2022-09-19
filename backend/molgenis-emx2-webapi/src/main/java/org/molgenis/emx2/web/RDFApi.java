package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.get;

import java.io.*;
import java.util.Collection;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.semantics.RDFService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class RDFApi {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);
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
  }

  private static int rdfForDatabase(Request request, Response response) throws IOException {
    Collection<String> schemaNames = MolgenisWebservice.getSchemaNames(request);
    String[] schemaNamesArr = schemaNames.toArray(new String[schemaNames.size()]);
    Schema[] schemas = new Schema[schemaNames.size()];
    for (int i = 0; i < schemas.length; i++) {
      schemas[i] = (sessionManager.getSession(request).getDatabase().getSchema(schemaNamesArr[i]));
    }
    OutputStream outputStream = response.raw().getOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, null, null, schemas);
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForSchema(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    OutputStream outputStream = response.raw().getOutputStream();
    RDFService.describeAsRDF(outputStream, request, response, RDF_API_LOCATION, null, null, schema);
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForTable(Request request, Response response) throws IOException {
    Table table = getTable(request);
    OutputStream outputStream = response.raw().getOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, table, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForRow(Request request, Response response) throws IOException {
    Table table = getTable(request);
    String rowId = sanitize(request.params("row"));
    OutputStream outputStream = response.raw().getOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, table, rowId, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }
}
