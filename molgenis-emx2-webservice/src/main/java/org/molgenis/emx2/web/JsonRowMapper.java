package org.molgenis.emx2.web;

import com.jsoniter.JsonIterator;
import com.jsoniter.ValueType;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.TypeLiteral;
import org.molgenis.*;
import org.molgenis.beans.RowBean;

import java.util.*;

public class JsonRowMapper {

  public static List<Row> jsonToRows(String json) {
    ArrayList<Row> rows = new ArrayList<>();

    List<Map<String, Object>> data =
        JsonIterator.deserialize(json, new TypeLiteral<ArrayList<Map<String, Object>>>() {});

    for (Map<String, Object> values : data) {
      rows.add(new RowBean(values));
    }

    return rows;
  }

  public static Row jsonToRow(Table t, Any json) throws MolgenisException {
    Row r = new RowBean();
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

  public static String rowsToJson(List<Row> rows) {
    Map<String, Object>[] values = new Map[rows.size()];
    int i = 0;
    for (Row r : rows) {
      Map<String, Object> map = r.getValueMap();
      for (String name : map.keySet()) {
        if (map.get(name) instanceof UUID) map.put(name, ((UUID) map.get(name)).toString());
      }
      values[i++] = r.getValueMap();
    }
    return JsonStream.serialize(values);
  }
}
