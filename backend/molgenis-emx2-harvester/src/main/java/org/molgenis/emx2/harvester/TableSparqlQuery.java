package org.molgenis.emx2.harvester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.DefaultNamespace;

public class TableSparqlQuery {

  private static final List<ColumnType> ONTOLOGY_TYPES =
      List.of(ColumnType.ONTOLOGY, ColumnType.ONTOLOGY_ARRAY);

  private final SchemaMetadata schema;
  private final TableMetadata table;
  private final SelectQuery select = Queries.SELECT();

  private TriplePattern mainPattern = null;
  private final List<GraphPattern> optionalPatterns = new ArrayList<>();

  private final Variable mainVar;

  public TableSparqlQuery(SchemaMetadata schema, String tableName) {
    this(schema, tableName, DefaultNamespace.streamAll());
  }

  public TableSparqlQuery(SchemaMetadata schema, String tableName, Stream<Namespace> namespaces) {
    this.schema = schema;
    this.table = schema.getTableMetadata(tableName);
    this.mainVar = SparqlBuilder.var(tableName);
    namespaces.forEach(select::prefix);
  }

  public void build() {
    if (table.getSemantics() != null && !tableContainsRdfsTypeSemantic(table)) {
      String semantic = table.getSemantics()[0];
      mainPattern = mainVar.isA(object(semantic));
    }

    for (Column column : table.getColumns()) {
      if (column.getSemantics() == null) {
        continue;
      }

      Variable columnVar = SparqlBuilder.var(column.getName().replace(" ", "_"));
      String semantic = Arrays.stream(column.getSemantics()).findFirst().orElseThrow();

      if (column.isRequired()) {
        addToMainPattern(semantic, columnVar);
      } else {
        TriplePattern triple = mainVar.has(predicate(semantic), columnVar);
        optionalPatterns.add(GraphPatterns.optional(triple));
      }

      if (column.isReference()) {
        if (ONTOLOGY_TYPES.contains(column.getColumnType())) {
          continue;
        }

        resolveReference(column, columnVar);
      }

      select.select(columnVar);
    }

    select.where(mainPattern);
    for (GraphPattern pattern : optionalPatterns) {
      select.where(pattern);
    }
  }

  private boolean tableContainsRdfsTypeSemantic(TableMetadata table) {
    return table.getColumns().stream()
        .map(Column::getSemantics)
        .filter(Objects::nonNull)
        .flatMap(Arrays::stream)
        .collect(Collectors.toSet())
        .contains("rdf:type");
  }

  private void resolveReference(Column column, Variable columnVar) {
    Reference reference = column.getReferences().getFirst();

    TableColumnMapper query = new TableColumnMapper(columnVar, reference, schema);
    if (column.isRequired()) {
      query.getPattern().forEach(mainPattern::and);
    } else {
      optionalPatterns.addAll(query.getPattern());
    }
  }

  private TriplePattern triple(Variable subject, String predicate, Variable object) {
    return subject.has(predicate(predicate), object);
  }

  private void addToMainPattern(String semantic, Variable variable) {
    if (mainPattern == null) {
      mainPattern = triple(mainVar, semantic, variable);
    } else {
      mainPattern.andHas(predicate(semantic), variable);
    }
  }

  public String asString() {
    return select.getQueryString();
  }

  private RdfPredicate predicate(String semantic) {
    return () -> semantic;
  }

  private RdfObject object(String semantic) {
    return () -> semantic;
  }
}
