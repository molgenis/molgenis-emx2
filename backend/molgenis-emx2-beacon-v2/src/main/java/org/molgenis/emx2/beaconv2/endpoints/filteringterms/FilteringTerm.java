package org.molgenis.emx2.beaconv2.endpoints.filteringterms;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import org.molgenis.emx2.Column;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FilteringTerm {

  private String type;
  private String id;
  private String label;
  private String scope;
  @JsonIgnore private Column column;

  public FilteringTerm(Column column, String type, String id, String label, String scope) {
    setColumn(column);
    setType(type);
    setId(id);
    setLabel(label);
    setScope(Character.toLowerCase(scope.charAt(0)) + scope.substring(1));
  }

  public FilteringTerm(String type, String id, String scope) {
    this.type = type;
    this.id = id;
    this.scope = Character.toLowerCase(scope.charAt(0)) + scope.substring(1);
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public Column getColumn() {
    return column;
  }

  public void setColumn(Column column) {
    this.column = column;
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
