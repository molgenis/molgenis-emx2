package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.get;
import static spark.Spark.head;
import static spark.Spark.path;
import static spark.Spark.redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.fairdatapoint.*;
import spark.Request;
import spark.Response;

public class FAIRDataPointApi {

  private static MolgenisSessionManager sessionManager;
  private static final String TEXT_TURTLE_MIME_TYPE = "text/turtle";

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;

    redirect.any("/api/fdp/", "/api/fdp");
    path(
        "/api/fdp",
        () -> {
          head("", FAIRDataPointApi::getHead);
          get("", FAIRDataPointApi::getFDP);
          head("/profile", FAIRDataPointApi::getHead);
          get("/profile", FAIRDataPointApi::getFDPProfile);

          path(
              "/catalog/",
              () -> {
                head(":schema/:id", FAIRDataPointApi::getHead);
                get(":schema/:id", FAIRDataPointApi::getCatalog);
                head("profile", FAIRDataPointApi::getHead);
                get("profile", FAIRDataPointApi::getCatalogProfile);
              });
          path(
              "/dataset/",
              () -> {
                head(":schema/:id", FAIRDataPointApi::getHead);
                get(":schema/:id", FAIRDataPointApi::getDataset);
                head("profile", FAIRDataPointApi::getHead);
                get("profile", FAIRDataPointApi::getDatasetProfile);
              });
          path(
              "/distribution/",
              () -> {
                head(":schema/:distribution/:format", FAIRDataPointApi::getHead);
                get(":schema/:distribution/:format", FAIRDataPointApi::getDistribution);
                head("profile", FAIRDataPointApi::getHead);
                get("profile", FAIRDataPointApi::getDistributionProfile);
              });
        });
  }

  /**
   * Response to a HTTP HEAD request with the minimal data and correct content type.
   *
   * @param request The request
   * @param response The response
   * @return Always returns an empty body per specification.
   */
  private static String getHead(Request request, Response response) {
    response.type(TEXT_TURTLE_MIME_TYPE);
    return "";
  }

  private static String getFDP(Request request, Response res) throws Exception {
    res.type(TEXT_TURTLE_MIME_TYPE);
    Schema[] schemas = getSchemasHavingTable("Catalog", request);
    return new FAIRDataPoint(request, schemas).getResult();
  }

  private static String getCatalog(Request request, Response res) throws Exception {
    res.type(TEXT_TURTLE_MIME_TYPE);
    Schema schema = getSchema(request);
    Table table = schema.getTable("Catalog");
    return new FAIRDataPointCatalog(request, table).getResult();
  }

  private static String getDataset(Request request, Response res) throws Exception {
    res.type(TEXT_TURTLE_MIME_TYPE);
    Schema schema = getSchema(request);
    Table table = schema.getTable("Dataset");
    return new FAIRDataPointDataset(request, table).getResult();
  }

  private static String getDistribution(Request request, Response res) throws Exception {
    res.type(TEXT_TURTLE_MIME_TYPE);
    return new FAIRDataPointDistribution(request, sessionManager.getSession(request).getDatabase())
        .getResult();
  }

  private static String getFDPProfile(Request request, Response res) {
    res.type(TEXT_TURTLE_MIME_TYPE);
    return FAIRDataPointProfile.FDP_SHACL;
  }

  private static String getCatalogProfile(Request request, Response res) {
    res.type(TEXT_TURTLE_MIME_TYPE);
    return FAIRDataPointProfile.CATALOG_SHACL;
  }

  private static String getDatasetProfile(Request request, Response res) {
    res.type(TEXT_TURTLE_MIME_TYPE);
    return FAIRDataPointProfile.DATASET_SHACL;
  }

  private static String getDistributionProfile(Request request, Response res) {
    res.type(TEXT_TURTLE_MIME_TYPE);
    return FAIRDataPointProfile.DISTRIBUTION_SHACL;
  }

  static Schema[] getSchemasHavingTable(String tableName, Request request) {
    List<Schema> schemas = new ArrayList<>();
    Collection<String> schemaNames = MolgenisWebservice.getSchemaNames(request);
    for (String sn : schemaNames) {
      Schema schema = sessionManager.getSession(request).getDatabase().getSchema(sn);
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
