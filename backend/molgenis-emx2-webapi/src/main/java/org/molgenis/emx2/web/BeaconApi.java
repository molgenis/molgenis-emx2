package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.endpoints.*;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;

public class BeaconApi {

  private static MolgenisSessionManager sessionManager;

  public static void create(Javalin app, MolgenisSessionManager sm) {
    sessionManager = sm;
    defineRoutes(app, "/{schema}/api/beacon");
    defineRoutes(app, "/{schema}/api/beacon_vp");
    defineRoutes(app, "/api/beacon");
    defineRoutes(app, "/api/beacon_vp");
  }

  private static void defineRoutes(Javalin app, String basePath) {
    app.before(basePath + "/*", BeaconApi::processRequest);

    app.get(basePath, BeaconApi::getInfo);
    app.get(basePath + "/", BeaconApi::getInfo);
    app.get(basePath + "/info", BeaconApi::getInfo);
    app.get(basePath + "/service-info", BeaconApi::getInfo);
    app.get(basePath + "/configuration", new Configuration()::getResponse);
    app.get(basePath + "/map", new Map()::getResponse);
    app.get(basePath + "/entry_types", new EntryTypes()::getResponse);
    app.get(basePath + "/filtering_terms", BeaconApi::getFilteringTerms);
    app.get(basePath + "/{entry_type}", BeaconApi::getEntryType);
    app.get(basePath + "/{entry_type}/{id}", BeaconApi::getEntryType);
    app.get(basePath + "/{entry_type_id}/{id}/{entry_type}", BeaconApi::getEntryType);
    app.post(basePath + "/{entry_type}", BeaconApi::postEntryType);
  }

  private static void processRequest(Context ctx) {
    extractSpecification(ctx);
    ctx.contentType(Constants.ACCEPT_JSON);
  }

  private static void extractSpecification(Context ctx) {
    String specification = ctx.matchedPath().split("/api/")[1].split("/")[0];
    ctx.attribute("specification", specification);
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

  private static void getFilteringTerms(Context ctx) {
    Database database = sessionManager.getSession(ctx.req()).getDatabase();
    ctx.json(new FilteringTerms(database));
  }

  private static void getInfo(Context ctx) {
    ctx.contentType(Constants.ACCEPT_JSON);
    Schema schema = getSchema(ctx);

    Database database = sessionManager.getSession(ctx.req()).getDatabase();
    ctx.json(new Info(database).getResponse(schema));
  }
}
