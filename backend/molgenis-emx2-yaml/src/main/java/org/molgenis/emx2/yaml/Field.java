package org.molgenis.emx2.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Field {
  @JsonIgnore private URL sourceURL;
  // either 'field' or 'entity' or 'import'
  private String field;
  private String heading;
  private String label;
  private Map<String, String> labels;
  private String description;
  private Map<String, String> descriptions;
  private String type;
  private String variant;

  @JsonProperty("import")
  private String import_path;

  private List<String> import_fields;

  Integer key;
  String ref;
  String refSchema;
  String refBack;
  Boolean required;
  Boolean readonly;
  String validIf;
  String visibleIf;
  String computed;
  String predicate;

  @JsonProperty("default")
  String defaultValue;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Integer getKey() {
    return key;
  }

  public void setKey(Integer key) {
    this.key = key;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public String getImport_path() {
    return import_path;
  }

  public void setImport_path(String import_path) {
    this.import_path = import_path;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public URL getSourceURL() {
    return sourceURL;
  }

  public void setSourceURL(URL sourceURL) {
    this.sourceURL = sourceURL;
  }

  public String getHeading() {
    return heading;
  }

  public void setHeading(String heading) {
    this.heading = heading;
  }

  public String getRefSchema() {
    return refSchema;
  }

  public void setRefSchema(String refSchema) {
    this.refSchema = refSchema;
  }

  public String getRefBack() {
    return refBack;
  }

  public void setRefBack(String refBack) {
    this.refBack = refBack;
  }

  public String getValidIf() {
    return validIf;
  }

  public void setValidIf(String validIf) {
    this.validIf = validIf;
  }

  public String getVisibleIf() {
    return visibleIf;
  }

  public void setVisibleIf(String visibleIf) {
    this.visibleIf = visibleIf;
  }

  public String getComputed() {
    return computed;
  }

  public void setComputed(String computed) {
    this.computed = computed;
  }

  public String getPredicate() {
    return predicate;
  }

  public void setPredicate(String predicate) {
    this.predicate = predicate;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public Map<String, String> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(Map<String, String> descriptions) {
    this.descriptions = descriptions;
  }

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(Boolean readonly) {
    this.readonly = readonly;
  }

  public String getVariant() {
    return variant;
  }

  public void setVariant(String variant) {
    this.variant = variant;
  }

  public List<String> getImport_fields() {
    return import_fields;
  }

  public void setImport_fields(List<String> import_fields) {
    this.import_fields = import_fields;
  }

  public void loadImports(Entity parentEntity) {
    if (this.import_path != null) {
      // usually a list of fields
      try {
        Entity entity =
            Yaml2Loader.loadEntity(
                Yaml2Loader.resolveImportUrl(import_path, Yaml2Loader.getBaseUrl(sourceURL)));
        List newFields = new ArrayList();
        newFields.addAll(parentEntity.getFields());
        newFields.addAll(entity.getFields());
        parentEntity.setFields(newFields);
      } catch (Exception e) {
        // might be just one field
        Field importedField =
            Yaml2Loader.loadField(
                Yaml2Loader.resolveImportUrl(import_path, Yaml2Loader.getBaseUrl(sourceURL)));
        List newFields = new ArrayList();
        newFields.addAll(parentEntity.getFields());
        newFields.add(importedField);
        parentEntity.setFields(newFields);
      }
    }
  }

  public void overrideProperties(Field overrideField) {
    // can't override name
    if (overrideField.heading != null) this.heading = overrideField.heading;
    if (overrideField.label != null) this.label = overrideField.label;
    if (overrideField.description != null) this.description = overrideField.description;
    if (overrideField.type != null) this.type = overrideField.type;
    if (overrideField.variant != null) this.variant = overrideField.variant;
    if (overrideField.import_path != null) this.import_path = overrideField.import_path;
    if (overrideField.import_fields != null) this.import_fields = overrideField.import_fields;
    if (overrideField.key != null) this.key = overrideField.key;
    if (overrideField.ref != null) this.ref = overrideField.ref;
    if (overrideField.refSchema != null) this.refSchema = overrideField.refSchema;
    if (overrideField.refBack != null) this.refBack = overrideField.refBack;
    if (overrideField.required != null) this.required = overrideField.required;
    if (overrideField.readonly != null) this.readonly = overrideField.readonly;
    if (overrideField.validIf != null) this.validIf = overrideField.validIf;
    if (overrideField.visibleIf != null) this.visibleIf = overrideField.visibleIf;
    if (overrideField.computed != null) this.computed = overrideField.computed;
    if (overrideField.predicate != null) this.predicate = overrideField.predicate;
    if (overrideField.defaultValue != null) this.defaultValue = overrideField.defaultValue;
    if (overrideField.labels != null) {
      if (this.labels == null) this.labels = new LinkedHashMap<>();
      this.labels.putAll(overrideField.labels);
    }
    if (overrideField.descriptions != null) {
      if (this.descriptions == null) this.descriptions = new LinkedHashMap<>();
      this.descriptions.putAll(overrideField.descriptions);
    }
  }
}
