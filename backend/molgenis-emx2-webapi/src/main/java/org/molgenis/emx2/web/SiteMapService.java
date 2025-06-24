package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.web.service.CatalogueSiteMap;

public class SiteMapService {

  private SiteMapService() {
    // hide constructor
  }

  public static void create(Javalin app) {
    app.get("/{schema}/sitemap.xml", SiteMapService::getSiteMapForSchema);
  }

  public static void getSiteMapForSchema(Context ctx) {
    ctx.res().setContentType("application/xml");
    Schema schema = getSchema(ctx);
    if (schema == null) {
      ctx.status(404);
      ctx.result("Schema not found");
      return;
    }

    final String baseUrl = "https://" + ctx.host();

    String siteMap = new CatalogueSiteMap(schema, baseUrl).buildSiteMap();
    ctx.result(siteMap);
  }
}
