package org.molgenis.emx2.beaconv2.responses.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DatasetsResponse {
  Collection[] collections;

  public DatasetsResponse(Schema schema) {
    List<Collection> cList = new ArrayList<>();
    for (Table t : schema.getTablesSorted()) {
      Collection c =
          new Collection(
              t.getName(), t.getName(), "2022-01-01T00:00:00+00:00", "2022-01-01T00:00:00+00:00");
      cList.add(c);
    }
    this.collections = cList.toArray(new Collection[cList.size()]);
  }
}
