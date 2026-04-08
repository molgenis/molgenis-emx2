package org.molgenis.emx2.harvester;

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
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.molgenis.emx2.*;

public class ReferenceQueryParts {

  private final Variable startingPoint;
  private final Column relatedColumn;
  private final TableMetadata relatedTable;
  private final boolean referenceRequired;

  public ReferenceQueryParts(Variable startingPoint, Reference reference, SchemaMetadata schema) {
    this.startingPoint = startingPoint;
    this.relatedTable = schema.getTableMetadata(reference.getTargetTable());
    this.relatedColumn = relatedTable.getColumn(reference.getTargetColumn());
    this.referenceRequired = reference.toPrimitiveColumn().isRequired();
  }

  public List<Projectable> getSelectors() {
    return Collections.emptyList();
  }

  public List<GraphPattern> getPattern() {
    if (relatedColumn.getSemantics() == null) {
      throw new MolgenisException(
          "Unable to resolve reference semantics for " + relatedColumn.getName());
    }

    List<GraphPattern> patterns = new ArrayList<>();

    for (Column pkey : relatedTable.getPrimaryKeyColumns()) {
      GraphPattern predicate = predicateForKey(startingPoint, pkey);
      patterns.add(predicate);
    }

    return patterns;
  }

  private GraphPattern predicateForKey(Variable variable, Column column) {
    if (column.getSemantics().length == 1) {
      String semantic = column.getSemantics()[0];
      Variable alias = SparqlBuilder.var(alias(column.getName()).getQueryString());
      TriplePattern pattern = variable.has(predicate(semantic), alias);
      return pattern.optional(referenceRequired);
    }

    List<GraphPattern> patterns = new ArrayList<>();
    List<Operand> aliases = new ArrayList<>();
    for (int i = 0; i < column.getSemantics().length; i++) {
      String semantic = column.getSemantics()[i];
      Variable alias = SparqlBuilder.var(alias(column.getName() + i).getQueryString());

      patterns.add(variable.has(predicate(semantic), alias).optional());
      aliases.add(alias);
    }

    Variable columnVariable = SparqlBuilder.var(alias(column.getName()).getQueryString());

    Expression<?> coalesce = Expressions.coalesce(aliases.toArray(new Operand[0]));
    patterns.add(Expressions.bind(coalesce, columnVariable));
    GraphPattern pattern = GraphPatterns.and(patterns.toArray(new GraphPattern[0]));

    if (referenceRequired) {
      Expression<?> bound = Expressions.bound(columnVariable);
      pattern.filter(bound);
    }

    return pattern;
  }

  public RdfObject alias(String name) {
    return () -> relatedTable.getTableName() + "_" + name;
  }

  private RdfPredicate predicate(String semantic) {
    return () -> semantic;
  }
}
