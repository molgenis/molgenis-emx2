package org.molgenis.emx2.web.service;

import static org.molgenis.emx2.SelectColumn.s;

import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogueSiteMap {
  private static final Logger logger = LoggerFactory.getLogger(CatalogueSiteMap.class);

  private static final String APP_NAME = "catalogue";
  private static final String TYPE_NETWORK = "Network";

  private enum ResourcePath {
    networks,
    collections,
  }

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
      Table resourceTable = schema.getTable("Resources");
      resourceTable
          .select(s("id"), s("type"))
          .retrieveRows()
          .forEach(
              resource -> {
                String collectionId = resource.getString("id");
                ResourcePath resourcePath = getResourcePath(resource);
                try {
                  wsg.addUrl(urlForResource(resourceBasePath, resourcePath, collectionId));
                } catch (MalformedURLException e) {
                  logger.error(
                      "Failed to generate sitemap url (schema: ({} , path: {} , id: {}",
                      schema.getName(),
                      resourcePath.name(),
                      collectionId,
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

  private ResourcePath getResourcePath(Row resource) {
    List<String> types = Arrays.asList(resource.getStringArray("type", false));
    // no switch bool in java 21
    if (types.contains(TYPE_NETWORK)) {
      return ResourcePath.networks;
    } else {
      return ResourcePath.collections;
    }
  }

  private WebSitemapUrl urlForResource(
      String resourceBasePath, ResourcePath resourcePath, String resourceId)
      throws MalformedURLException {
    return new WebSitemapUrl.Options(
            resourceBasePath + "/all/" + resourcePath.name() + "/" + resourceId)
        .build();
  }
}
