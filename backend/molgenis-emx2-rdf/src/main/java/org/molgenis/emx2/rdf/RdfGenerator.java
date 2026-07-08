package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;
import static org.molgenis.emx2.rdf.RdfUtils.formatBaseURL;

import graphql.ExecutionResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.molgenis.emx2.rdf.writers.RdfWriter;

public class RdfGenerator {
  private final RdfWriter writer;
  private final String baseURL;

  public RdfGenerator(RdfWriter writer, String baseURL) {
    this.writer = writer;
    this.baseURL = formatBaseURL(baseURL);
  }

  public void generate(Schema schema, ExecutionResult executionResult) {
    NamespaceMapper namespaces = new NamespaceMapper(baseURL, schema);
    namespaces.getAllNamespaces(schema).forEach(writer::processNamespace);

    Object data = executionResult.getData();
    if (data instanceof Map) {
      // Loop through level 1 tables
      for (Map.Entry<String, Object> tableEntry : ((Map<String, Object>) data).entrySet()) {
        Table table = schema.getTable(tableEntry.getKey());
        if (tableEntry.getValue() instanceof List) {
          for (Object rowData : (List) tableEntry.getValue()) {
            if (rowData instanceof Map) {
              Map<String, Object> rowDataMap = (Map<String, Object>) rowData;
              List<String> primaryKeyColumnNames =
                  table.getMetadata().getPrimaryKeyColumns().stream()
                      .map(Column::getIdentifier)
                      .toList();

              PrimaryKey primaryKey =
                  PrimaryKey.fromEncodedString(
                      table,
                      primaryKeyColumnNames.stream()
                          .map(pkColumn -> pkColumn + "=" + rowDataMap.get(pkColumn).toString())
                          .collect(Collectors.joining("&")));

              IRI subject = rowIRI(baseURL, table, primaryKey);

              for (Map.Entry<String, Object> columnValuePair : rowDataMap.entrySet()) {
                String[] semantics =
                    table.getMetadata().getColumn(columnValuePair.getKey()).getSemantics();
                if (semantics != null) {
                  for (String semantic : semantics) {
                    IRI predicate = namespaces.map(schema, semantic);

                    if (columnValuePair.getValue() instanceof Map) {
                      // todo: when Map is found, we need to generate a deeper level of triples
                      // (recursion?)
                    } else {
                      // todo: actual implementation using ColumnTypeRdfMapper
                      Value object = Values.literal(columnValuePair.getValue().toString());
                      writer.processTriple(subject, predicate, object);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
