package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.junit.Test;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.web.json.JsonQueryMapper;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.beans.QueryBean;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.web.json.JsonSchemaMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.web.json.JsonRowMapper.jsonToRow;
import static org.molgenis.emx2.web.json.JsonRowMapper.jsonToRows;
import static org.molgenis.emx2.web.json.JsonMapper.rowsToJson;
import static org.molgenis.emx2.ColumnType.DECIMAL;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.STRING;

public class TestJson {

  @Test
  public void testSchemaToJson() throws MolgenisException, IOException {

    SchemaMetadata s = new SchemaMetadata("test");
    ProductComponentPartsExample.create(s);
    String json = JsonSchemaMapper.schemaToJson(s);
    System.out.println("json:\n" + json);

    SchemaMetadata s2 = JsonSchemaMapper.jsonToSchema(json);
    CompareTools.assertEquals(s, s2);
  }

  @Test
  public void testJsonToRow() throws MolgenisException {

    TableMetadata t = new TableMetadata(null, "Person");
    t.addColumn("FirstName", STRING);
    t.addColumn("Age", INT);
    t.addColumn("Weight", DECIMAL);

    String json = "{\"FirstName\":\"Donald\", \"Age\":50, \"Weight\":15.4}";
    Any any = JsonIterator.deserialize(json);
    Row r = jsonToRow(t, any);
    assertEquals(new Integer(50), r.getInteger("Age"));

    try {
      String malformed = "{\"FirstName\":\"Donald\", \"Age\":\"blaat\"}";
      any = JsonIterator.deserialize(malformed);
      r = jsonToRow(t, any);
      fail("Should error on Age being String instead of a Integer");
    } catch (Exception e) {
      System.out.println("Error correctely: " + e);
    }

    try {
      String malformed = "{\"FirstName\":[\"Donald\",\"Duck\"], \"Age\":50}";
      any = JsonIterator.deserialize(malformed);
      r = jsonToRow(t, any);
      fail("Should error on 'FirstName' begin as String[] instead of a String");
    } catch (Exception e) {
      System.out.println("Error correctly: " + e);
    }

    try {
      String malformed = "{\"FirstName\":\"Donald\", \"Age\":50, \"Weight\":\"blaat\"}";
      any = JsonIterator.deserialize(malformed);
      r = jsonToRow(t, any);
      fail("Should error on 'Weight' as String[] instead of a Double");
    } catch (Exception e) {
      System.out.println("Error correctly:" + e);
    }
  }

  @Test
  public void testRowsToJson() throws JsonProcessingException {
    List<Row> rows = new ArrayList<>();
    rows.add(new Row().setString("FirstName", "Donald"));

    String json = rowsToJson(rows);
    System.out.println(json);
    List<Row> rows2 = jsonToRows(json);
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
        .select("FirstName")
        .where("Age", EQUALS, 50)
        .or("Age", EQUALS, 60)
        .and("Father/LastName", EQUALS, "Blaat")
        .asc("LastName")
        .asc("FirstName");

    String json1 = JsonQueryMapper.queryToJson(q);

    System.out.println(q);

    System.out.println(json1);

    Query q2 = JsonQueryMapper.jsonToQuery(json1, new QueryBean());
    String json2 = JsonQueryMapper.queryToJson(q2);

    System.out.println(q2);
    System.out.println(json2);

    assertEquals(json1, json2);
  }
}
