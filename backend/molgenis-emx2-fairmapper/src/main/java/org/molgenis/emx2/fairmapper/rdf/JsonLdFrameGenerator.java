package org.molgenis.emx2.fairmapper.rdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.DefaultNamespace;

public class JsonLdFrameGenerator {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public JsonNode generate(SchemaMetadata schema) {
    ObjectNode root = MAPPER.createObjectNode();
    ObjectNode context = MAPPER.createObjectNode();

    DefaultNamespace.streamAll().forEach(ns -> context.put(ns.getPrefix(), ns.getName()));

    String schemaPrefix = schema.getIdentifier();
    context.put(schemaPrefix, "urn:molgenis:schema:" + schemaPrefix + "#");

    for (TableMetadata table : schema.getTables()) {
      if (table.getSemantics() != null && table.getSemantics().length > 0) {
        ObjectNode tableNode = MAPPER.createObjectNode();
        tableNode.put("@id", schemaPrefix + ":" + table.getIdentifier());
        tableNode.put("@type", table.getSemantics()[0]);
        context.set(table.getIdentifier(), tableNode);
      }
    }

    Set<String> processedColumns = new HashSet<>();
    for (TableMetadata table : schema.getTables()) {
      for (Column column : table.getLocalColumns()) {
        String columnId = column.getIdentifier();
        if (processedColumns.contains(columnId)) {
          continue;
        }

        String[] semantics = column.getSemantics();
        boolean hasSemantics = semantics != null && semantics.length > 0;
        boolean isReference = column.isReference() || column.isOntology();

        if (isReference) {
          processedColumns.add(columnId);
          ObjectNode columnNode = MAPPER.createObjectNode();
          String predicateIri;
          if (hasSemantics) {
            predicateIri = semantics[0];
          } else {
            predicateIri = schemaPrefix + ":" + columnId;
          }
          columnNode.put("@id", predicateIri);
          columnNode.put("@type", "@id");
          context.set(columnId, columnNode);

          ObjectNode embedNode = MAPPER.createObjectNode();
          embedNode.put("@embed", "@always");
          root.set(predicateIri, embedNode);
        } else if (hasSemantics) {
          processedColumns.add(columnId);
          ObjectNode columnNode = MAPPER.createObjectNode();
          columnNode.put("@id", semantics[0]);
          context.set(columnId, columnNode);
        }
      }
    }

    root.set("@context", context);
    root.put("@embed", "@always");
    return root;
  }

  public String generateAsString(SchemaMetadata schema) {
    try {
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(generate(schema));
    } catch (Exception e) {
      throw new MolgenisException("Failed to generate JSON-LD frame: " + e.getMessage());
    }
  }
}
