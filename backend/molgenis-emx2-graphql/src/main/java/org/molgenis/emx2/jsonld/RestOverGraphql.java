package org.molgenis.emx2.jsonld;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.*;
import org.jooq.tools.StringUtils;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlExecutor;
import org.molgenis.emx2.utils.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestOverGraphql {
  private static final ObjectMapper mapper =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  private static final Logger logger = LoggerFactory.getLogger(RestOverGraphql.class);

  // todo add option to override query + variables
  public static String getAllAsTurtle(GraphqlExecutor graphql, String schemaURL) {
    return getAllAsTurtle(graphql, schemaURL, null);
  }

  public static String getAllAsJsonLd(GraphqlExecutor graphql, String schemaURL, String query) {
    long start = System.currentTimeMillis();
    try {
      if (StringUtils.isEmpty(query)) {
        query = graphql.getSelectAllQuery();
      }
      Map data = graphql.queryAsMap(query, Map.of());
      Map context =
          org.molgenis.emx2.rdf.jsonld.JsonLdSchemaGenerator.generateJsonLdSchemaAsMap(
              graphql.getSchema().getMetadata(), schemaURL);

      Map wrapper = new LinkedHashMap();
      wrapper.putAll(context);
      wrapper.put("data", data);
      String result = mapper.writeValueAsString(wrapper);
      logger.info("Complete getAllAsJsonLd in " + (System.currentTimeMillis() - start) + "ms");
      return result;
    } catch (Exception e) {
      logger.error("Failed getAllAsJsonLd in " + (System.currentTimeMillis() - start) + "ms");
      throw new MolgenisException(e.getMessage(), e);
    }
  }

  public static String getAllAsTurtle(GraphqlExecutor graphql, String schemaURL, String query) {
    long start = System.currentTimeMillis();
    try {
      if (StringUtils.isEmpty(query)) {
        query = graphql.getSelectAllQuery();
      }
      Map data = graphql.queryAsMap(query, Map.of());
      String jsonLdSchema =
          org.molgenis.emx2.rdf.jsonld.JsonLdSchemaGenerator.generateJsonLdSchema(
              graphql.getSchema().getMetadata(), schemaURL);
      String result = convertToTurtle(mapper.readValue(jsonLdSchema, Map.class), data);
      logger.info("Complete getAllAsTurtle in " + (System.currentTimeMillis() - start) + "ms");
      return result;
    } catch (Exception e) {
      logger.error("Failed getAllAsTurtle in " + (System.currentTimeMillis() - start) + "ms");
      throw new MolgenisException(e.getMessage(), e);
    }
  }

  public static String getTableAsJson(
      GraphqlExecutor graphql, String tableId, Map<String, Object> variables) {
    // todo add ability to pass query filters, limit, offset via variables
    String query = String.format("{%s{...All%sFields}}", tableId, tableId);
    return graphql.queryAsString(query, variables);
  }

  public static String getTableAsTurtle(GraphqlExecutor graphql, String tableId) {
    return getTableAsTurtle(graphql, tableId, Map.of());
  }

  public static String getTableAsTurtle(
      GraphqlExecutor graphql, String tableId, Map<String, Object> variables) {
    try {
      String query = String.format("{%s{...All%sFields}}", tableId, tableId);
      Map data = graphql.queryAsMap(query, variables);
      String jsonLdSchema =
          org.molgenis.emx2.rdf.jsonld.JsonLdSchemaGenerator.generateJsonLdSchema(
              graphql.getSchema().getMetadata(), "http://localhost");
      return convertToTurtle(mapper.readValue(jsonLdSchema, Map.class), data);
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage(), e);
    }
  }

  // todo add
  // set based post, put, delete later
  // id based idem

  public static String convertToTurtle(
      Map<String, Object> jsonLdSchema, Map<String, Object> graphqlLikeData) throws IOException {
    Map wrapper = new LinkedHashMap<>();
    wrapper.putAll(jsonLdSchema);
    wrapper.put("data", graphqlLikeData);
    try (StringReader reader = new StringReader(mapper.writeValueAsString(wrapper))) {
      Model model = Rio.parse(reader, "", RDFFormat.JSONLD);
      StringWriter writer = new StringWriter();
      Rio.write(model, writer, RDFFormat.TURTLE);
      return writer.toString();
    } catch (Exception e) {
      JsonLdValidator.validateJsonLd(wrapper);
      throw new MolgenisException("Convert to turtle failed", e);
    }
  }

  public static Map<String, Object> stripJsonLdKeywords(Map<String, Object> data) {
    if (data == null) {
      return null;
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      String key = entry.getKey();
      if (!key.startsWith("@")) {
        Object value = entry.getValue();
        if (value instanceof Map) {
          result.put(key, stripJsonLdKeywords((Map<String, Object>) value));
        } else if (value instanceof List) {
          result.put(key, stripJsonLdKeywords((List) value));
        } else {
          result.put(key, value);
        }
      }
    }
    return result;
  }

  private static List stripJsonLdKeywords(List data) {
    if (data == null) {
      return null;
    }
    List result = new ArrayList<>();
    for (Object item : data) {
      if (item instanceof Map) {
        result.add(stripJsonLdKeywords((Map<String, Object>) item));
      } else if (item instanceof List) {
        result.add(stripJsonLdKeywords((List) item));
      } else {
        result.add(item);
      }
    }
    return result;
  }

  public static int importJsonLd(Table table, Map<String, Object> jsonLdData) {
    if (jsonLdData == null || table == null) {
      throw new MolgenisException("Table and jsonLdData cannot be null");
    }

    Object dataObj = jsonLdData.get("data");
    if (dataObj == null) {
      dataObj = jsonLdData.get("@graph");
    }
    if (dataObj == null) {
      throw new MolgenisException("JSON-LD must contain 'data' or '@graph' key");
    }

    List<Map<String, Object>> rows;
    if (dataObj instanceof List) {
      rows = (List<Map<String, Object>>) dataObj;
    } else if (dataObj instanceof Map) {
      Map<String, Object> dataMap = (Map<String, Object>) dataObj;
      String tableId = table.getMetadata().getIdentifier();
      if (dataMap.containsKey(tableId)) {
        Object tableData = dataMap.get(tableId);
        if (tableData instanceof List) {
          rows = (List<Map<String, Object>>) tableData;
        } else {
          throw new MolgenisException("Table data must be a List");
        }
      } else {
        rows = Collections.singletonList(dataMap);
      }
    } else {
      throw new MolgenisException("Data must be a Map or List of Maps");
    }

    List<Map<String, Object>> cleanedMaps = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      cleanedMaps.add(stripJsonLdKeywords(row));
    }

    List<Row> cleanedRows = TypeUtils.convertToRows(table.getMetadata(), cleanedMaps);
    return table.save(cleanedRows);
  }
}
