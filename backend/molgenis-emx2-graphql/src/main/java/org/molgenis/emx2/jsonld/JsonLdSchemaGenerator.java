package org.molgenis.emx2.jsonld;

import static org.molgenis.emx2.Constants.MG_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import org.jooq.JSONB;
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

  public static Map<String, ?> generateJsonLdSchemaAsMap(SchemaMetadata schema, String schemaUrl) {
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
    context.put(PREFIX.replace(":", ""), schemaUrl);

    for (Map.Entry<String, String> entry : schemaNamespaces.entrySet()) {
      context.put(entry.getKey(), entry.getValue());
    }
    context.put("schema", "http://schema.org/");

    context.put("data", "@graph"); // graphql data will work
    context.put("@base", "my:"); // makes sure all id are prefixed properly
    context.put(MG_ID, "@id");

    // this statement allows @type to be a set
    // context.put("@context", Map.of("@version", 1.1, "@type", Map.of("@container", "@set")));

    for (TableMetadata table : schema.getTables()) {
      Map<String, Object> tableContext = new LinkedHashMap<>();

      for (Column column :
          table.getColumns().stream().filter(column -> !column.getName().equals(MG_ID)).toList()) {
        Map<String, Object> columnContext = new HashMap<>();
        // todo, we will ensure each graphql output will get generated id
        columnContext.put("@id", PREFIX + table.getIdentifier() + "#" + column.getIdentifier());
        if (column.isReference()) {
          columnContext.put("@type", "@id");
        } else {
          columnContext.put("@type", getXsdType(column.getColumnType()));
        }
        tableContext.put(column.getIdentifier(), columnContext);
        tableContext.put(MG_ID, "@id");
      }
      Map<String, Object> tableNode = new LinkedHashMap<>();
      tableNode.put("@id", PREFIX + table.getIdentifier());
      if (table.getSemantics() != null && table.getSemantics().length > 0) {
        tableNode.put(
            "@type",
            table.getSemantics().length == 1 ? table.getSemantics()[0] : table.getSemantics());
      }
      tableNode.put("@context", tableContext);
      context.put(table.getIdentifier(), tableNode);
    }

    Map<String, Object> root = new HashMap<>();
    root.put("@context", context);
    return root;
  }

  private static final Map<Class<?>, String> XSD_TYPE_MAP =
      Map.ofEntries(
          Map.entry(String.class, "xsd:string"),
          Map.entry(Integer.class, "xsd:integer"),
          Map.entry(Long.class, "xsd:long"),
          Map.entry(Double.class, "xsd:double"),
          Map.entry(Boolean.class, "xsd:boolean"),
          Map.entry(LocalDate.class, "xsd:date"),
          Map.entry(LocalDateTime.class, "xsd:dateTime"),
          Map.entry(JSONB.class, "xsd:string"),
          Map.entry(UUID.class, "xsd:string"),
          Map.entry(Period.class, "xsd:duration"),
          Map.entry(byte[].class, "xsd:base64Binary"));

  private static Object getXsdType(ColumnType columnType) {
    Class<?> type = columnType.getNonArrayType();
    String result = XSD_TYPE_MAP.get(type);
    if (result == null) {
      throw new MolgenisException("XSD type missing for type " + type.getName());
    }
    return result;
  }
}
