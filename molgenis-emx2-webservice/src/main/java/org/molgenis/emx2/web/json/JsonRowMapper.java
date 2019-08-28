package org.molgenis.emx2.web.json;

import com.jsoniter.JsonIterator;
import com.jsoniter.ValueType;
import com.jsoniter.any.Any;
import com.jsoniter.spi.TypeLiteral;
import org.molgenis.Row;
import org.molgenis.Column;
import org.molgenis.TableMetadata;
import org.molgenis.utils.MolgenisException;

import java.util.*;

public class JsonRowMapper {

  private JsonRowMapper() {
    // hide constructor
  }

  public static List<Row> jsonToRows(String json) {
    ArrayList<Row> rows = new ArrayList<>();

    List<Map<String, Object>> data =
        JsonIterator.deserialize(json, new TypeLiteral<ArrayList<Map<String, Object>>>() {});

    for (Map<String, Object> values : data) {
      rows.add(new Row(values));
    }

    return rows;
  }

  public static Row jsonToRow(String json) {
    Map<String, Object> map =
        JsonIterator.deserialize(json, new TypeLiteral<Map<String, Object>>() {});
    return new Row(map);
  }

  public static Row jsonToRow(TableMetadata t, Any json) throws MolgenisException {
    Row r = new Row();
    for (Column c : t.getColumns()) {
      try {
        switch (c.getType()) {
          case INT:
            r.setInt(c.getColumnName(), json.get(c.getColumnName()).toInt());
            break;
          case DECIMAL:
            r.setDecimal(c.getColumnName(), json.get(c.getColumnName()).toDouble());
            break;
          case STRING:
            if (ValueType.STRING.equals(json.get(c.getColumnName()).valueType())) {
              r.setString(c.getColumnName(), json.get(c.getColumnName()).toString());
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
                c.getColumnName(), c.getType(), json.get(c.getColumnName()).valueType(), json),
            e);
      }
    }
    return r;
  }
}
