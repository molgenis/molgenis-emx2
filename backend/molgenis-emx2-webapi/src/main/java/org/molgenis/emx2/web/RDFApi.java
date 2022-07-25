package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.getTable;
import static spark.Spark.get;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.molgenis.emx2.Database;
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

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;
    get("/:schema/api/rdf", RDFApi::rdfForSchema);
    get("/:schema/api/rdf/:table", RDFApi::rdfForTable);
    get("/api/rdfdump", RDFApi::rdfDump);
  }

  private static String rdfForSchema(Request request, Response response) {
    Schema schema = getSchema(request);
    StringWriter sw = new StringWriter();
    RDFService.getRdfForSchema(schema, new PrintWriter(sw), request, response);
    return sw.getBuffer().toString();
  }

  private static String rdfForTable(Request request, Response response) {
    Table table = getTable(request);
    StringWriter sw = new StringWriter();
    RDFService.getRdfForTable(table, new PrintWriter(sw), request, response);
    return sw.getBuffer().toString();
  }

  // todo make streaming (also the other endpoints)
  private static String rdfDump(Request request, Response response) {
    Database database = getSchema(request).getDatabase();
    StringWriter sw = new StringWriter();
    RDFService.getRdfDatabaseDump(database, new PrintWriter(sw), request, response);
    return sw.getBuffer().toString();
  }
}
