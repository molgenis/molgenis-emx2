package org.molgenis.emx2.web.controllers;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.web.service.CatalogueSiteMap;
import spark.Request;
import spark.Response;

public class SiteMapController {

  private SiteMapController() {}

  public static String getSiteMapForSchema(Request request, Response response) {
    response.type("text/xml, application/xml");
    Schema schema = getSchema(request);

    final String baseUrl = request.scheme() + "://" + request.host() + "/" + schema.getName();

    return new CatalogueSiteMap(schema, baseUrl).buildSiteMap();
  }
}
