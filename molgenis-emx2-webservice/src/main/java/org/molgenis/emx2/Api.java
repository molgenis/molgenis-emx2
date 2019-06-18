package org.molgenis.emx2;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Row;
import org.molgenis.Table;
import org.molgenis.emx2.org.molgenis.emx2.json.JsonMapper;

import java.util.List;
import java.util.Map;

public class Api {
  private Database db;

  public Api(Database db) {
    this.db = db;
  }

  /**
   * expects { "tableName": { "update", {"key1":{values}, "key2":{values}} //deletes } }
   *
   * <p>returns { "message":"message string" "messages":[] }
   */
  public void patch(String input) {
    Any any = JsonIterator.deserialize(input);
    Map<String, Any> tables = any.asMap();
    for (String table : tables.keySet()) {
      Map<String, Any> updates = tables.get(table).asMap();
      // Row rows = JsonMapper.map(t, any);
    }
  }

  /**
   * expects { "expand" : { "firstName", "lastName", "father": { "firstName" }}, "where": {
   * "FirstName": {"eq":} }}
   */
  public String query(String input) {
    throw new UnsupportedOperationException();
  }

  public String list(Table t) throws MolgenisException {
    List<Row> result = db.query(t.getName()).retrieve();
    return JsonMapper.map(result);
  }
}
