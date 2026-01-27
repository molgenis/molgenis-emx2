package org.molgenis.emx2.fairmapper.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.StepConfigDeserializer;

public record Mapping(
    String name,
    String endpoint,
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
}
