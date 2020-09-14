package org.molgenis.emx2.json;

import org.jooq.conf.Settings;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Setting;
import org.molgenis.emx2.TableMetadata;

import java.util.*;
import java.util.stream.Collectors;

import static org.molgenis.emx2.TableMetadata.table;

public class Schema {
  private List<Table> tables = new ArrayList<>();
  private List<Setting> settings = new ArrayList();

  public Schema() {
    // for json serialization
  }

  public Schema(SchemaMetadata schema) {
    // deterministic order is important for all kinds of comparisons
    List<String> list = new ArrayList<>();
    list.addAll(schema.getTableNames());
    this.settings =
        schema.getSettings().entrySet().stream()
            .map(entry -> new Setting(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    Collections.sort(list);
    for (String tableName : list) {
      tables.add(new Table(schema.getTableMetadata(tableName)));
    }
  }

  public SchemaMetadata getSchemaMetadata() {
    SchemaMetadata s = new SchemaMetadata();
    s.setSettings(settings.stream().collect(Collectors.toMap(Setting::getKey, Setting::getValue)));
    for (Table t : tables) {
      TableMetadata tm = s.create(table(t.getName()));
      for (Column c : t.getColumns()) {
        tm.add(c.getColumnMetadata(tm));
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
