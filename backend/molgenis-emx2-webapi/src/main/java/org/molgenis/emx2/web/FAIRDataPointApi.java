package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.BeaconApi.getSchemasHavingTable;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.get;

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

    // the four FDP layers
    get("/api/fdp", FAIRDataPointApi::getFDP);
    get(
        "/api/fdp/",
        FAIRDataPointApi::getFDP); // todo ideally, pass this to get("/api/fdp",..), but how?
    get("/api/fdp/catalog/:schema/:id", FAIRDataPointApi::getCatalog);
    get("/api/fdp/dataset/:schema/:id", FAIRDataPointApi::getDataset);
    get("/api/fdp/distribution/:schema/:distribution/:format", FAIRDataPointApi::getDistribution);

    // corresponding profiles
    get("/api/fdp/profile", FAIRDataPointApi::getFDPProfile);
    get("/api/fdp/catalog/profile", FAIRDataPointApi::getCatalogProfile);
    get("/api/fdp/dataset/profile", FAIRDataPointApi::getDatasetProfile);
    get("/api/fdp/distribution/profile", FAIRDataPointApi::getDistributionProfile);
  }

  private static String getFDP(Request request, Response res) throws Exception {
    res.type(TEXT_TURTLE_MIME_TYPE);
    Schema[] schemas = getSchemasHavingTable("FDP_Catalog", request);
    return new FAIRDataPoint(request, schemas).getResult();
  }

  private static String getCatalog(Request request, Response res) throws Exception {
    res.type(TEXT_TURTLE_MIME_TYPE);
    Schema schema = getSchema(request);
    Table table = schema.getTable("FDP_Catalog");
    return new FAIRDataPointCatalog(request, table).getResult();
  }

  private static String getDataset(Request request, Response res) throws Exception {
    res.type(TEXT_TURTLE_MIME_TYPE);
    Schema schema = getSchema(request);
    Table table = schema.getTable("FDP_Dataset");
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
}
