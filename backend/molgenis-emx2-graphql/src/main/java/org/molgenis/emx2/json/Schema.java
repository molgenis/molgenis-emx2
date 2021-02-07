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
    // deterministic order is important for all kinds of comparisons
    List<String> list = new ArrayList<>();
    list.addAll(schema.getTableNames());
    this.settings = List.of(schema.getSettings().toArray(new Setting[0]));
    Collections.sort(list);
    Set<String> imported = new LinkedHashSet<>();
    for (String tableName : list) {
      org.molgenis.emx2.TableMetadata t = schema.getTableMetadata(tableName);
      tables.add(new Table(t));
      getImportedTablesRecursively(imported, t);
    }
  }

  private void getImportedTablesRecursively(Set<String> imported, TableMetadata t) {
    for (org.molgenis.emx2.Column c : t.getColumns()) {
      if (!c.getRefSchema().equals(c.getSchemaName()) && !imported.contains(c.getRefTableName())) {
        Table ref = new Table(c.getRefTable());
        ref.setExternalSchema(c.getRefSchema());
        tables.add(ref);
        imported.add(c.getRefTableName());

        // recurse
        for (org.molgenis.emx2.Column c2 : c.getRefTable().getPrimaryKeyColumns()) {
          if (c2.isReference()) {
            getImportedTablesRecursively(imported, c2.getRefTable());
          }
        }
      }
    }
  }

  public SchemaMetadata getSchemaMetadata() {
    SchemaMetadata s = new SchemaMetadata();
    s.setSettings(this.settings);
    for (Table t : this.tables) {
      TableMetadata tm = s.create(table(t.getName()));
      tm.setInherit(t.getInherit());
      tm.setSettings(t.getSettings());
      if (t.getDrop()) tm.drop();
      tm.setJsonldType(t.getJsonldType());
      tm.setDescription(t.getDescription());
      for (Column c : t.getColumns()) {
        int i = 1;
        if (!c.isInherited()) {
          // we remove clearly inherited columns here
          org.molgenis.emx2.Column cm = c.getColumnMetadata(tm);
          // add position so we can also deal with ordering
          cm.setPosition(i++);
          tm.add(cm);
        }
      }
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
