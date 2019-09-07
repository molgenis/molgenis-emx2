package org.molgenis.emx2.web.json;

import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.ArrayList;
import java.util.List;

public class Schema {
  public String name;
  public List<Table> tables = new ArrayList<>();

  public Schema() {};

  public Schema(SchemaMetadata schema) throws MolgenisException {
    this.name = schema.getName();
    for (String tableName : schema.getTableNames()) {
      tables.add(new Table(schema.getTableMetadata(tableName)));
    }
  }

  public SchemaMetadata getSchemaMetadata() throws MolgenisException {
    SchemaMetadata s = new SchemaMetadata(name);
    for (Table t : tables) {
      s.createTable(t.getTableMetadata(s));
    }
    return s;
  }
}
