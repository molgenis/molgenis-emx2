package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Arrays;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Filter {
  private Object id;
  private String operator; // todo: use BeaconFilterOperator but serialization is tricky
  private Object value;

  // must deal with values as either 'string' or 'string array' hence this workaround
  @JsonIgnore private String[] values;
  @JsonIgnore private boolean valuesAreParsed;

  // values can (for some reason) also be supplied in the ID field for 'ontology queries' and they
  // must also be dealt with as either 'string' or 'string array'
  @JsonIgnore private String[] ids;
  @JsonIgnore private boolean idsAreParsed;

  // constructor required for JUnit tests
  public Filter() {
    super();
  }

  // regular constructor
  public Filter(Object id, String operator, Object value) {
    this.id = id;
    this.operator = operator;
    this.value = value;
  }

  /** Helper function to support both 'string' and 'string array' inputs for Values */
  public void parseValues() {
    if (this.value instanceof ArrayList<?>) {
      ArrayList<String> valArrList = (ArrayList<String>) this.value;
      this.values = valArrList.toArray(new String[valArrList.size()]);
    } else {
      this.values = this.value == null ? null : new String[] {this.value + ""};
    }
  }

  /** Helper function to support both 'string' and 'string array' inputs for IDs */
  public void parseIDs() {
    if (this.id instanceof ArrayList<?>) {
      ArrayList<String> idArrList = (ArrayList<String>) this.id;
      this.ids = idArrList.toArray(new String[idArrList.size()]);
    } else {
      this.ids = this.id == null ? null : new String[] {this.id + ""};
    }
  }

  /**
   * Getter that checks last-moment if IDs still need parsing
   *
   * @return
   */
  public String[] getIds() {
    if (!idsAreParsed) {
      parseIDs();
      idsAreParsed = true;
    }
    return ids;
  }

  /**
   * Getter that checks last-moment if Values still need parsing
   *
   * @return
   */
  public String[] getValues() {
    if (!valuesAreParsed) {
      parseValues();
      valuesAreParsed = true;
    }
    return values;
  }

  public String getOperator() {
    return operator;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "Filter{"
        + "id="
        + id
        + ", operator='"
        + operator
        + '\''
        + ", value="
        + value
        + ", values="
        + Arrays.toString(values)
        + ", ids="
        + Arrays.toString(ids)
        + '}';
  }
}
