package org.molgenis.emx2.rdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;

public class ReverseAnnotationMapper {

  private static final IRI RDF_TYPE = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
  private static final String LOCALHOST_BASE = "http://localhost/";

  public record ColumnMapping(TableMetadata table, Column column) {}

  private ReverseAnnotationMapper() {}

  public static Map<IRI, List<ColumnMapping>> buildPredicateMap(Schema schema) {
    NamespaceMapper namespaceMapper = new NamespaceMapper(LOCALHOST_BASE, schema);
    return buildPredicateMap(
        schema.getMetadata(), semantic -> namespaceMapper.map(schema, semantic));
  }

  public static Map<IRI, List<ColumnMapping>> buildPredicateMap(SchemaMetadata schema) {
    return buildPredicateMap(schema, Values::iri);
  }

  private static Map<IRI, List<ColumnMapping>> buildPredicateMap(
      SchemaMetadata schema, Function<String, IRI> semanticToIri) {
    Map<IRI, List<ColumnMapping>> predicateMap = new HashMap<>();

    for (TableMetadata table : schema.getTables()) {
      if (table.getTableType() != TableType.DATA) {
        continue;
      }
      for (Column column : table.getColumns()) {
        if (column.getComputed() != null) {
          continue;
        }
        String[] semantics = column.getSemantics();
        if (semantics == null) {
          continue;
        }
        for (String semantic : semantics) {
          IRI predicate = semanticToIri.apply(semantic);
          if (RDF_TYPE.equals(predicate)) {
            continue;
          }
          predicateMap
              .computeIfAbsent(predicate, key -> new ArrayList<>())
              .add(new ColumnMapping(table, column));
        }
      }
    }

    return predicateMap;
  }
}
