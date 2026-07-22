package org.molgenis.emx2.rdf.generators.query.generators;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.rdf.generators.query.ColumnNameSparqlEncoder;
import org.molgenis.emx2.rdf.generators.query.SparqlVariableUtil;

public class ArrayColumnSparqlQueryGenerator extends LiteralColumnSparqlQueryGenerator {

  public ArrayColumnSparqlQueryGenerator(Variable subject, Column column) {
    this(subject, column, ColumnNameSparqlEncoder.encodeSparqlVariable(column));
  }

  public ArrayColumnSparqlQueryGenerator(Variable subject, Column column, Variable object) {
    super(
        subject,
        column,
        // "_single" variable binds each array element before aggregation in getSelectors()
        SparqlVariableUtil.singleVariable(object),
        object,
        column.isRequired());
  }

  /** Aggregates multiple values into a comma-separated string, e.g. "val1,val2,val3" */
  @Override
  public List<Projectable> getSelectors() {
    return List.of(SparqlVariableUtil.concatAs(object, selector));
  }

  @Override
  public List<Groupable> getGroupBy() {
    return List.of();
  }
}
