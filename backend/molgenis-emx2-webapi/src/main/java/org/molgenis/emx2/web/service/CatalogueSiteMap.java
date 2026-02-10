package org.molgenis.emx2.web.service;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.web.util.EncodingHelpers.encodePathSegment;

import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import java.net.MalformedURLException;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogueSiteMap {
  private static final Logger logger = LoggerFactory.getLogger(CatalogueSiteMap.class);

  private static final String TYPE_NETWORK = "Network";
  private static final String RESOURCE = "resource";

  private final Schema schema;
  private final String baseUrl;

  public CatalogueSiteMap(Schema schema, String baseUrl) {
    this.schema = schema;
    this.baseUrl = baseUrl;
  }

  public String buildSiteMap() {
    try {
      WebSitemapGenerator wsg = new WebSitemapGenerator(baseUrl);
      Table resourceTable = schema.getTable("Resources");
      if (resourceTable == null) {
        throw new MolgenisException(
            "Expected table 'Resources' not found in schema: %s".formatted(schema.getName()));
      }
      resourceTable
          .select(s("id"))
          .retrieveRows()
          .forEach(
              resource -> {
                String resourceId = resource.getString("id");
                try {
                  wsg.addUrl(urlForResource(baseUrl, resourceId));
                } catch (MalformedURLException e) {
                  logger.error(
                      "Failed to generate sitemap url (schema: {} , id: {}",
                      schema.getName(),
                      resourceId,
                      e);
                }
              });

      if (schema.getTable("Variables") != null) {
        schema
            .query("Variables")
            .select(s("name"), s(RESOURCE), s("dataset"))
            .where(f(RESOURCE, f("type", f("name", Operator.EQUALS, TYPE_NETWORK))))
            .retrieveRows()
            .forEach(
                variable -> {
                  try {
                    wsg.addUrl(urlForVariable(variable));
                  } catch (MalformedURLException e) {
                    logger.error(
                        "Failed to generate sitemap url (schema: ({} , path: {} , id: {}",
                        schema.getName(),
                        "variables",
                        variable.getString("name"),
                        e);
                  }
                });
      }

      return String.join(System.lineSeparator(), wsg.writeAsStrings());
    } catch (MalformedURLException e) {
      String errorDescription = "Error initializing WebSitemapGenerator";
      logger.error(errorDescription, e);
      throw new MolgenisException(errorDescription);
    }
  }

  private WebSitemapUrl urlForResource(String resourceBasePath, String resourceId)
      throws MalformedURLException {
    return new WebSitemapUrl.Options(
            "%s/%s".formatted(resourceBasePath, encodePathSegment(resourceId)))
        .build();
  }

  private WebSitemapUrl urlForVariable(Row variable) throws MalformedURLException {
    String variableId = variable.getString("name");
    String resource = variable.getString(RESOURCE);
    String dataset = variable.getString("dataset");

    return new WebSitemapUrl.Options(
            "%s/%s/datasets/%s/%s"
                .formatted(
                    baseUrl,
                    encodePathSegment(resource),
                    encodePathSegment(dataset),
                    encodePathSegment(variableId)))
        .build();
  }
}
