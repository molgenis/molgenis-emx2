package org.molgenis.emx2.cafevariome.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecordIndexResponse(Capability capability, int recordCount, EavIndex eavIndex) {
  public RecordIndexResponse(int recordCount, EavIndex eavIndex) {
    this(new Capability(), recordCount, eavIndex);
  }

  public record Capability(
      boolean subject,
      boolean genes,
      boolean hpo,
      boolean ordo,
      boolean snomed,
      boolean variant,
      boolean source,
      boolean eav) {
    public Capability() {
      this(true, true, true, true, true, true, true, true);
    }
  }

  public record EavIndex(
      Map<String, String> attributes,
      Map<String, String> values,
      Map<String, List<String>> mappings) {}
}
