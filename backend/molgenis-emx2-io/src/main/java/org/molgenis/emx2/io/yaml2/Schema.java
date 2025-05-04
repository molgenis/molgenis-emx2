package org.molgenis.emx2.io.yaml2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.List;

public class Schema {
  @JsonProperty(required = true)
  private String name;

  private String description;

  @JsonProperty(required = true)
  private List<Entity> entities;

  @JsonIgnore private URL sourceURL;

  public void loadImports() {
    if (entities != null)
      for (int i = 0; i < entities.size(); i++) {
        String importPath = entities.get(i).getImport_path();
        if (importPath != null) {
          // replace with imported entity
          entities.set(
              i, Yaml2Loader.loadEntity(Yaml2Loader.resolveImportUrl(importPath, sourceURL)));
        }
      }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
}
