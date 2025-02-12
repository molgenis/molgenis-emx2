package org.molgenis.emx2.web.service;

import static org.molgenis.emx2.SelectColumn.s;

import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import java.net.MalformedURLException;
import java.util.Map;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogueSiteMap {
  private static final Logger logger = LoggerFactory.getLogger(CatalogueSiteMap.class);

  private static final String APP_NAME = "catalogue";
  private static final Map<String, String> resourceTypes =
      Map.of(
          "Cohort study",
          "cohorts",
          "Study",
          "studies",
          "Network",
          "networks",
          "Databank",
          "databanks",
          "Data source",
          "datasources");

  private final Schema schema;
  private final String baseUrl;
  private final String resourceBasePath;

  public CatalogueSiteMap(Schema schema, String baseUrl) {
    this.schema = schema;
    this.baseUrl = baseUrl;

    this.resourceBasePath = baseUrl + '/' + APP_NAME;
  }

  public String buildSiteMap() {
    try {
      WebSitemapGenerator wsg = new WebSitemapGenerator(baseUrl);
      Table collectionsTables = schema.getTable("Collections");
      collectionsTables
          .select(s("id"), s("type"))
          .retrieveRows()
          .forEach(
              collection -> {
                String collectionPath = resourceTypes.get(collection.getString("type"));
                if (collectionPath == null) {
                  collectionPath = "collections";
                }
                String collectionId = collection.getString("id");
                try {
                  wsg.addUrl(urlForResource(resourceBasePath, collectionPath, collectionId));
                } catch (MalformedURLException e) {
                  logger.error(
                      "Failed to generate sitemap url (schema: ("
                          + schema.getName()
                          + " , path: "
                          + collectionPath
                          + " , id: "
                          + collectionId,
                      e);
                }
              });
      return String.join(System.lineSeparator(), wsg.writeAsStrings());
    } catch (MalformedURLException e) {
      String errorDescription = "Error initializing WebSitemapGenerator";
      logger.error(errorDescription, e);
      throw new MolgenisException(errorDescription);
    }
  }

  private WebSitemapUrl urlForResource(
      String resourceBasePath, String resourceName, String resourceId)
      throws MalformedURLException {
    return new WebSitemapUrl.Options(resourceBasePath + "/all/" + resourceName + "/" + resourceId)
        .build();
  }
}
