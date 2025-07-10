package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.Templates;
import org.molgenis.emx2.beaconv2.endpoints.*;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.sql.SqlDatabase;

public class BeaconApi {

  private static MolgenisSessionManager sessionManager;

  public static void create(Javalin app, MolgenisSessionManager sm) {
    sessionManager = sm;
    defineRoutes(app, "/{schema}/api/beacon");
    defineRoutes(app, "/{schema}/api/beacon_vp");
    defineRoutes(app, "/api/beacon");
    defineRoutes(app, "/api/beacon_vp");
    createTemplates();
  }

  private static void createTemplates() {
    Database database = new SqlDatabase(SqlDatabase.ADMIN_USER);
    Templates.addTemplatesToDb(database);
  }

  private static void defineRoutes(Javalin app, String basePath) {
    app.before(basePath + "/*", BeaconApi::beforeRequest);

    app.get(basePath, BeaconApi::getInfo);
    app.get(basePath + "/", BeaconApi::getInfo);
    app.get(basePath + "/info", BeaconApi::getInfo);
    app.get(basePath + "/service-info", BeaconApi::getInfo);
    app.get(basePath + "/configuration", BeaconApi::getConfiguration);
    app.get(basePath + "/map", BeaconApi::getMap);
    app.get(basePath + "/entry_types", BeaconApi::getEntryTypes);
    app.get(basePath + "/filtering_terms", BeaconApi::getFilteringTerms);
    app.get(basePath + "/{entry_type}", BeaconApi::getEntryType);
    app.get(basePath + "/{entry_type}/{id}", BeaconApi::getEntryType);
    app.get(basePath + "/{entry_type_id}/{id}/{entry_type}", BeaconApi::getEntryType);
    app.post(basePath + "/{entry_type}", BeaconApi::postEntryType);
  }

  private static void beforeRequest(Context ctx) {
    extractSpecification(ctx);
    ctx.contentType(Constants.ACCEPT_JSON);
  }

  private static void extractSpecification(Context ctx) {
    String specification = ctx.matchedPath().split("/api/")[1].split("/")[0];
    ctx.attribute("specification", specification);
  }

  private static void getInfo(Context ctx) {
    ctx.contentType(Constants.ACCEPT_JSON);
    Schema schema = getSchema(ctx);

    Database database = sessionManager.getSession(ctx.req()).getDatabase();
    ctx.json(new Info(database).getResponse(schema));
  }

  private static void getConfiguration(Context ctx) {
    ctx.json(new Configuration().getResponse(ctx));
  }

  private static void getMap(Context ctx) {
    ctx.json(new Map().getResponse(ctx));
  }

  private static void getEntryTypes(Context ctx) {
    ctx.json(new EntryTypes().getResponse(ctx));
  }

  private static void getFilteringTerms(Context ctx) {
    Database database = sessionManager.getSession(ctx.req()).getDatabase();
    ctx.json(new FilteringTerms(database));
  }

  private static void getEntryType(Context ctx) {
    entryTypeRequest(ctx, new BeaconRequestBody(ctx));
  }

  private static void postEntryType(Context ctx) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    BeaconRequestBody beaconRequest = mapper.readValue(ctx.body(), BeaconRequestBody.class);
    beaconRequest.addRequestParameters(ctx);
    entryTypeRequest(ctx, beaconRequest);
  }

  private static void entryTypeRequest(Context ctx, BeaconRequestBody requestBody) {
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);

    Schema schema = getSchema(ctx);
    if (schema != null) {
      ctx.json(queryEntryType.query(schema));
    } else {
      Database database = sessionManager.getSession(ctx.req()).getDatabase();
      ctx.json(queryEntryType.query(database));
    }
  }
}
