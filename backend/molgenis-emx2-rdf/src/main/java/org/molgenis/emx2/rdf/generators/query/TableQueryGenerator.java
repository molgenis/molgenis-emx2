package org.molgenis.emx2.rdf.generators.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.DefaultNamespace;
import org.molgenis.emx2.rdf.generators.query.mappers.CollectionColumnMapper;
import org.molgenis.emx2.rdf.generators.query.mappers.ColumnMapper;
import org.molgenis.emx2.rdf.generators.query.mappers.PlainColumnMapper;
import org.molgenis.emx2.rdf.generators.query.mappers.ReferenceMapper;

public class TableQueryGenerator {

  private static final Variable ANY_PREDICATE = SparqlBuilder.var("anyPredicate");
  private static final Variable ANY_OBJECT = SparqlBuilder.var("anyObject");

  public SelectQuery generate(TableMetadata tableMetadata) {
    List<Projectable> selectors = new ArrayList<>();
    List<GraphPattern> whereClauses = new ArrayList<>();
    List<Groupable> groups = new ArrayList<>();

    Variable tableVar = SparqlBuilder.var(tableMetadata.getTableName());
    selectors.add(tableVar);
    groups.add(tableVar);

    for (Column column : tableMetadata.getColumns()) {
      if (column.getSemantics() == null || column.getSemantics().length == 0) {
        continue;
      }

      ColumnMapper mapper;
      if (column.isReference()) {
        mapper = new ReferenceMapper(tableVar, column);
      } else if (Boolean.TRUE.equals(column.isArray())) {
        mapper = new CollectionColumnMapper(tableVar, column);
      } else {
        mapper = new PlainColumnMapper(tableVar, column);
      }

      selectors.addAll(mapper.getSelectors());
      whereClauses.addAll(mapper.getPattern());
      groups.addAll(mapper.getGroupBy());
    }

    SelectQuery query = setupQuery();
    if (tableMetadata.getSemantics() != null) {
      addTableTypeSemantics(tableMetadata, tableVar, query);
    } else {
      anchorTableVar(tableVar, query);
    }

    return query
        .select(selectors.toArray(new Projectable[0]))
        .where(whereClauses.toArray(new GraphPattern[0]))
        .groupBy(groups.toArray(new Groupable[0]));
  }

  /**
   * Anchors a table variable in the query scope by adding a required triple pattern.
   *
   * <p>When a query consists entirely of OPTIONAL clauses, or the WHERE clauses start with an
   * OPTIONAL clause, the table variable ({@code ?Pet}, {@code ?Resource}, etc.) is never bound
   * before the optional clauses are evaluated. This causes potential hits that only match later
   * optionals to be silently dropped from results.
   *
   * <p>Adding {@code ?tableVar ?anyPredicate ?anyObject} as a required triple ensures the variable
   * is bound to all matching subjects in the graph before any OPTIONAL clauses are evaluated.
   *
   * @param tableVar the variable to anchor, typically representing the primary subject of the
   *     query.
   * @param query the query to add the anchor pattern to.
   */
  private static void anchorTableVar(Variable tableVar, SelectQuery query) {
    query.where(GraphPatterns.tp(tableVar, ANY_PREDICATE, ANY_OBJECT));
  }

  private static void addTableTypeSemantics(
      TableMetadata tableMetadata, Variable tableVar, SelectQuery select) {
    String[] tableSemantics = tableMetadata.getSemantics();
    if (tableSemantics.length == 1) {
      select.where(tableVar.isA(() -> tableSemantics[0]));
    } else if (tableSemantics.length > 1) {
      GraphPatternNotTriples union = GraphPatterns.union();
      Arrays.stream(tableSemantics)
          .map(semantic -> (RdfObject) () -> semantic)
          .map(tableVar::isA)
          .forEach(union::union);
      select.where(union);
    }
  }

  private SelectQuery setupQuery() {
    SelectQuery select = Queries.SELECT();
    DefaultNamespace.streamAll().forEach(select::prefix);
    return select;
  }
}
