package org.molgenis.emx2.cafevariome.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.util.*;
import org.molgenis.emx2.*;
import spark.Request;

public class CafeVariomeIndexService {

  private static ObjectMapper jsonMapper =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

  public static IndexResponse index(Request request, List<Table> tables) throws Exception {
    IndexResponse indexResponse = new IndexResponse();

    Map<String, List> attributeValues = new HashMap<>();
    Map<String, String> attributesDisplayNames = new HashMap<>();
    Map<Object, Object> valueDisplayNames = new HashMap<>();

    for (Table t : tables) {
      TableMetadata metadata = t.getMetadata();
      // String result = t.query().select(s("Individuals",
      // s("id"),s("phenotypes",s("name")))).where(f("name", Operator.EQUALS,
      // "blaat")).retrieveJSON();
      // graphl {Individuals{id,phenotypes{name}}}

      // List<Map<String,?>> map = new ObjectMapper().readValue(result, List.class);
      List<Row> rows = t.getSchema().retrieveSql("Select distinct * from \"" + t.getName() + "\"");
      for (Row row : rows) {
        for (Column column : metadata.getColumns()) {
          if (column.isPrimaryKey()) {
            continue;
          }
          String colName = column.getName();
          String value = row.getString(colName);
          if (value != null) {

            // FIXME: values should be types ('String', 'Array', 'Decimal' etc so we only split on
            // Arrays!
            for (String valSplit : value.split(",", -1)) {
              if (!attributeValues.containsKey(colName)
                  && !valueDisplayNames.containsKey(valSplit)) {
                attributeValues.put(colName, new ArrayList<>());
                attributesDisplayNames.put(colName, colName);
              }
              if (!valueDisplayNames.containsKey(valSplit)) {
                attributeValues.get(colName).add(valSplit);
                valueDisplayNames.put(valSplit, valSplit);
              }
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
