package org.molgenis.emx2.cafevariome.get;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndexResponse {

  public IndexResponse() {}

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Map<String, Object[]> attributes_values;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Map<String, String> attributes_display_names;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Map<Object, Object> values_display_names;

  public Map<String, Object[]> getAttributes_values() {
    return attributes_values;
  }

  public void setAttributes_values(Map<String, Object[]> attributes_values) {
    this.attributes_values = attributes_values;
  }

  public Map<String, String> getAttributes_display_names() {
    return attributes_display_names;
  }

  public void setAttributes_display_names(Map<String, String> attributes_display_names) {
    this.attributes_display_names = attributes_display_names;
  }

  public Map<Object, Object> getValues_display_names() {
    return values_display_names;
  }

  public void setValues_display_names(Map<Object, Object> values_display_names) {
    this.values_display_names = values_display_names;
  }
}
