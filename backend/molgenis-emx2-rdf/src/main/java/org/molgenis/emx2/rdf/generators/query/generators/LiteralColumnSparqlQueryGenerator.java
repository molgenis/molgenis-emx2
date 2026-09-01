package org.molgenis.emx2.rdf.generators.query.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Operand;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.rdf.generators.query.ColumnNameSparqlEncoder;

public class LiteralColumnSparqlQueryGenerator implements ColumnSparqlQueryGenerator {

  private final Column column;
  private final boolean isRequired;
  private final boolean inverse;

  protected final Variable subject;
  protected final Variable object;
  protected final Variable selector;

  public LiteralColumnSparqlQueryGenerator(Variable subject, Column column) {
    this(
        subject, column, ColumnNameSparqlEncoder.encodeSparqlVariable(column), column.isRequired());
  }

  public LiteralColumnSparqlQueryGenerator(
      Variable subject, Column column, Variable object, boolean isRequired) {
    this(subject, column, object, object, isRequired, false);
  }

  protected LiteralColumnSparqlQueryGenerator(
      Variable subject,
      Column column,
      Variable object,
      Variable selector,
      boolean isRequired,
      boolean inverse) {
    this.subject = subject;
    this.column = column;
    this.object = object;
    this.selector = selector;
    this.isRequired = isRequired;
    this.inverse = inverse;
  }

  @Override
  public List<Projectable> getSelectors() {
    return (selector == null) ? Collections.emptyList() : List.of(selector);
  }

  @Override
  public List<Groupable> getGroupBy() {
    return (selector == null) ? Collections.emptyList() : List.of(selector);
  }

  @Override
  public List<GraphPattern> getPatterns() {
    if (column.getSemantics().length == 0) {
      return Collections.emptyList();
    } else if (column.getSemantics().length > 1) {
      return multiSemanticPattern();
    }

    RdfPredicate predicate =
        column
            .getSemanticsStringStream()
            .findFirst()
            .orElseThrow()
            .transform(this::inverseIfNeeded);
    GraphPattern pattern = GraphPatterns.tp(subject, predicate, object);

    return List.of(isRequired ? pattern : pattern.optional());
  }

  private List<GraphPattern> multiSemanticPattern() {
    List<GraphPattern> semanticPatterns = new ArrayList<>();
    List<Operand> aliases = new ArrayList<>();

    RdfPredicate[] semantics =
        column.getSemanticsStringStream().map(this::inverseIfNeeded).toArray(RdfPredicate[]::new);

    for (int i = 0; i < semantics.length; i++) {
      Variable alias = SparqlBuilder.var(object.getVarName() + i);
      GraphPattern pattern = GraphPatterns.tp(subject, semantics[i], alias).optional();
      semanticPatterns.add(pattern);
      aliases.add(alias);
    }

    Expression<?> coalesce = Expressions.coalesce(aliases.toArray(new Operand[0]));
    semanticPatterns.add(Expressions.bind(coalesce, object));

    GraphPatternNotTriples mainPattern =
        GraphPatterns.and(semanticPatterns.toArray(new GraphPattern[0])).optional();

    if (isRequired) {
      Expression<?> bound = Expressions.bound(object);
      return List.of(mainPattern, filter(bound.getQueryString()));
    }

    return List.of(mainPattern);
  }

  private RdfPredicate inverseIfNeeded(String semanticString) {
    return () -> (inverse ? "^" + semanticString : semanticString);
  }

  /**
   * The RDF4J SparqlBuilder's {@link GraphPattern} does not support generating a bare {@code
   * FILTER} statement. Available alternatives either wrap the filter in a group graph pattern
   * ({@code { FILTER(...) }}) or inside an {@code OPTIONAL} clause, both of which alter the scoping
   * of variables and produce incorrect query behavior when filtering on variables bound via {@code
   * OPTIONAL} or {@code BIND}.
   *
   * <p>This method works around that limitation by returning a {@link GraphPattern} that renders as
   * a bare {@code FILTER} statement, ensuring it appears as a sibling pattern in the enclosing
   * group rather than in an isolated scope.
   */
  private static GraphPattern filter(String toFilter) {
    return () -> "FILTER ( " + toFilter + " )";
  }
}
