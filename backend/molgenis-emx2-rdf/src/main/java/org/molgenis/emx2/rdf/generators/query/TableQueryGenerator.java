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
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.DefaultNamespace;
import org.molgenis.emx2.rdf.generators.query.mappers.CollectionColumnMapper;
import org.molgenis.emx2.rdf.generators.query.mappers.ColumnMapper;
import org.molgenis.emx2.rdf.generators.query.mappers.PlainColumnMapper;
import org.molgenis.emx2.rdf.generators.query.mappers.ReferenceMapper;

public class TableQueryGenerator {

  public SelectQuery generate(TableMetadata tableMetadata) {
    List<GraphPattern> whereClauses = new ArrayList<>();
    List<Projectable> selectors = new ArrayList<>();
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

      whereClauses.addAll(mapper.getPattern());
      selectors.addAll(mapper.getSelectors());
      groups.addAll(mapper.getGroupBy());
    }

    return addTableTypeSemantics(tableMetadata, tableVar, setupQuery())
        .where(whereClauses.toArray(new GraphPattern[0]))
        .select(selectors.toArray(new Projectable[0]))
        .groupBy(groups.toArray(new Groupable[0]));
  }

  private static SelectQuery addTableTypeSemantics(
      TableMetadata tableMetadata, Variable tableVar, SelectQuery select) {
    String[] tableSemantics = tableMetadata.getSemantics();
    if (tableSemantics != null && tableSemantics.length > 0) {
      GraphPatternNotTriples union = GraphPatterns.union();
      Arrays.stream(tableSemantics)
          .map(SparqlBuilder::var)
          .map(tableVar::isA)
          .forEach(union::union);
      select.where(union);
    }
    return select;
  }

  private SelectQuery setupQuery() {
    SelectQuery select = Queries.SELECT();
    DefaultNamespace.streamAll().forEach(select::prefix);
    return select;
  }
}
