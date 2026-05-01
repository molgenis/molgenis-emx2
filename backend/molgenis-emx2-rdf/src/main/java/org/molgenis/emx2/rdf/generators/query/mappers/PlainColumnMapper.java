package org.molgenis.emx2.rdf.generators.query.mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Operand;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.molgenis.emx2.Column;

public class PlainColumnMapper implements ColumnMapper {

  private final Variable subject;
  private final Column column;
  protected final Variable object;
  private final boolean isRequired;
  protected final Variable selector;

  public PlainColumnMapper(Variable subject, Column column) {
    this(subject, column, SparqlBuilder.var(new ColumnVariableName(column).getSparqlName()));
  }

  public PlainColumnMapper(Variable subject, Column column, Variable object) {
    this(subject, column, object, object, column.isRequired());
  }

  public PlainColumnMapper(Variable subject, Column column, Variable object, boolean isRequired) {
    this(subject, column, object, object, isRequired);
  }

  public PlainColumnMapper(
      Variable subject, Column column, Variable object, Variable selector, boolean isRequired) {
    this.subject = subject;
    this.column = column;
    this.object = object;
    this.selector = selector;
    this.isRequired = isRequired;
  }

  @Override
  public List<Projectable> getSelectors() {
    if (selector == null) {
      return Collections.emptyList();
    }

    return List.of(selector);
  }

  @Override
  public List<GraphPattern> getPattern() {
    if (column.getSemantics().length > 1) {
      return multiSemanticPattern();
    }

    String semantic = column.getSemantics()[0];
    GraphPattern pattern = GraphPatterns.tp(subject, () -> semantic, object);

    return List.of(isRequired ? pattern : pattern.optional());
  }

  private List<GraphPattern> multiSemanticPattern() {
    List<GraphPattern> semanticPatterns = new ArrayList<>();
    List<Operand> aliases = new ArrayList<>();

    for (int i = 0; i < column.getSemantics().length; i++) {
      String semantic = column.getSemantics()[i];
      Variable alias = SparqlBuilder.var(object.getVarName() + i);

      GraphPattern pattern = GraphPatterns.tp(subject, () -> semantic, alias).optional();
      semanticPatterns.add(pattern);
      aliases.add(alias);
    }

    Expression<?> coalesce = Expressions.coalesce(aliases.toArray(new Operand[0]));
    semanticPatterns.add(Expressions.bind(coalesce, object));

    GraphPatternNotTriples mainPattern =
        GraphPatterns.and(semanticPatterns.toArray(new GraphPattern[0])).optional();

    if (isRequired) {
      Expression<?> bound = Expressions.bound(object);
      return List.of(mainPattern, () -> "FILTER ( " + bound.getQueryString() + " )");
    }

    return List.of(mainPattern);
  }
}
