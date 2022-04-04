package org.molgenis.emx2.web.service;

import static org.molgenis.emx2.SelectColumn.s;

import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogueSiteMap {
  private static final Logger logger = LoggerFactory.getLogger(CatalogueSiteMap.class);

  private static final String APP_NAME = "ssr-catalogue";
  private static final List<String> resourceTypes =
      Arrays.asList("Cohorts", "Studies", "Networks", "Institutions", "Databanks", "Datasources");

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

      resourceTypes.forEach(
          resource -> {
            List<String> pids = getResourcePids(schema, resource);
            pids.forEach(
                pid -> {
                  try {
                    wsg.addUrl(urlForResource(resourceBasePath, resource, pid));
                  } catch (MalformedURLException e) {
                    logger.error(
                        "Failed to generate sitemap url (schema: ("
                            + schema.getName()
                            + " , resource: "
                            + resource
                            + " , pid: "
                            + pid,
                        e);
                  }
                });
          });

      return String.join(System.lineSeparator(), wsg.writeAsStrings());
    } catch (MalformedURLException e) {
      String errorDescription = "Error initializing WebSitemapGenerator";
      logger.error(errorDescription, e);
      throw new MolgenisException(errorDescription);
    }
  }

  private List<String> getResourcePids(Schema schema, String resourceName) {
    Table table = schema.getTable(resourceName);
    if (table == null) {
      return Collections.emptyList();
    }
    List<Row> rows = table.select(s("pid")).retrieveRows();
    return rows.stream().map(row -> row.getString("pid")).toList();
  }

  private WebSitemapUrl urlForResource(
      String resourceBasePath, String resourceName, String resourceId)
      throws MalformedURLException {
    return new WebSitemapUrl.Options(
            resourceBasePath + "/" + resourceName + "/" + resourceId)
        .build();
  }
}
