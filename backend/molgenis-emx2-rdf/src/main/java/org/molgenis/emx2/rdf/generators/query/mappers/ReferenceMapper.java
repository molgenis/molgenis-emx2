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

  public ReferenceMapper(Variable variable, Reference reference, SchemaMetadata metadata) {
    this.variable = variable;
    this.targetTable = metadata.getTableMetadata(reference.getTargetTable());
    this.isRequired = reference.toPrimitiveColumn().isRequired();
  }

  @Override
  public List<Projectable> getSelectors() {
    return List.of();
  }

  @Override
  public List<GraphPattern> getPattern() {
    List<GraphPattern> patterns = new ArrayList<>();

    for (Column column : targetTable.getPrimaryKeyColumns()) {

      List<GraphPattern> columnPatterns =
          new ArrayList<>(
              new PlainColumnMapper(variable, column, getRefVariable(column), isRequired)
                  .getPattern());
      if (column.isReference()) {
        Variable ref = SparqlBuilder.var(variable.getVarName() + "_" + column.getName());
        columnPatterns.addAll(
            new ReferenceMapper(ref, column.getReferences().getFirst(), targetTable.getSchema())
                .getPattern());
      }

      if (isRequired) {
        patterns.addAll(columnPatterns);
      } else {
        patterns.add(GraphPatterns.and(columnPatterns.toArray(new GraphPattern[0])).optional());
      }
    }

    return patterns;
  }

  private Variable getRefVariable(Column column) {
    return SparqlBuilder.var(variable.getVarName() + "_" + column.getName().replace(".", "_"));
  }
}
