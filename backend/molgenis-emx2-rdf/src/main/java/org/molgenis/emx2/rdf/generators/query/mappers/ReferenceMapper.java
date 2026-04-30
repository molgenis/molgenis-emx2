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
  private final List<GraphPattern> patterns = new ArrayList<>();
  private final List<Projectable> selectors = new ArrayList<>();

  public ReferenceMapper(Variable variable, Reference reference, SchemaMetadata metadata) {
    this.variable = variable;
    this.targetTable = metadata.getTableMetadata(reference.getTargetTable());
    this.isRequired = reference.toPrimitiveColumn().isRequired();
    map();
  }

  private void map() {
    for (Column column : targetTable.getPrimaryKeyColumns()) {
      Variable object = getRefVariable(column);
      ColumnMapper columnMapper = new PlainColumnMapper(variable, column, object, isRequired);
      patterns.addAll(columnMapper.getPattern());

      if (column.isReference()) {
        Variable ref = SparqlBuilder.var(variable.getVarName() + "_" + column.getName());
        Reference firstRef = column.getReferences().getFirst();
        ColumnMapper reference = new ReferenceMapper(ref, firstRef, targetTable.getSchema());

        patterns.addAll(reference.getPattern());
        selectors.addAll(reference.getSelectors());
      } else {
        selectors.addAll(columnMapper.getSelectors());
      }
    }
  }

  @Override
  public List<Projectable> getSelectors() {
    return new ArrayList<>(selectors);
  }

  @Override
  public List<GraphPattern> getPattern() {
    if (isRequired) {
      return new ArrayList<>(patterns);
    } else {
      return List.of(GraphPatterns.and(patterns.toArray(new GraphPattern[0])).optional());
    }
  }

  private Variable getRefVariable(Column column) {
    return SparqlBuilder.var(variable.getVarName() + "_" + column.getName().replace(".", "_"));
  }
}
