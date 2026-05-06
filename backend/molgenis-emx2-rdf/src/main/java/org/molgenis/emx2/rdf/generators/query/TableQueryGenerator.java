package org.molgenis.emx2.rdf.generators.query;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.DefaultNamespace;
import org.molgenis.emx2.rdf.generators.query.mappers.CollectionColumnMapper;
import org.molgenis.emx2.rdf.generators.query.mappers.ColumnMapper;
import org.molgenis.emx2.rdf.generators.query.mappers.PlainColumnMapper;
import org.molgenis.emx2.rdf.generators.query.mappers.ReferenceMapper;

public class TableQueryGenerator {

  public SelectQuery generate(TableMetadata tableMetadata) {
    SelectQuery select = setupQuery();

    Variable tableVar = SparqlBuilder.var(tableMetadata.getTableName());
    select.select(tableVar);
    select.groupBy(tableVar);

    for (Column column : tableMetadata.getColumns()) {
      if (column.getSemantics() == null || column.getSemantics().length == 0) {
        continue;
      }

      ColumnMapper mapper;
      if (column.isReference()) {
        mapper = new ReferenceMapper(tableVar, column);
      } else if (column.isArray()) {
        mapper = new CollectionColumnMapper(tableVar, column);
      } else {
        mapper = new PlainColumnMapper(tableVar, column);
      }

      mapper.getSelectors().forEach(select::select);
      mapper.getPattern().forEach(select::where);
      mapper.getGroupBy().forEach(select::groupBy);
    }

    return select;
  }

  private SelectQuery setupQuery() {
    SelectQuery select = Queries.SELECT();
    DefaultNamespace.streamAll().forEach(select::prefix);
    return select;
  }
}
