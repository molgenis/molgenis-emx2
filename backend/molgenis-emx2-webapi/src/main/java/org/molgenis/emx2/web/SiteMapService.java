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

    String catalogueSiteMapLocation = schema.getSettingValue(Constants.CATALOGUE_SITE_MAP_LOCATION);
    String host = catalogueSiteMapLocation != null ? catalogueSiteMapLocation : ctx.host();

    final String baseUrl = "https://" + host + "/" + schema.getName();

    String siteMap = new CatalogueSiteMap(schema, baseUrl).buildSiteMap();
    ctx.result(siteMap);
  }
}
