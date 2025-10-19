package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static org.molgenis.emx2.web.MolgenisWebservice.SCHEMA;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.molgenis.emx2.graphql.GraphqlApi;

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
    GraphqlApi graphqlForSchema = applicationCache.getSchemaGraphqlForUser(schemaName, ctx);
  }
}
