package org.molgenis.emx2.web.service;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogueSiteMap {
  private static final Logger logger = LoggerFactory.getLogger(CatalogueSiteMap.class);

  private static final String APP_NAME = "catalogue";
  private static final String TYPE_NETWORK = "Network";
  private static final String RESOURCE = "resource";

  private enum ResourcePath {
    networks,
    collections,
  }

  private final Schema schema;
  private final String baseUrl;
  private final String basePath;

  public CatalogueSiteMap(Schema schema, String baseUrl) {
    this.schema = schema;
    this.baseUrl = baseUrl;

    this.basePath = baseUrl + '/' + APP_NAME;
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
          .select(s("id"), s("type"))
          .retrieveRows()
          .forEach(
              resource -> {
                String collectionId = resource.getString("id");
                ResourcePath resourcePath = getResourcePath(resource);
                try {
                  wsg.addUrl(urlForResource(basePath, resourcePath, collectionId));
                } catch (MalformedURLException e) {
                  logger.error(
                      "Failed to generate sitemap url (schema: ({} , path: {} , id: {}",
                      schema.getName(),
                      resourcePath.name(),
                      collectionId,
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

  private WebSitemapUrl urlForVariable(Row variable) throws MalformedURLException {
    String variableId = encodePathSegment(variable.getString("name"));
    String resource = encodePathSegment(variable.getString(RESOURCE));
    String dataset = encodePathSegment(variable.getString("dataset"));

    // human-readable key
    String variableCombiKey = String.join("-", variableId, resource, dataset, resource);

    // key used to pinpoint the resource
    String queryPart =
        String.format(
            "?keys={\"name\":\"%s\",\"resource\":{\"id\":\"%s\"},\"dataset\":{\"name\":\"%s\",\"resource\":{\"id\":\"%s\"}}}",
            variableId, resource, dataset, resource);

    return new WebSitemapUrl.Options(
            "%s/all/variables/%s%s"
                .formatted(basePath, encodePathSegment(variableCombiKey), queryPart))
        .build();
  }

  private String encodePathSegment(String segment) {
    String encodedSegment =
        segment.replace("\n", "").replace("\r", "").replace(" ", "+").replace("\u00A0", " ").trim();
    try {
      encodedSegment = URI.create(encodedSegment).getRawPath();
    } catch (IllegalArgumentException e) {
      throw new MolgenisException("Failed to encode path segment: %s".formatted(segment), e);
    }
    return encodedSegment;
  }
}
