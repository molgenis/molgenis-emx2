package org.molgenis.emx2.jsonld;

import static org.molgenis.emx2.Constants.MG_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.*;
import org.molgenis.emx2.*;

public class JsonLdSchemaGenerator {
  private static final ObjectMapper mapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  public static String generateJsonLdSchema(SchemaMetadata schema, String schemaUrl) {
    try {
      return mapper.writeValueAsString(generateJsonLdSchemaAsMap(schema, schemaUrl));
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
  }

  public static Map<String, Object> generateJsonLdSchemaAsMap(
      SchemaMetadata schema, String schemaUrl) {
    final String PREFIX = "my:";

    // todo solve how schema can add namespaces
    Map<String, String> schemaNamespaces =
        Map.ofEntries(
            Map.entry("adms", "http://www.w3.org/ns/adms#"),
            Map.entry("csvw", "http://www.w3.org/ns/csvw#"),
            Map.entry("dcat", "http://www.w3.org/ns/dcat#"),
            Map.entry("dcatap", "http://data.europa.eu/r5r/"),
            Map.entry("dct", "http://purl.org/dc/terms/"),
            Map.entry("dcterms", "http://purl.org/dc/terms/"),
            Map.entry("dctype", "http://purl.org/dc/dcmitype/"),
            Map.entry("dpv", "https://w3id.org/dpv#"),
            Map.entry("dqv", "http://www.w3.org/ns/dqv#"),
            Map.entry("foaf", "http://xmlns.com/foaf/0.1/"),
            Map.entry("healthdcatap", "http://healthdataportal.eu/ns/health#"),
            Map.entry("locn", "http://www.w3.org/ns/locn#"),
            Map.entry("odrl", "http://www.w3.org/ns/odrl/2/"),
            Map.entry("owl", "http://www.w3.org/2002/07/owl#"),
            Map.entry("prov", "http://www.w3.org/ns/prov#"),
            Map.entry("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
            Map.entry("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
            Map.entry("skos", "http://www.w3.org/2004/02/skos/core#"),
            Map.entry("spdx", "http://spdx.org/rdf/terms#"),
            Map.entry("time", "http://www.w3.org/2006/time#"),
            Map.entry("vcard", "http://www.w3.org/2006/vcard/ns#"),
            Map.entry("xsd", "http://www.w3.org/2001/XMLSchema#"),
            Map.entry("fdp-o", "https://w3id.org/fdp/fdp-o#"),
            Map.entry("ldp", "http://www.w3.org/ns/ldp#"),
            Map.entry("healthDCAT-AP", "http://healthdataportal.eu/ns/health#"),
            Map.entry("or", "http://www.w3.org/ns/org#"));

    Map<String, Object> context = new LinkedHashMap<>();
    String namespaceUrl =
        schemaUrl.endsWith("/") || schemaUrl.endsWith("#") ? schemaUrl : schemaUrl + "#";
    context.put(PREFIX.replace(":", ""), namespaceUrl);

    for (Map.Entry<String, String> entry : schemaNamespaces.entrySet()) {
      context.put(entry.getKey(), entry.getValue());
    }
    context.put("schema", "http://schema.org/");

    context.put("data", "@graph");
    context.put("@base", schemaUrl + "/");
    context.put(MG_ID, "@id");

    for (TableMetadata table : schema.getTables()) {
      Map<String, Object> tableNode = new LinkedHashMap<>();
      tableNode.put("@id", PREFIX + table.getIdentifier());
      if (table.getSemantics() != null && table.getSemantics().length > 0) {
        tableNode.put("@type", table.getSemantics()[0]);
      }
      context.put(table.getIdentifier(), tableNode);
    }

    for (TableMetadata table : schema.getTables()) {
      for (Column column : table.getLocalColumns()) {
        String columnKey = column.getIdentifier();
        if (!context.containsKey(columnKey)
            && (column.isReference() || "ontologyTermURI".equals(column.getName()))) {
          Map<String, Object> columnNode = new LinkedHashMap<>();
          if (column.getSemantics() != null && column.getSemantics().length > 0) {
            columnNode.put("@id", column.getSemantics()[0]);
          } else {
            columnNode.put("@id", PREFIX + columnKey);
          }
          columnNode.put("@type", "@id");
          context.put(columnKey, columnNode);
        }
      }
    }

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("@context", context);
    root.put("@embed", "@always");
    for (TableMetadata table : schema.getTables()) {
      for (Column column : table.getLocalColumns()) {
        if (column.isReference()) {
          Map<String, Object> refFrame = new LinkedHashMap<>();
          refFrame.put("@embed", "@always");
          root.put(column.getIdentifier(), refFrame);
        }
      }
    }

    return root;
  }
}
