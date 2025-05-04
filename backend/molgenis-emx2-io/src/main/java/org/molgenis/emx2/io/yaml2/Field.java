package org.molgenis.emx2.io.yaml2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Field {
  @JsonIgnore private URL sourceURL;
  private String name;
  private String label;
  private String description;
  private String type;

  @JsonProperty("import")
  private String import_path;

  Integer key;
  String ref;
  Boolean required;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
}
