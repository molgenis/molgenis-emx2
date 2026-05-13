package org.molgenis.emx2.rdf.generators.query.mappers;

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
import org.molgenis.emx2.Column;

public class LiteralColumnSparqlQueryGenerator implements SparqlQueryGenerator {

  private final Variable subject;
  private final Column column;
  protected final Variable object;
  private final boolean isRequired;
  protected final Variable selector;

  public LiteralColumnSparqlQueryGenerator(Variable subject, Column column) {
    this(subject, column, SparqlBuilder.var(ColumnVariableNameMapper.columnToSparql(column)));
  }

  public LiteralColumnSparqlQueryGenerator(Variable subject, Column column, Variable object) {
    this(subject, column, object, object, column.isRequired());
  }

  public LiteralColumnSparqlQueryGenerator(Variable subject, Column column, Variable object, boolean isRequired) {
    this(subject, column, object, object, isRequired);
  }

  public LiteralColumnSparqlQueryGenerator(
      Variable subject, Column column, Variable object, Variable selector, boolean isRequired) {
    this.subject = subject;
    this.column = column;
    this.object = object;
    this.selector = selector;
    this.isRequired = isRequired;
  }

  @Override
  public List<Projectable> getSelectors() {
    return getSelector().stream().map(Projectable.class::cast).toList();
  }

  @Override
  public List<Groupable> getGroupBy() {
    return getSelector().stream().map(Groupable.class::cast).toList();
  }

  @Override
  public List<GraphPattern> getPatterns() {
    if (column.getSemantics().length > 1) {
      return multiSemanticPattern();
    }

    String semantic = column.getSemantics()[0];
    GraphPattern pattern =
        GraphPatterns.tp(subject, ColumnSemanticMapper.resolveIri(semantic), object);

    return List.of(isRequired ? pattern : pattern.optional());
  }

  private List<GraphPattern> multiSemanticPattern() {
    List<GraphPattern> semanticPatterns = new ArrayList<>();
    List<Operand> aliases = new ArrayList<>();

    for (int i = 0; i < column.getSemantics().length; i++) {
      String semantic = column.getSemantics()[i];
      Variable alias = SparqlBuilder.var(object.getVarName() + i);

      GraphPattern pattern =
          GraphPatterns.tp(subject, ColumnSemanticMapper.resolveIri(semantic), alias).optional();
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

  private List<Variable> getSelector() {
    if (selector == null) {
      return Collections.emptyList();
    }

    return List.of(selector);
  }
}
