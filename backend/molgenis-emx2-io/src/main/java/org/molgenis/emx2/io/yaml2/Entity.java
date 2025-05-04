package org.molgenis.emx2.io.yaml2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Entity {

  @JsonProperty(required = true)
  private String name;

  @JsonProperty(required = true)
  private List<Field> fields;

  @JsonProperty("import")
  private String import_path;

  @JsonIgnore private URL sourceURL;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getImport_path() {
    return import_path;
  }

  public void setImport_path(String import_path) {
    this.import_path = import_path;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  public URL getSourceURL() {
    return sourceURL;
  }

  public void setSourceURL(URL baseUrl) {
    this.sourceURL = baseUrl;
    if (fields != null) {
      fields.forEach(field -> field.setSourceURL(baseUrl));
    }
  }

  public void loadImports() {
    if (fields != null) {
      List<Field> result = new ArrayList<>();
      for (Field field : fields) {
        if (field.getImport_path() != null) {
          try {
            // try to be an entity
            Entity importedEntity =
                Yaml2Loader.loadEntity(
                    Yaml2Loader.resolveImportUrl(
                        field.getImport_path(), Yaml2Loader.getBaseUrl(sourceURL)));
            result.addAll(importedEntity.fields);
          } catch (Exception e) {
            // might be one field
            result.add(
                Yaml2Loader.loadField(
                    Yaml2Loader.resolveImportUrl(
                        field.getImport_path(), Yaml2Loader.getBaseUrl(sourceURL))));
          }
        } else {
          result.add(field);
        }
      }
      this.fields = result;
    }
  }
}
