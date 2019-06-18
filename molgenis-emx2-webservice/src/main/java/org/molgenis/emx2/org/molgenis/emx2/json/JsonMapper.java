package org.molgenis.emx2.org.molgenis.emx2.json;

import com.jsoniter.ValueType;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import org.molgenis.Column;
import org.molgenis.MolgenisException;
import org.molgenis.Row;
import org.molgenis.Table;
import org.molgenis.beans.RowBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JsonMapper {

  public static Row[] map(Table t, Map<String, Any> json) throws MolgenisException {
    Row[] rows = new Row[json.size()];
    int i = 0;
    for (Any a : json.values()) {
      rows[i++] = map(t, a);
    }
    return rows;
  }

  public static Row map(Table t, Any json) throws MolgenisException {
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

  public static String map(List<Row> rows) {
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
