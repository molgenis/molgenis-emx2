package org.molgenis.emx2.web.service;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.FilterBean.or;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.web.util.EncodingHelpers.encodePathSegment;
import static org.molgenis.emx2.web.util.EncodingHelpers.encodeQueryParam;

import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import java.net.MalformedURLException;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogueSiteMap {
  private static final Logger logger = LoggerFactory.getLogger(CatalogueSiteMap.class);

  private static final String RESOURCE = "resource";

  private enum ResourcePath {
    networks,
    collections,
  }

  private final Schema schema;
  private final String baseUrl;
  private final String networkTableClass;
  private final String catalogueTableClass;
  private final String collectionTableClass;

  public CatalogueSiteMap(Schema schema, String baseUrl) {
    this.schema = schema;
    this.baseUrl = baseUrl;
    networkTableClass = "%s.%s".formatted(schema.getName(), "Networks");
    catalogueTableClass = "%s.%s".formatted(schema.getName(), "Catalogues");
    collectionTableClass = "%s.%s".formatted(schema.getName(), "Collections");
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
          .select(s("id"), s(Constants.MG_TABLECLASS))
          .retrieveRows()
          .forEach(
              resource -> {
                String collectionId = resource.getString("id");
                ResourcePath resourcePath = getResourcePath(resource);
                if (resourcePath != null) {
                  try {
                    wsg.addUrl(urlForResource(baseUrl, resourcePath, collectionId));
                  } catch (MalformedURLException e) {
                    logger.error(
                        "Failed to generate sitemap url (schema: {} , path: {} , id: {})",
                        schema.getName(),
                        resourcePath.name(),
                        collectionId,
                        e);
                  }
                }
              });

      Table variableTable = schema.getTable("Variables");

      if (variableTable != null) {
        variableTable
            .select(s("name"), s(RESOURCE), s("dataset"))
            .where(
                f(
                    RESOURCE,
                    or(
                        f(
                            Constants.MG_TABLECLASS,
                            Operator.EQUALS,
                            schema.getName() + "." + "Networks"),
                        f(
                            Constants.MG_TABLECLASS,
                            Operator.EQUALS,
                            schema.getName() + "." + "Catalogues"))))
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
    String tableClass = resource.getString(Constants.MG_TABLECLASS);
    if (networkTableClass.equals(tableClass) || catalogueTableClass.equals(tableClass)) {
      return ResourcePath.networks;
    } else if (collectionTableClass.equals(tableClass)) {
      return ResourcePath.collections;
    } else {
      return null;
    }
  }

  private WebSitemapUrl urlForResource(
      String resourceBasePath, ResourcePath resourcePath, String resourceId)
      throws MalformedURLException {
    return new WebSitemapUrl.Options(
            "%s/all/%s/%s"
                .formatted(
                    resourceBasePath,
                    encodePathSegment(resourcePath.name()),
                    encodePathSegment(resourceId)))
        .build();
  }

  private WebSitemapUrl urlForVariable(Row variable) throws MalformedURLException {
    String variableId = variable.getString("name");
    String resource = variable.getString(RESOURCE);
    String dataset = variable.getString("dataset");

    // human-readable key
    String variableIdPathSegment = String.join("-", variableId, resource, dataset, resource);

    // JSON query parameter value
    String variableIdQueryParamValue =
        String.format(
            "{\"name\":\"%s\",\"resource\":{\"id\":\"%s\"},\"dataset\":{\"name\":\"%s\",\"resource\":{\"id\":\"%s\"}}}",
            variableId, resource, dataset, resource);

    // note segment and query have their own encoding
    return new WebSitemapUrl.Options(
            "%s/all/variables/%s%s"
                .formatted(
                    baseUrl,
                    encodePathSegment(variableIdPathSegment),
                    "?keys=" + encodeQueryParam(variableIdQueryParamValue)))
        .build();
  }
}
