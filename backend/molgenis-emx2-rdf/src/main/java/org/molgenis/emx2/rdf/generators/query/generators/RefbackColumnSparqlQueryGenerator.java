package org.molgenis.emx2.rdf.generators.query.generators;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.rdf.generators.query.SparqlVariableUtil;

public class RefbackColumnSparqlQueryGenerator extends LiteralColumnSparqlQueryGenerator {

  public RefbackColumnSparqlQueryGenerator(Variable subject, Column column, Column rootColumn) {
    this(subject, column, SparqlVariableUtil.subjectVariable(rootColumn), rootColumn.isRequired());
  }

  private RefbackColumnSparqlQueryGenerator(
      Variable subject, Column column, Variable refbackSubject, boolean required) {
    super(
        subject,
        column,
        SparqlVariableUtil.singleVariable(refbackSubject),
        refbackSubject,
        required,
        true);
  }

  @Override
  public List<Projectable> getSelectors() {
    return List.of(SparqlVariableUtil.concatAs(object, selector));
  }

  @Override
  public List<Groupable> getGroupBy() {
    return List.of();
  }
}
