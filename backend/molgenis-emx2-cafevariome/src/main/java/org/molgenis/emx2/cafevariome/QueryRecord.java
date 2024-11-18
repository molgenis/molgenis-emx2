package org.molgenis.emx2.cafevariome;

import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.filter.FilterConceptVP;

public class QueryRecord {

  private static final String TABLE_NAME = "Individuals";

  public static Object post(Schema schema, CafeVariomeQuery query) {

    Table table = schema.getTable(TABLE_NAME);

    List<String> filters = parseFilters(query);
    int count = QueryEntryType.doCountQuery(table, filters);

    return switch (query.advanced.granularity()) {
      case BOOLEAN -> new Response(null, null, count > 0);
      case COUNT -> new Response(count, new Range(count, count), count > 0);
      default ->
          throw new MolgenisException(
              "Not implemented granularity: " + query.advanced.granularity());
    };
  }

  private static List<String> parseFilters(CafeVariomeQuery query) {
    String ageAtDiagFilter =
        FilterConceptVP.AGE_AT_DIAG
            .getGraphQlQuery()
            .formatted(
                "\"P" + query.subject.ageFirstDiagnosis.min() + "Y\"",
                "\"P" + query.subject.ageFirstDiagnosis.max() + "Y\"");

    String diseaseTermFilter =
        FilterConceptVP.DISEASE.getGraphQlQuery().formatted(query.hpo.get(0).terms().get(0));

    return List.of(ageAtDiagFilter, diseaseTermFilter);
  }
}
