package org.molgenis.emx2.cafevariome;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.List;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.QueryBuilder;
import org.molgenis.emx2.beaconv2.filter.FilterConceptVP;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class QueryRecord {

  public static Object post(Schema schema, CafeVariomeQuery query) {

    Table table = schema.getTable("Individuals");

    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());

    String ageAtDiagFilter =
        FilterConceptVP.AGE_AT_DIAG
            .getGraphQlQuery()
            .formatted(
                "P" + query.subject.ageFirstDiagnosis.min() + "Y",
                "P" + query.subject.ageFirstDiagnosis.max() + "Y");

    String diseaseTermFilter =
        FilterConceptVP.DISEASE.getGraphQlQuery().formatted(query.hpo.get(0).terms().get(0));

    List<String> filters = List.of(ageAtDiagFilter, diseaseTermFilter);

    String graphQlQuery = new QueryBuilder(table).addAllColumns(2).addFilters(filters).getQuery();

    ExecutionResult result = graphQL.execute(graphQlQuery);

    return result.getData();
  }
}
