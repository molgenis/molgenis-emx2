package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.ArrayList;
import java.util.List;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DatasetsResponse {
  Collection[] collections;

  public DatasetsResponse(Request request, java.util.Collection<String> schemaNames) {
    List<Collection> cList = new ArrayList<>();

    //    for (Table t : schema.getTablesSorted()) {
    //      Collection c =
    //          new Collection(
    //              t.getName(), t.getName(), "2022-01-01T00:00:00+00:00",
    // "2022-01-01T00:00:00+00:00");
    //      cList.add(c);
    //    }

    for (String schema : schemaNames) {
      Collection c =
          new Collection(schema, schema, "2022-01-01T00:00:00+00:00", "2022-01-01T00:00:00+00:00");
      cList.add(c);
    }

    this.collections = cList.toArray(new Collection[cList.size()]);
  }
}
