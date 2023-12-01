package org.molgenis.emx2.json;

import static org.molgenis.emx2.TableMetadata.table;

import java.util.*;
import java.util.stream.Collectors;
import org.molgenis.emx2.SchemaMetadata;
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
    this.settings =
        schema.getSettings().entrySet().stream()
            .map(entry -> new Setting(entry.getKey(), entry.getValue()))
            .toList();
    List<TableMetadata> list = new ArrayList<>();
    list.addAll(schema.getTables());
    // deterministic order is important for all kinds of comparisons
    Collections.sort(list);
    // add these tables
    for (TableMetadata t : list) {
      tables.add(new Table(t, minimal));
    }
  }

  public SchemaMetadata getSchemaMetadata() {
    SchemaMetadata s = new SchemaMetadata();
    s.setSettings(
        this.settings.stream()
            .filter(d -> d.value() != null)
            .collect(Collectors.toMap(Setting::key, Setting::value)));
    for (Table t : this.tables) {
      TableMetadata tm = s.create(table(t.getName()));
      tm.setInheritName(t.getInheritName());
      tm.setSettings(
          t.getSettings().stream()
              .filter(d -> d.value() != null)
              .collect(Collectors.toMap(Setting::key, Setting::value)));
      tm.setOldName(t.getOldName());
      if (t.getTableType() != null) {
        tm.setTableType(t.getTableType());
      }
      if (t.getDrop()) tm.drop();
      tm.setSemantics(t.getSemantics());
      tm.setLabels(
          t.getLabels().stream()
              .filter(d -> d.value() != null)
              .collect(Collectors.toMap(LanguageValue::locale, LanguageValue::value)));
      tm.setDescriptions(
          t.getDescriptions().stream()
              .filter(d -> d.value() != null)
              .collect(Collectors.toMap(LanguageValue::locale, LanguageValue::value)));
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
