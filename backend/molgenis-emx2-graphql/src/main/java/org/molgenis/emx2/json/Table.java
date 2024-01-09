package org.molgenis.emx2.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;

public class Table {
  private String name;
  private String label;
  private String description;
  private String oldName;
  private boolean drop;
  private String[] pkey;
  private String inheritId;
  private String inheritName;
  private List<LanguageValue> labels = new ArrayList<>();
  private List<LanguageValue> descriptions = new ArrayList<>();
  private String schemaName;
  private String schemaId;
  private Collection<String[]> unique = new ArrayList<>();
  private Collection<Column> columns = new ArrayList<>();
  private List<Setting> settings = new ArrayList<>();
  private String[] semantics;
  private String id;
  private TableType tableType;

  public Table() {
    // for json serialisation
  }

  public Table(TableMetadata tableMetadata) {
    this(tableMetadata, false);
  }

  public Table(TableMetadata tableMetadata, boolean minimal) {
    this.name = tableMetadata.getTableName();
    this.label = tableMetadata.getLabel();
    this.description = tableMetadata.getDescription();
    this.labels =
        tableMetadata.getLabels().entrySet().stream()
            .map(entry -> new LanguageValue(entry.getKey(), entry.getValue()))
            .toList();
    this.id = tableMetadata.getIdentifier();
    this.drop = tableMetadata.isDrop();
    this.oldName = tableMetadata.getOldName();
    if (tableMetadata.getInheritName() != null) {
      this.inheritId = tableMetadata.getInheritedTable().getIdentifier();
      this.inheritName = tableMetadata.getInheritName();
    }
    this.descriptions =
        tableMetadata.getDescriptions().entrySet().stream()
            .map(entry -> new LanguageValue(entry.getKey(), entry.getValue()))
            .toList();
    this.semantics = tableMetadata.getSemantics();
    this.settings =
        tableMetadata.getSettings().entrySet().stream()
            .map(entry -> new Setting(entry.getKey(), entry.getValue()))
            .toList();
    this.schemaName = tableMetadata.getSchemaName();
    this.schemaId = tableMetadata.getSchema().getName(); // todo? getIdentifier?
    for (org.molgenis.emx2.Column column : tableMetadata.getColumns()) {
      this.columns.add(new Column(column, tableMetadata, minimal));
    }
    this.tableType = tableMetadata.getTableType();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOldName() {
    return oldName;
  }

  public void setOldName(String oldName) {
    this.oldName = oldName;
  }

  public Collection<String[]> getUnique() {
    return unique;
  }

  public void setUnique(Collection<String[]> unique) {
    this.unique = unique;
  }

  public Collection<Column> getColumns() {
    return columns;
  }

  public void setColumns(Collection<Column> columns) {
    this.columns = columns;
  }

  public String[] getPkey() {
    return pkey;
  }

  public void setPkey(String[] pkey) {
    this.pkey = pkey;
  }

  public String getInheritId() {
    return inheritId;
  }

  public void setInheritId(String inheritId) {
    this.inheritId = inheritId;
  }

  public List<LanguageValue> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(List<LanguageValue> descriptions) {
    this.descriptions = descriptions;
  }

  public List<Setting> getSettings() {
    return settings;
  }

  public void setSettings(List<Setting> settings) {
    this.settings = settings;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public String[] getSemantics() {
    return semantics;
  }

  public void setSemantics(String[] semantics) {
    this.semantics = semantics;
  }

  public boolean getDrop() {
    return drop;
  }

  public void setDrop(boolean drop) {
    this.drop = drop;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TableType getTableType() {
    return tableType;
  }

  public void setTableType(TableType tableType) {
    this.tableType = tableType;
  }

  public List<LanguageValue> getLabels() {
    return labels;
  }

  public void setLabels(List<LanguageValue> labels) {
    this.labels = labels;
  }

  public String getInheritName() {
    return inheritName;
  }

  public void setInheritName(String inheritName) {
    this.inheritName = inheritName;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSchemaId() {
    return schemaId;
  }

  public void setSchemaId(String schemaId) {
    this.schemaId = schemaId;
  }
}
