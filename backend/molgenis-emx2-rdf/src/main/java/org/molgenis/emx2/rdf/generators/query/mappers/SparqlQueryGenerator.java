package org.molgenis.emx2.rdf.generators.query.mappers;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;

/**
 * Generates the SPARQL SELECT clause components for a given column type.
 *
 * <p>Implementations map a specific {@link org.molgenis.emx2.TableMetadata} column kind to the
 * three structural parts of a SPARQL SELECT query:
 *
 * <ul>
 *   <li>projected variables
 *   <li>graph patterns (WHERE clause)
 *   <li>and GROUP BY expressions
 * </ul>
 *
 * @see ArrayColumnSparqlQueryGenerator
 * @see LiteralColumnSparqlQueryGenerator
 * @see ReferenceColumnSparqlQueryGenerator
 */
public interface SparqlQueryGenerator {

  /** Returns the variables or expressions projected in the SELECT clause. */
  List<Projectable> getSelectors();

  /**
   * Returns the graph patterns that constrain the input in the WHERE clause.
   * Multiple patterns are combined conjunctively.
   */
  List<GraphPattern> getPatterns();

  /** Returns the expressions added to the GROUP BY clause */
  List<Groupable> getGroupBy();
}
