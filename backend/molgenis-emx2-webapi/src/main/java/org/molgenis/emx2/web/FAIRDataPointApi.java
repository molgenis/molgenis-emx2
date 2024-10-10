package org.molgenis.emx2.web;

import static io.javalin.apibuilder.ApiBuilder.*;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.fairdatapoint.*;

public class FAIRDataPointApi {

  private static MolgenisSessionManager sessionManager;
  private static final String TEXT_TURTLE_MIME_TYPE = "text/turtle";

  public static void create(Javalin app, MolgenisSessionManager sm) {
    sessionManager = sm;

    // Base routes for /api/fdp
    app.head("/api/fdp", FAIRDataPointApi::getHead);
    app.get("/api/fdp", FAIRDataPointApi::getFDP);
    app.head("/api/fdp/profile", FAIRDataPointApi::getHead);
    app.get("/api/fdp/profile", FAIRDataPointApi::getFDPProfile);

    // Routes for /api/fdp/catalog
    app.head("/api/fdp/catalog/{schema}/{id}", FAIRDataPointApi::getHead);
    app.get("/api/fdp/catalog/{schema}/{id}", FAIRDataPointApi::getCatalog);
    app.head("/api/fdp/catalog/profile", FAIRDataPointApi::getHead);
    app.get("/api/fdp/catalog/profile", FAIRDataPointApi::getCatalogProfile);

    // Routes for /api/fdp/dataset
    app.head("/api/fdp/dataset/{schema}/{id}", FAIRDataPointApi::getHead);
    app.get("/api/fdp/dataset/{schema}/{id}", FAIRDataPointApi::getDataset);
    app.head("/api/fdp/dataset/profile", FAIRDataPointApi::getHead);
    app.get("/api/fdp/dataset/profile", FAIRDataPointApi::getDatasetProfile);

    // Routes for /api/fdp/distribution
    app.head("/api/fdp/distribution/{schema}/{distribution}/{format}", FAIRDataPointApi::getHead);
    app.get(
        "/api/fdp/distribution/{schema}/{distribution}/{format}",
        FAIRDataPointApi::getDistribution);
    app.head("/api/fdp/distribution/profile", FAIRDataPointApi::getHead);
    app.get("/api/fdp/distribution/profile", FAIRDataPointApi::getDistributionProfile);
  }

  private static void getHead(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
  }

  private static void getFDP(Context ctx) throws Exception {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    Schema[] schemas = getSchemasHavingTable("Catalog", ctx);
    ctx.result(new FAIRDataPoint(ctx, schemas).getResult());
  }

  private static void getCatalog(Context ctx) throws Exception {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    Schema schema = getSchema(ctx);
    Table table = schema.getTable("Catalog");
    ctx.result(new FAIRDataPointCatalog(ctx, table).getResult());
  }

  private static void getDataset(Context ctx) throws Exception {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    Schema schema = getSchema(ctx);
    Table table = schema.getTable("Dataset");
    ctx.result(new FAIRDataPointDataset(ctx, table).getResult());
  }

  private static void getDistribution(Context ctx) throws Exception {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    ctx.result(
        new FAIRDataPointDistribution(ctx, sessionManager.getSession(ctx.req()).getDatabase())
            .getResult());
  }

  private static void getFDPProfile(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    ctx.result(FAIRDataPointProfile.FDP_SHACL);
  }

  private static void getCatalogProfile(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    ctx.result(FAIRDataPointProfile.CATALOG_SHACL);
  }

  private static void getDatasetProfile(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    ctx.result(FAIRDataPointProfile.DATASET_SHACL);
  }

  private static void getDistributionProfile(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    ctx.result(FAIRDataPointProfile.DISTRIBUTION_SHACL);
  }

  static Schema[] getSchemasHavingTable(String tableName, Context ctx) {
    List<Schema> schemas = new ArrayList<>();
    Collection<String> schemaNames = MolgenisWebservice.getSchemaNames(ctx);
    for (String sn : schemaNames) {
      Schema schema = sessionManager.getSession(ctx.req()).getDatabase().getSchema(sn);
      Table t = schema.getTable(tableName);
      if (t != null) {
        schemas.add(schema);
      }
    }
    Schema[] schemaArr = new Schema[schemas.size()];
    schemaArr = schemas.toArray(schemaArr);
    return schemaArr;
  }
}
