package org.molgenis.emx2.harvester.mappers;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Operand;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.molgenis.emx2.Column;

public class PlainColumnMapper implements ColumnMapper {

  private final Variable startingPoint;
  private final Column column;
  private final Variable objectVariable;

  public PlainColumnMapper(Variable subjectVariable, Column column) {
    this(subjectVariable, column, SparqlBuilder.var(normalizedColumnName(column.getName())));
  }

  public PlainColumnMapper(Variable startingPoint, Column column, Variable objectVariable) {
    this.startingPoint = startingPoint;
    this.column = column;
    this.objectVariable = objectVariable;
  }

  @Override
  public List<Projectable> getSelectors() {
    return List.of();
  }

  @Override
  public List<GraphPattern> getPattern() {
    if (column.getSemantics().length > 1) {
      return multiSemanticPattern();
    }

    String semantic = column.getSemantics()[0];
    GraphPattern pattern = GraphPatterns.tp(startingPoint, () -> semantic, objectVariable);

    return List.of(column.isRequired() ? pattern : pattern.optional());
  }

  private List<GraphPattern> multiSemanticPattern() {
    List<GraphPattern> patterns = new ArrayList<>();
    List<Operand> aliases = new ArrayList<>();

    for (int i = 0; i < column.getSemantics().length; i++) {
      String semantic = column.getSemantics()[i];
      Variable alias = SparqlBuilder.var(objectVariable.getVarName() + i);

      GraphPattern pattern = GraphPatterns.tp(startingPoint, () -> semantic, alias).optional();
      patterns.add(pattern);
      aliases.add(alias);
    }

    Expression<?> coalesce = Expressions.coalesce(aliases.toArray(new Operand[0]));
    patterns.add(Expressions.bind(coalesce, objectVariable));

    if (column.isRequired()) {
      Expression<?> bound = Expressions.bound(objectVariable);
      patterns.add(() -> "FILTER ( " + bound.getQueryString() + " )");
    }

    return patterns;
  }

  private static String normalizedColumnName(String input) {
    return input.replace(" ", "_");
  }
}
