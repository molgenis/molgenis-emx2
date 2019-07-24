import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Query;
import org.molgenis.Table;
import org.molgenis.beans.QueryBean;
import org.molgenis.Row;
import org.molgenis.beans.TableBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Column.Type.DECIMAL;
import static org.molgenis.Column.Type.INT;
import static org.molgenis.Column.Type.STRING;
import static org.molgenis.emx2.web.JsonQueryMapper.jsonToQuery;
import static org.molgenis.emx2.web.JsonQueryMapper.queryToJson;
import static org.molgenis.emx2.web.JsonRowMapper.jsonToRow;
import static org.molgenis.emx2.web.JsonRowMapper.jsonToRows;
import static org.molgenis.emx2.web.JsonRowMapper.rowsToJson;

public class TestJson {

  @Test
  public void testJsonToRow() throws MolgenisException {

    Table t = new TableBean(null, "Person");
    t.addColumn("FirstName", STRING);
    t.addColumn("Age", INT);
    t.addColumn("Weight", DECIMAL);

    String json = "{\"FirstName\":\"Donald\", \"Age\":50, \"Weight\":15.4}";
    Any any = JsonIterator.deserialize(json);
    org.molgenis.Row r = jsonToRow(t, any);

    try {
      String malformed = "{\"FirstName\":\"Donald\", \"Age\":\"blaat\"}";
      any = JsonIterator.deserialize(malformed);
      r = jsonToRow(t, any);
    } catch (Exception e) {
      System.out.println(e);
    }

    try {
      String malformed = "{\"FirstName\":[\"Donald\",\"Duck\"], \"Age\":50}";
      any = JsonIterator.deserialize(malformed);
      r = jsonToRow(t, any);
    } catch (Exception e) {
      System.out.println(e);
    }

    try {
      String malformed = "{\"FirstName\":\"Donald\", \"Age\":50, \"Weight\":\"blaat\"}";
      any = JsonIterator.deserialize(malformed);
      r = jsonToRow(t, any);
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  @Test
  public void testRowsToJson() {
    List<org.molgenis.Row> rows = new ArrayList<>();
    rows.add(new Row().setString("FirstName", "Donald"));

    String json = rowsToJson(rows);
    System.out.println(json);
    List<org.molgenis.Row> rows2 = jsonToRows(json);
    String json2 = rowsToJson(rows2);
    System.out.println(json2);

    assertEquals(json, json2);
  }

  @Test
  public void testQuery() {
    Query q = new QueryBean();

    q.select("FirstName")
        .select("LastName")
        .expand("Father")
        .include("FirstName")
        .where("Age")
        .eq(50)
        .or("Age")
        .eq(60)
        .and("Father", "LastName")
        .eq("Blaat")
        .asc("LastName")
        .asc("FirstName");

    String json1 = queryToJson(q);

    System.out.println(q);

    System.out.println(json1);

    Query q2 = jsonToQuery(json1, new QueryBean());
    String json2 = queryToJson(q2);

    System.out.println(q2);
    System.out.println(json2);

    assertEquals(json1, json2);
  }
}
