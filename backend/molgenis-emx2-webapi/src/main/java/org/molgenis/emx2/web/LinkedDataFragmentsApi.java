package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.getTable;
import static spark.Spark.get;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.semantics.LinkedDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class LinkedDataFragmentsApi {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);
  private static MolgenisSessionManager sessionManager;

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;
    get("/:schema/api/jsonld", LinkedDataFragmentsApi::jsonld);
    get("/:schema/api/ttl", LinkedDataFragmentsApi::ttl);
    get("/:schema/api/jsonld/:table", LinkedDataFragmentsApi::jsonldTable);
    get("/:schema/api/ttl/:table", LinkedDataFragmentsApi::ttlTable);
  }

  private static String jsonldTable(Request request, Response response) {
    Table table = getTable(request);
    StringWriter sw = new StringWriter();
    LinkedDataService.getJsonLdForTable(table, new PrintWriter(sw));
    return sw.getBuffer().toString();
  }

  private static String jsonld(Request request, Response response) {
    Schema schema = getSchema(request);
    StringWriter sw = new StringWriter();
    LinkedDataService.getJsonLdForSchema(schema, new PrintWriter(sw));
    return sw.getBuffer().toString();
  }

  private static String ttl(Request request, Response response) {
    Schema schema = getSchema(request);
    StringWriter sw = new StringWriter();
    LinkedDataService.getTtlForSchema(schema, new PrintWriter(sw));
    return sw.getBuffer().toString();
  }

  private static String ttlTable(Request request, Response response) {
    Table table = getTable(request);
    StringWriter sw = new StringWriter();
    LinkedDataService.getTtlForTable(table, new PrintWriter(sw));
    return sw.getBuffer().toString();
  }

  // should deliver resource on resource URI
  // should deliver a complete dump
  // future: deliver fragements filtered on subject, predicate, object

}
