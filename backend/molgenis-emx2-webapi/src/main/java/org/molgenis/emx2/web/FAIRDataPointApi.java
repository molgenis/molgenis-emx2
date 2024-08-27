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

    //    app.redirect.any("/api/fdp/", "/api/fdp");
    //    path(
    //        "/api/fdp",
    //        () -> {
    //          head("", FAIRDataPointApi::getHead);
    //          get("", FAIRDataPointApi::getFDP);
    //          head("/profile", FAIRDataPointApi::getHead);
    //          get("/profile", FAIRDataPointApi::getFDPProfile);
    //
    //          path(
    //              "/catalog/",
    //              () -> {
    //                head("{schema}/{id}", FAIRDataPointApi::getHead);
    //                get("{schema}/{id}", FAIRDataPointApi::getCatalog);
    //                head("profile", FAIRDataPointApi::getHead);
    //                get("profile", FAIRDataPointApi::getCatalogProfile);
    //              });
    //          path(
    //              "/dataset/",
    //              () -> {
    //                head("{schema}/{id}", FAIRDataPointApi::getHead);
    //                get("{schema}/{id}", FAIRDataPointApi::getDataset);
    //                head("profile", FAIRDataPointApi::getHead);
    //                get("profile", FAIRDataPointApi::getDatasetProfile);
    //              });
    //          path(
    //              "/distribution/",
    //              () -> {
    //                head("{schema}/{distribution}/{format}", FAIRDataPointApi::getHead);
    //                get("{schema}/{distribution}/{format}", FAIRDataPointApi::getDistribution);
    //                head("profile", FAIRDataPointApi::getHead);
    //                get("profile", FAIRDataPointApi::getDistributionProfile);
    //              });
    //        });
  }

  private static String getHead(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    return "";
  }

  private static String getFDP(Context ctx) throws Exception {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    Schema[] schemas = getSchemasHavingTable("Catalog", ctx);
    return new FAIRDataPoint(ctx, schemas).getResult();
  }

  private static String getCatalog(Context ctx) throws Exception {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    Schema schema = getSchema(ctx);
    Table table = schema.getTable("Catalog");
    return new FAIRDataPointCatalog(ctx, table).getResult();
  }

  private static String getDataset(Context ctx) throws Exception {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    Schema schema = getSchema(ctx);
    Table table = schema.getTable("Dataset");
    return new FAIRDataPointDataset(ctx, table).getResult();
  }

  private static String getDistribution(Context ctx) throws Exception {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    return new FAIRDataPointDistribution(ctx, sessionManager.getSession(ctx.req()).getDatabase())
        .getResult();
  }

  private static String getFDPProfile(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    return FAIRDataPointProfile.FDP_SHACL;
  }

  private static String getCatalogProfile(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    return FAIRDataPointProfile.CATALOG_SHACL;
  }

  private static String getDatasetProfile(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    return FAIRDataPointProfile.DATASET_SHACL;
  }

  private static String getDistributionProfile(Context ctx) {
    ctx.contentType(TEXT_TURTLE_MIME_TYPE);
    return FAIRDataPointProfile.DISTRIBUTION_SHACL;
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
