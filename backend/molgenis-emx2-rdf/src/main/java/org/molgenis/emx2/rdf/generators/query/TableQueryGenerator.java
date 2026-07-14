package org.molgenis.emx2.rdf.generators.query;

import java.util.*;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.generators.query.generators.ArrayColumnSparqlQueryGenerator;
import org.molgenis.emx2.rdf.generators.query.generators.ColumnSparqlQueryGenerator;
import org.molgenis.emx2.rdf.generators.query.generators.LiteralColumnSparqlQueryGenerator;
import org.molgenis.emx2.rdf.generators.query.generators.ReferenceColumnSparqlQueryGenerator;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;

public class TableQueryGenerator implements QueryGenerator {

  private static final Variable ANY_PREDICATE = SparqlBuilder.var("anyPredicate");
  private static final Variable ANY_OBJECT = SparqlBuilder.var("anyObject");
  private static final Variable TYPE_VARIABLE = SparqlBuilder.var("_type_");
  private static final Variable SUBJECT_VARIABLE = SparqlBuilder.var("_subject_");

  @Override
  public String generate(TableMetadata tableMetadata) {
    List<Projectable> selectors = new ArrayList<>();
    List<GraphPattern> whereClauses = new ArrayList<>();
    List<Groupable> groups = new ArrayList<>();

    selectors.add(SUBJECT_VARIABLE);
    groups.add(SUBJECT_VARIABLE);

    for (Column column : tableMetadata.getColumns()) {
      if (hasSemantics(column.getSemantics())) {
        continue;
      }

      ColumnSparqlQueryGenerator mapper;
      if (column.isReference()) {
        mapper = new ReferenceColumnSparqlQueryGenerator(SUBJECT_VARIABLE, column);
      } else if (column.isArray()) {
        mapper = new ArrayColumnSparqlQueryGenerator(SUBJECT_VARIABLE, column);
      } else {
        mapper = new LiteralColumnSparqlQueryGenerator(SUBJECT_VARIABLE, column);
      }

      selectors.addAll(mapper.getSelectors());
      whereClauses.addAll(mapper.getPatterns());
      groups.addAll(mapper.getGroupBy());
    }

    SelectQuery query = setupQuery(tableMetadata);
    if (hasSemantics(tableMetadata.getSemantics())) {
      anchorTableVar(query);
    } else {
      addTableTypeSemantics(tableMetadata, query);
    }

    return query
        .select(selectors.toArray(new Projectable[0]))
        .where(whereClauses.toArray(new GraphPattern[0]))
        .groupBy(groups.toArray(new Groupable[0]))
        .getQueryString();
  }

  private static boolean hasSemantics(String[] semantics) {
    return semantics == null || semantics.length == 0;
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
   * @param query the query to add the anchor pattern to.
   */
  private static void anchorTableVar(SelectQuery query) {
    query.where(GraphPatterns.tp(SUBJECT_VARIABLE, ANY_PREDICATE, ANY_OBJECT));
  }

  private static void addTableTypeSemantics(TableMetadata tableMetadata, SelectQuery select) {
    String[] tableSemantics = tableMetadata.getSemantics();
    if (tableSemantics.length == 1) {
      select.where(SUBJECT_VARIABLE.isA(() -> tableSemantics[0]));
    } else if (tableSemantics.length > 1) {
      RdfValue[] semantics =
          Arrays.stream(tableSemantics)
              .map(semantic -> (RdfValue) () -> semantic)
              .toArray(RdfValue[]::new);

      select
          .where(SUBJECT_VARIABLE.isA(TYPE_VARIABLE))
          .values(value -> value.variables(TYPE_VARIABLE).values(semantics));
    }
  }

  private SelectQuery setupQuery(TableMetadata tableMetadata) {
    SelectQuery select = Queries.SELECT();
    NamespaceMapper namespaceMapper = new NamespaceMapper(tableMetadata.getSchema());
    namespaceMapper.getAllNamespaces().forEach(select::prefix);
    return select;
  }
}
