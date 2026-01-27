package org.molgenis.emx2.fairmapper.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.StepConfigDeserializer;

public record Mapping(
    String name,
    String endpoint,
    String fetch,
    List<String> methods,
    String input,
    String output,
    String frame,
    @JsonDeserialize(using = StepConfigDeserializer.class) List<StepConfig> steps,
    E2e e2e) {

  public String input() {
    return input != null ? input : "json";
  }

  public String output() {
    return output != null ? output : "json";
  }

  public String getEffectiveName() {
    if (name != null && !name.isBlank()) return name;
    if (endpoint != null) return endpoint.replace("/{schema}/api/", "").replace("/", "-");
    return null;
  }

  public void validate() {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Mapping requires 'name' field");
    }
    if (endpoint != null && fetch != null) {
      throw new IllegalArgumentException("Mapping cannot have both 'endpoint' and 'fetch'");
    }
    if (endpoint == null && fetch == null) {
      throw new IllegalArgumentException("Mapping requires either 'endpoint' or 'fetch'");
    }
    if (fetch != null && (frame == null || frame.isBlank())) {
      throw new IllegalArgumentException("Mapping with 'fetch' requires 'frame' field");
    }
  }
}
