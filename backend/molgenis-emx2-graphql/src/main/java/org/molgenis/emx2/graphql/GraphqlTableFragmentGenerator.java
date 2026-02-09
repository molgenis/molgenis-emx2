package org.molgenis.emx2.graphql;

import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;

public class GraphqlTableFragmentGenerator {
  private GraphqlTableFragmentGenerator() {}

  public static Map<String, String> generate(Schema schema) {
    GraphqlTableFieldFactory tableField = new GraphqlTableFieldFactory(schema);
    Map<String, String> fragments = new LinkedHashMap<>();
    for (TableMetadata table : schema.getMetadata().getTables()) {
      for (int depth = 1; depth <= 3; depth++) {
        fragments.put(
            table.getIdentifier() + "AllFields" + depth,
            tableField.getGraphqlFragments(table, depth));
      }
      fragments.put(
          table.getIdentifier() + "AllFields",
          tableField.getGraphqlFragments(table, 1, table.getIdentifier() + "AllFields"));
    }
    return fragments;
  }
}
