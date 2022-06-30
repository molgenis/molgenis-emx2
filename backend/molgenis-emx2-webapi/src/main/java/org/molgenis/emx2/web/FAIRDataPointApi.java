package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.BeaconApi.getTableFromAllSchemas;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.get;

import java.util.List;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.fairdatapoint.FAIRDataPoint;
import org.molgenis.emx2.fairdatapoint.FAIRDataPointCatalog;
import org.molgenis.emx2.fairdatapoint.FAIRDataPointDistribution;
import org.molgenis.emx2.fairdatapoint.FAIRDataPointProfile;
import spark.Request;
import spark.Response;

// is a beacon on level of database, schema or table?
public class FAIRDataPointApi {

  private static MolgenisSessionManager sessionManager;

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;
    get("/api/fdp", FAIRDataPointApi::getFDP);
    get("/api/fdp/profile", FAIRDataPointApi::getFDPProfile);
    get("/api/fdp/catalog/profile", FAIRDataPointApi::getCatalogProfile);
    get("/api/fdp/catalog/:schema/:id", FAIRDataPointApi::getCatalog);

    get("/api/fdp/distribution/:schema/:table/:format", FAIRDataPointApi::getDistribution);
  }

  private static String getFDP(Request request, Response res) throws Exception {
    List<Table> tables = getTableFromAllSchemas("FDP_Catalog", request);
    return new FAIRDataPoint(request, tables).getResult();
  }

  private static String getFDPProfile(Request request, Response res) throws Exception {
    return FAIRDataPointProfile.FDP_SHACL;
  }

  private static String getCatalogProfile(Request request, Response res) throws Exception {
    return FAIRDataPointProfile.CATALOG_SHACL;
  }

  private static String getCatalog(Request request, Response res) throws Exception {
    Schema schema = getSchema(request);
    Table table = schema.getTable("FDP_Catalog");
    return new FAIRDataPointCatalog(request, table).getResult();
  }

  private static String getDistribution(Request request, Response res) throws Exception {
    return new FAIRDataPointDistribution(request, sessionManager.getSession(request).getDatabase())
        .getResult();
  }
}
