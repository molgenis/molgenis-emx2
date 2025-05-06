package org.molgenis.emx2.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Schema {
  @JsonProperty(required = true)
  private String schema;

  private String description;
  private List<String> comments;
  // default values used throughout the schema. E.g. for ontologySchema
  private Map<String, String> defaults;

  @JsonProperty(required = true)
  private List<Entity> entities;

  @JsonIgnore private URL sourceURL;

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Entity> getEntities() {
    return entities;
  }

  public void setEntities(List<Entity> entities) {
    this.entities = entities;
  }

  public URL getSourceURL() {
    return sourceURL;
  }

  public List<String> getComments() {
    return comments;
  }

  public void setComments(List<String> comments) {
    this.comments = comments;
  }

  public Map<String, String> getDefaults() {
    return defaults;
  }

  public void setDefaults(Map<String, String> defaults) {
    this.defaults = defaults;
  }

  public void setSourceURL(URL sourceURL) {
    this.sourceURL = sourceURL;
    if (this.entities != null) {
      this.entities.forEach(
          entity -> {
            entity.setSourceURL(sourceURL);
            if (entity.getFields() != null) {
              entity.getFields().forEach(field -> field.setSourceURL(sourceURL));
            }
          });
    }
  }

  public void loadImports() {
    if (entities != null)
      for (int i = 0; i < entities.size(); i++) {
        Entity entity = entities.get(i);
        String importPath = entity.getImport_path();
        if (importPath != null) {
          Entity importedEntity =
              Yaml2Loader.loadEntity(Yaml2Loader.resolveImportUrl(importPath, sourceURL));
          importedEntity.loadImports();
          importedEntity.applyFieldsFilter(entity.getImport_fields());
          importedEntity.applyVariantsFilter(entity.getImport_variants());
          // copy values
          importedEntity.overrideProperties(entity);
          entities.set(i, importedEntity);
        } else {
          entity.loadImports();
        }
      }
  }
}
