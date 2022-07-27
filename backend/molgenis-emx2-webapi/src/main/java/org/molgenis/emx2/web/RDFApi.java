package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.get;

import java.io.PrintWriter;
import java.io.StringWriter;
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
    sessionManager = sm;
    get(RDF_API_LOCATION, RDFApi::rdfForDatabase);
    get("/:schema" + RDF_API_LOCATION, RDFApi::rdfForSchema);
    get("/:schema" + RDF_API_LOCATION + "/:table", RDFApi::rdfForTable);
    get("/:schema" + RDF_API_LOCATION + "/:table/:row", RDFApi::rdfForRow);
  }

  // todo make streaming (also the other endpoints)
  private static String rdfForDatabase(Request request, Response response) {
    Collection<String> schemaNames = MolgenisWebservice.getSchemaNames(request);
    String[] schemaNamesArr = schemaNames.toArray(new String[schemaNames.size()]);
    Schema[] schemas = new Schema[schemaNames.size()];
    for (int i = 0; i < schemas.length; i++) {
      schemas[i] = (sessionManager.getSession(request).getDatabase().getSchema(schemaNamesArr[i]));
    }
    StringWriter sw = new StringWriter();
    RDFService.getRdfForSchema(new PrintWriter(sw), request, response, RDF_API_LOCATION, schemas);
    return sw.getBuffer().toString();
  }

  private static String rdfForSchema(Request request, Response response) {
    Schema schema = getSchema(request);
    StringWriter sw = new StringWriter();
    RDFService.getRdfForSchema(new PrintWriter(sw), request, response, RDF_API_LOCATION, schema);
    return sw.getBuffer().toString();
  }

  private static String rdfForTable(Request request, Response response) {
    Table table = getTable(request);
    StringWriter sw = new StringWriter();
    RDFService.getRdfForTable(
        table, null, new PrintWriter(sw), request, response, RDF_API_LOCATION);
    return sw.getBuffer().toString();
  }

  private static String rdfForRow(Request request, Response response) {
    Table table = getTable(request);
    String rowId = sanitize(request.params("row"));
    StringWriter sw = new StringWriter();
    RDFService.getRdfForTable(
        table, rowId, new PrintWriter(sw), request, response, RDF_API_LOCATION);
    return sw.getBuffer().toString();
  }
}
