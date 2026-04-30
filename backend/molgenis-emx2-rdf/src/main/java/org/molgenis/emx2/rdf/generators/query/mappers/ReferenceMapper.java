package org.molgenis.emx2.rdf.generators.query.mappers;

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

public class ReferenceMapper implements ColumnMapper {

  private final Variable variable;
  private final TableMetadata targetTable;
  private final boolean isRequired;
  private final List<ColumnMapper> mappers;

  public ReferenceMapper(Variable variable, Reference reference, SchemaMetadata metadata) {
    this.variable = variable;
    this.targetTable = metadata.getTableMetadata(reference.getTargetTable());
    this.isRequired = reference.toPrimitiveColumn().isRequired();
    this.mappers = setupMappers(variable);
  }

  private List<ColumnMapper> setupMappers(Variable variable) {
    List<ColumnMapper> columnMappers = new ArrayList<>();
    for (Column column : targetTable.getPrimaryKeyColumns()) {
      columnMappers.add(
          new PlainColumnMapper(variable, column, getRefVariable(column), isRequired));
      if (column.isReference()) {
        Variable ref = SparqlBuilder.var(variable.getVarName() + "_" + column.getName());
        columnMappers.add(
            new ReferenceMapper(ref, column.getReferences().getFirst(), targetTable.getSchema()));
      }
    }

    return columnMappers;
  }

  @Override
  public List<Projectable> getSelectors() {
    return List.of();
  }

  @Override
  public List<GraphPattern> getPattern() {
    List<GraphPattern> columnPatterns =
        mappers.stream().flatMap(mapper -> mapper.getPattern().stream()).toList();

    if (isRequired) {
      return columnPatterns;
    } else {
      return List.of(GraphPatterns.and(columnPatterns.toArray(new GraphPattern[0])).optional());
    }
  }

  private Variable getRefVariable(Column column) {
    return SparqlBuilder.var(variable.getVarName() + "_" + column.getName().replace(".", "_"));
  }
}
