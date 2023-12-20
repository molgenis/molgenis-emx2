package org.molgenis.emx2.beaconv2.endpoints.filteringterms;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FilteringTerm {

  private String type;
  private String id;
  private String label;
  private String scope;

  public FilteringTerm(String type, String id, String label, String scope) {
    this.type = type;
    this.id = id;
    this.label = label;
    this.scope = Character.toLowerCase(scope.charAt(0)) + scope.substring(1);
  }

  public FilteringTerm(String type, String id, String scope) {
    this.type = type;
    this.id = id;
    this.scope = Character.toLowerCase(scope.charAt(0)) + scope.substring(1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FilteringTerm that = (FilteringTerm) o;

    if (!Objects.equals(type, that.type)) return false;
    if (!Objects.equals(id, that.id)) return false;
    if (!Objects.equals(label, that.label)) return false;
    return Objects.equals(scope, that.scope);
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (id != null ? id.hashCode() : 0);
    result = 31 * result + (label != null ? label.hashCode() : 0);
    result = 31 * result + (scope != null ? scope.hashCode() : 0);
    return result;
  }
}
