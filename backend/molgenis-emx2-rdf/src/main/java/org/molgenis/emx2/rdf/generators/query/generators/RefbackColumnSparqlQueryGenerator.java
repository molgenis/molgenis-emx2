package org.molgenis.emx2.rdf.generators.query.generators;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.rdf.generators.query.SparqlVariableUtil;

/**
 * Generates SPARQL query components for a refback column, i.e. a column with no predicate of its
 * own that shows the other side of a reference column on a different table:
 *
 * <pre>{@code
 * Person --------- foaf:pet --------> Pet
 * (refbackColumn)                     (referencedColumn)
 * }</pre>
 *
 * <p>The pattern follows {@code foaf:pet} backwards from {@code Pet}, e.g. {@code ?pet ^foaf:pet
 * ?owner}.
 */
public class RefbackColumnSparqlQueryGenerator extends LiteralColumnSparqlQueryGenerator {

  /**
   * @param subject variable for the current row
   * @param referenced the reference column on the other table
   * @param refbackColumn the refback column itself
   */
  public RefbackColumnSparqlQueryGenerator(
      Variable subject, Column referenced, Column refbackColumn) {
    this(
        subject,
        referenced,
        SparqlVariableUtil.subjectVariable(refbackColumn),
        refbackColumn.isRequired());
  }

  private RefbackColumnSparqlQueryGenerator(
      Variable subject, Column referenced, Variable refbackSubject, boolean required) {
    super(
        subject,
        referenced,
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
