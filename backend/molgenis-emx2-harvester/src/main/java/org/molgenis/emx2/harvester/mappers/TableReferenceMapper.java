package org.molgenis.emx2.harvester.mappers;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

public class TableReferenceMapper implements ColumnMapper {

  private final SchemaMetadata schema;
  private final Variable startingPoint;
  private final Reference reference;
  private final TableMetadata targetTable;
  private final Column targetColumn;
  private final Column fromColumn;

  public TableReferenceMapper(
      SchemaMetadata schema, Variable startingPoint, Column fromColumn, Reference reference) {
    this.schema = schema;
    this.startingPoint = startingPoint;
    this.reference = reference;
    this.fromColumn = fromColumn;
    this.targetTable = schema.getTableMetadata(reference.getTargetTable());
    this.targetColumn = targetTable.getColumn(reference.getTargetColumn());
  }

  @Override
  public List<Projectable> getSelectors() {
    return List.of();
  }

  @Override
  public List<GraphPattern> getPattern() {
    List<GraphPattern> patterns = new ArrayList<>();
    Variable ref = normalizedColumnName(fromColumn.getName());
    GraphPattern pattern = GraphPatterns.tp(startingPoint, () -> fromColumn.getSemantics()[0], ref);
    patterns.add(pattern);

    for (Column pkey : targetTable.getPrimaryKeyColumns()) {
      Variable objectVariable = normalizedColumnName(fromColumn.getName() + "." + pkey.getName());
      ColumnMapper mapper = new PlainColumnMapper(ref, pkey, objectVariable);
      patterns.addAll(mapper.getPattern());
    }

    return patterns;
  }

  private Variable normalizedColumnName(String name) {
    return SparqlBuilder.var(name.replace(" ", "_"));
  }
}
