package org.molgenis.emx2.cafevariome;

import java.util.List;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.filter.FilterConceptVP;

public class QueryRecord {

  private static final String TABLE_NAME = "Individuals";

  public static Object post(Schema schema, CafeVariomeQuery query) {

    Table table = schema.getTable(TABLE_NAME);

    String ageAtDiagFilter =
        FilterConceptVP.AGE_AT_DIAG
            .getGraphQlQuery()
            .formatted(
                "\"P" + query.subject.ageFirstDiagnosis.min() + "Y\"",
                "\"P" + query.subject.ageFirstDiagnosis.max() + "Y\"");

    String diseaseTermFilter =
        FilterConceptVP.DISEASE.getGraphQlQuery().formatted(query.hpo.get(0).terms().get(0));

    List<String> filters = List.of(ageAtDiagFilter, diseaseTermFilter);
    int count = QueryEntryType.doCountQuery(table, filters);

    return new Response(count, new Range(count, count), count > 0);
  }
}
