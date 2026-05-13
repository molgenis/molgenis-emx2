package org.molgenis.emx2.rdf.generators.query.mappers;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.molgenis.emx2.Column;

public class ArrayColumnSparqlQueryGenerator extends LiteralColumnSparqlQueryGenerator {

  public ArrayColumnSparqlQueryGenerator(Variable subject, Column column) {
    this(subject, column, SparqlBuilder.var(ColumnVariableNameMapper.columnToSparql(column)));
  }

  public ArrayColumnSparqlQueryGenerator(Variable subject, Column column, Variable object) {
    super(
        subject,
        column,
        // "_single" variable binds each array element before aggregation in getSelectors()
        SparqlBuilder.var(object.getVarName() + "_single"),
        object,
        column.isRequired());
  }

  /** Aggregates multiple values into a comma-separated string, e.g. "val1,val2,val3" */
  @Override
  public List<Projectable> getSelectors() {
    return List.of(
        Expressions.group_concat("','", Expressions.str(object)).distinct().as(selector));
  }

  @Override
  public List<Groupable> getGroupBy() {
    return new ArrayList<>();
  }
}
