package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static org.molgenis.emx2.web.MolgenisWebservice.SCHEMA;

import graphql.GraphQL;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class JsonldApi {
  private JsonldApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    final String schemaPath = "/{schema}/jsonld";
    app.get(schemaPath, JsonldApi::getJsonLdForSchema);
  }

  private static void getJsonLdForSchema(Context ctx) {
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    GraphQL graphqlForSchema = applicationCache.getSchemaGraphqlForUser(schemaName, ctx);
  }
}
