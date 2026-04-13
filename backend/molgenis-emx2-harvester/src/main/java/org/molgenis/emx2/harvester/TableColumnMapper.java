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
import org.molgenis.emx2.harvester.mappers.ColumnMapper;

public class TableColumnMapper implements ColumnMapper {

  private final Variable startingPoint;
  private final Column relatedColumn;
  private final TableMetadata relatedTable;
  private final boolean referenceRequired;

  public TableColumnMapper(Variable startingPoint, Reference reference, SchemaMetadata schema) {
    this.startingPoint = startingPoint;
    this.relatedTable = schema.getTableMetadata(reference.getTargetTable());
    this.relatedColumn = relatedTable.getColumn(reference.getTargetColumn());
    this.referenceRequired = reference.toPrimitiveColumn().isRequired();
  }

  @Override
  public List<Projectable> getSelectors() {
    return Collections.emptyList();
  }

  @Override
  public List<GraphPattern> getPattern() {
    if (relatedColumn.getSemantics() == null) {
      throw new MolgenisException(
          "Unable to resolve reference semantics for " + relatedColumn.getName());
    }

    List<GraphPattern> patterns = new ArrayList<>();

    for (Column pkey : relatedTable.getPrimaryKeyColumns()) {
      List<GraphPattern> predicate = predicateForKey(startingPoint, pkey);
      patterns.addAll(predicate);
    }

    return patterns;
  }

  private List<GraphPattern> predicateForKey(Variable variable, Column column) {
    if (column.getSemantics().length == 1) {
      String semantic = column.getSemantics()[0];
      Variable alias = SparqlBuilder.var(alias(column.getName()).getQueryString());
      TriplePattern pattern = GraphPatterns.tp(variable, predicate(semantic), alias);

      if (referenceRequired) {
        return List.of(pattern);
      } else {
        return List.of(pattern.optional());
      }
    }

    List<GraphPattern> patterns = new ArrayList<>();
    List<Operand> aliases = new ArrayList<>();
    String columnAlias =
        startingPoint.getVarName() + "_" + column.getQualifiedName().replace(".", "_");
    for (int i = 0; i < column.getSemantics().length; i++) {
      String semantic = column.getSemantics()[i];
      Variable alias = SparqlBuilder.var(alias(columnAlias + i).getQueryString());

      patterns.add(variable.has(predicate(semantic), alias).optional());
      aliases.add(alias);
    }

    Variable columnVariable = SparqlBuilder.var(alias(columnAlias).getQueryString());

    Expression<?> coalesce = Expressions.coalesce(aliases.toArray(new Operand[0]));
    patterns.add(Expressions.bind(coalesce, columnVariable));

    if (referenceRequired) {
      Expression<?> bound = Expressions.bound(columnVariable);
      patterns.add(() -> "FILTER ( " + bound.getQueryString() + " )");
    }

    return patterns;
  }

  public RdfObject alias(String name) {
    return () -> relatedTable.getTableName() + "_" + name;
  }

  private RdfPredicate predicate(String semantic) {
    return () -> semantic;
  }
}
