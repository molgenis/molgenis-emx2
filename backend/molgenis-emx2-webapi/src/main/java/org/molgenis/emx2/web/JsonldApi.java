package org.molgenis.emx2.web;

import static org.molgenis.emx2.jsonld.RestOverGraphql.getAllAsJsonLd;
import static org.molgenis.emx2.jsonld.RestOverGraphql.getAllAsTurtle;
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
    app.get("/{schema}/api/ttl2/", JsonldApi::getDocs);
    app.get("/{schema}/api/ttl2/_all", JsonldApi::getTtlForSchema);
    app.get("/{schema}/api/ttl2/_context", JsonldApi::getJsonLdContextSchema);
    app.get("/{schema}/api/ttl2/_schema", JsonldApi::getJsonLdContextSchema);
    app.get("/{schema}/api/ttl2/_json", JsonldApi::getJsonLdForSchema);
  }

  private static void getJsonLdContextSchema(Context ctx) {
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    GraphqlApi graphqlForSchema = applicationCache.getSchemaGraphqlForUser(schemaName, ctx);
    ctx.header("Content-Type", "text/ld+json");
    ctx.result(graphqlForSchema.getJsonLdSchema(ctx.url()));
  }

  private static void getDocs(Context ctx) {
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    GraphqlApi graphqlForSchema = applicationCache.getSchemaGraphqlForUser(schemaName, ctx);
    String selectAllQuery = graphqlForSchema.getSelectAllQuery();

    ctx.header("Content-Type", "text/html");
    ctx.result(
        """
            <p>
            Welcome to new ttl2 API.

            You can retrieve all as turtle via
            <a href="/%s/api/ttl2/_all">_all</a>
            </p>

            <p>
            You can also pass a graphql query if don't want all data. This using normal graphql, our you can use ...framgents. For example:
            <a href="/%s/api/ttl2/_all?query=%s">_all?query=%s</a>
            </p>
            <p>
            N.B. don't forget to include 'mg_id' fields in your query if you want short identifier. E.g. {Pet{mg_id,name}}
            </p>
            <p>You can also view the jsonld @context schema that is used to convert graphql to rdf/ttl:
            <a href="/%s/api/ttl2/_context">_context</a>
            </p>
            """
            .formatted(schemaName, schemaName, selectAllQuery, selectAllQuery, schemaName));
  }

  private static void getJsonLdForSchema(Context ctx) {
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    GraphqlApi graphqlForSchema = applicationCache.getSchemaGraphqlForUser(schemaName, ctx);
    String result = getAllAsJsonLd(graphqlForSchema, ctx.url(), ctx.queryParam("query"));
    ctx.header("Content-Type", "text/ld+json");
    ctx.result(result);
  }

  private static void getTtlForSchema(Context ctx) {
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    GraphqlApi graphqlForSchema = applicationCache.getSchemaGraphqlForUser(schemaName, ctx);
    String result = getAllAsTurtle(graphqlForSchema, ctx.url(), ctx.queryParam("query"));
    ctx.header("Content-Type", "text/turtle");
    ctx.result(result);
  }
}
