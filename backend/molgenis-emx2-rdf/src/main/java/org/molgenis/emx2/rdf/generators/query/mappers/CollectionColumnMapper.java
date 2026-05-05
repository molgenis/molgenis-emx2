package org.molgenis.emx2.rdf.generators.query.mappers;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.molgenis.emx2.Column;

public class CollectionColumnMapper extends PlainColumnMapper {

  public CollectionColumnMapper(Variable subject, Column column) {
    this(subject, column, SparqlBuilder.var(new ColumnVariableName(column).getSparqlName()));
  }

  public CollectionColumnMapper(Variable subject, Column column, Variable object) {
    super(
        subject,
        column,
        SparqlBuilder.var(object.getVarName() + "_single"),
        object,
        column.isRequired());
  }

  @Override
  public List<Projectable> getSelectors() {
    return List.of(Expressions.group_concat(",", Expressions.str(object)).as(selector));
  }
}
