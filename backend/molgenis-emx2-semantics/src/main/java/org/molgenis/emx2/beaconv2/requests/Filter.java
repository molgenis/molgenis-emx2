package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Arrays;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Filter {
  private String id;
  private String operator; // todo: use BeaconFilterOperator but serialization is tricky
  private Object value;

  // must deal with values as either 'string' or 'string array' hence this workaround
  @JsonIgnore private String[] values;

  // required for JUnit tests
  public Filter() {
    super();
  }

  public Filter(String id, String operator, Object value) {
    this.id = id;
    this.operator = operator;
    this.value = value;
  }

  public Filter(String id, String operator, String[] values) {
    this.id = id;
    this.operator = operator;
    this.values = values;
  }

  public void parseValues() {
    if (value instanceof ArrayList<?>) {
      ArrayList<String> valArrList = (ArrayList<String>) value;
      this.values = valArrList.toArray(new String[valArrList.size()]);
    } else {
      this.values = new String[] {value + ""};
    }
  }

  public String getId() {
    return id;
  }

  public String getOperator() {
    return operator;
  }

  public String[] getValues() {
    return values;
  }

  @Override
  public String toString() {
    return "Filter{"
        + "id='"
        + id
        + '\''
        + ", operator='"
        + operator
        + '\''
        + ", value="
        + value
        + ", values="
        + Arrays.toString(values)
        + '}';
  }
}
