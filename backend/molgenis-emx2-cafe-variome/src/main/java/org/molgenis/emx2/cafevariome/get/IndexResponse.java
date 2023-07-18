package org.molgenis.emx2.cafevariome.get;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndexResponse {

  public IndexResponse() {}

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String source_id;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Map<String, String[]> attributes_values;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Map<String, String> attributes_display_names;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Map<String, String> values_display_names;

  public String getSource_id() {
    return source_id;
  }

  public void setSource_id(String source_id) {
    this.source_id = source_id;
  }

  public Map<String, String[]> getAttributes_values() {
    return attributes_values;
  }

  public void setAttributes_values(Map<String, String[]> attributes_values) {
    this.attributes_values = attributes_values;
  }

  public Map<String, String> getAttributes_display_names() {
    return attributes_display_names;
  }

  public void setAttributes_display_names(Map<String, String> attributes_display_names) {
    this.attributes_display_names = attributes_display_names;
  }

  public Map<String, String> getValues_display_names() {
    return values_display_names;
  }

  public void setValues_display_names(Map<String, String> values_display_names) {
    this.values_display_names = values_display_names;
  }
}
