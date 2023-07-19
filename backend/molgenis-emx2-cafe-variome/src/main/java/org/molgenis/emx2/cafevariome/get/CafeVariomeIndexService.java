package org.molgenis.emx2.cafevariome.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.util.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import spark.Request;

public class CafeVariomeIndexService {

  private static ObjectMapper jsonMapper =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

  public static IndexResponse index(Request request, List<Table> tables) throws Exception {
    IndexResponse indexResponse = new IndexResponse();
    indexResponse.setSource_id("1");

    Map<String, List> attributeValues = new HashMap<>();
    Map<String, String> attributesDisplayNames = new HashMap<>();
    Map<Object, Object> valueDisplayNames = new HashMap<>();

    for (Table t : tables) {
      List<Row> rows = t.getSchema().retrieveSql("Select * from \"" + t.getName() + "\"");
      for (Row row : rows) {
        for (String colName : row.getColumnNames()) {
          if (colName.endsWith("_TEXT_SEARCH_COLUMN")) {
            continue;
          }
          Column column = Column.column(colName);
          Object value = row.get(column);
          if (value != null) {
            if (!attributeValues.containsKey(colName) && !valueDisplayNames.containsKey(value)) {
              attributeValues.put(colName, new ArrayList<>());
              attributesDisplayNames.put(colName, colName);
            }
            if (!valueDisplayNames.containsKey(value)) {
              attributeValues.get(colName).add(value);
              valueDisplayNames.put(value, value);
            }
          }
        }
      }
    }

    Map<String, Object[]> attributeValuesPrim = new HashMap<>();
    for (String key : attributeValues.keySet()) {
      attributeValuesPrim.put(key, attributeValues.get(key).toArray(new Object[0]));
    }

    indexResponse.setAttributes_values(attributeValuesPrim);
    indexResponse.setValues_display_names(valueDisplayNames);
    indexResponse.setAttributes_display_names(attributesDisplayNames);

    return indexResponse;
  }
}
