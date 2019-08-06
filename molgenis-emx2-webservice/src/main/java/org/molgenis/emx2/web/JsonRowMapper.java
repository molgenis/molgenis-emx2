package org.molgenis.emx2.web;

import com.jsoniter.JsonIterator;
import com.jsoniter.ValueType;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.TypeLiteral;
import org.molgenis.*;
import org.molgenis.Row;

import java.util.*;

public class JsonRowMapper {

  private JsonRowMapper() {
    // hide constructor
  }

  public static String rowToJson(Row row) {

    Map<String, Object> map = row.getValueMap();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof UUID)
        map.put(entry.getKey(), ((UUID) entry.getValue()).toString());
    }
    return JsonStream.serialize(map);
  }

  public static List<org.molgenis.Row> jsonToRows(String json) {
    ArrayList<org.molgenis.Row> rows = new ArrayList<>();

    List<Map<String, Object>> data =
        JsonIterator.deserialize(json, new TypeLiteral<ArrayList<Map<String, Object>>>() {});

    for (Map<String, Object> values : data) {
      rows.add(new Row(values));
    }

    return rows;
  }

  public static org.molgenis.Row jsonToRow(String json) {
    Map<String, Object> map =
        JsonIterator.deserialize(json, new TypeLiteral<Map<String, Object>>() {});
    return new Row(map);
  }

  public static org.molgenis.Row jsonToRow(Table t, Any json) throws MolgenisException {
    org.molgenis.Row r = new Row();
    for (Column c : t.getColumns()) {
      try {
        switch (c.getType()) {
          case INT:
            r.setInt(c.getName(), json.get(c.getName()).toInt());
            break;
          case DECIMAL:
            r.setDecimal(c.getName(), json.get(c.getName()).toDouble());
            break;
          case STRING:
            if (ValueType.STRING.equals(json.get(c.getName()).valueType())) {
              r.setString(c.getName(), json.get(c.getName()).toString());
              break;
            } else throw new IllegalArgumentException();
          default:
            throw new UnsupportedOperationException(
                "data type " + c.getType() + " not yet implemented");
        }

      } catch (Exception e) {
        throw new MolgenisException(
            String.format(
                "Malformed json: expected '%s' to be of type '%s' but found '%s'. Total object: %s",
                c.getName(), c.getType(), json.get(c.getName()).valueType(), json),
            e);
      }
    }
    return r;
  }

  public static String rowsToJson(List<org.molgenis.Row> rows) {
    Map<String, Object>[] values = new Map[rows.size()];
    int i = 0;
    for (org.molgenis.Row r : rows) {
      Map<String, Object> map = r.getValueMap();
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        if (entry.getValue() instanceof UUID)
          map.put(entry.getKey(), ((UUID) entry.getValue()).toString());
      }
      values[i++] = r.getValueMap();
    }
    return JsonStream.serialize(values);
  }
}
