package org.molgenis.emx2.json;

import static org.molgenis.emx2.TableMetadata.table;

import java.util.*;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Setting;
import org.molgenis.emx2.TableMetadata;

public class Schema {
  private List<Table> tables = new ArrayList<>();
  private List<Setting> settings = new ArrayList<>();

  public Schema() {
    // for json serialization
  }

  public Schema(SchemaMetadata schema) {
    this(schema, false);
  }

  public Schema(SchemaMetadata schema, boolean minimal) {
    this.settings = List.of(schema.getSettings().toArray(new Setting[0]));
    List<TableMetadata> list = new ArrayList<>();
    list.addAll(schema.getTablesIncludingExternal());
    // deterministic order is important for all kinds of comparisons
    Collections.sort(list);
    // add these tables
    for (TableMetadata t : list) {
      tables.add(new Table(schema, t, minimal));
    }
  }

  public SchemaMetadata getSchemaMetadata() {
    SchemaMetadata s = new SchemaMetadata();
    s.setSettings(this.settings);
    for (Table t : this.tables) {
      TableMetadata tm = s.create(table(t.getName()));
      tm.setInherit(t.getInherit());
      tm.setSettings(t.getSettings());
      tm.setOldName(t.getOldName());
      if (t.getTableType() != null) {
        tm.setTableType(t.getTableType());
      }
      if (t.getDrop()) tm.drop();
      tm.setSemantics(t.getSemantics());
      tm.setDescription(t.getDescription());
      for (Column c : t.getColumns()) {
        int i = 1;
        if (!c.isInherited()) {
          // we remove clearly inherited columns here
          org.molgenis.emx2.Column cm = c.getColumnMetadata(tm);
          tm.add(cm);
        }
      }
      tm.setTableType(t.getTableType());
    }
    return s;
  }

  public List<Table> getTables() {
    return tables;
  }

  public void setTables(List<Table> tables) {
    this.tables = tables;
  }

  public List<Setting> getSettings() {
    return settings;
  }

  public void setSettings(List<Setting> settings) {
    this.settings = settings;
  }
}
