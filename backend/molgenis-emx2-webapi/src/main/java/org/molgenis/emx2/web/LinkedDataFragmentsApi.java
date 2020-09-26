package org.molgenis.emx2.web;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.linkeddata.LinkedDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.*;

public class LinkedDataFragmentsApi {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);
  private static MolgenisSessionManager sessionManager;

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;

    final String schemaPath = "/api/ld/:schema";
    get(schemaPath, LinkedDataFragmentsApi::dump);
  }

  private static String dump(Request request, Response response) {
    Schema schema = getSchema(request);
    StringWriter sw = new StringWriter();
    LinkedDataService.dump(schema, new PrintWriter(sw));
    return sw.getBuffer().toString();
  }

  // should deliver resource on resource URI
  // should deliver a complete dump
  // future: deliver fragements filtered on subject, predicate, object

}
