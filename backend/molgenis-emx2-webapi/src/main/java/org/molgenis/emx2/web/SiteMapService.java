package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.get;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.web.service.CatalogueSiteMap;
import spark.Request;
import spark.Response;

public class SiteMapService {

  private SiteMapService() {
    // hide constructor
  }

  public static void create() {
    get("/:schema/sitemap.xml", SiteMapService::getSiteMapForSchema);
  }

  public static String getSiteMapForSchema(Request request, Response response) {
    response.type("text/xml, application/xml");
    Schema schema = getSchema(request);

    final String baseUrl = request.scheme() + "://" + request.host() + "/" + schema.getName();

    return new CatalogueSiteMap(schema, baseUrl).buildSiteMap();
  }
}
