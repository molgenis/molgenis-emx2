package org.molgenis.emx2.beaconv2.filter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Arrays;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.requests.Similarity;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Filter {

  private Object id;
  private String operator;
  private Object value;

  boolean includeDescendantTerms;
  Similarity similarity;

  @JsonIgnore private FilterConceptVP concept;
  @JsonIgnore private FilterType filterType;

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

  public Filter(Filter filter) {
    this.id = filter.id;
    this.ids = filter.ids;
    this.operator = filter.operator;
    this.value = filter.value;
    this.values = filter.values;
    this.filterType = filter.filterType;
    this.concept = filter.concept;
    this.includeDescendantTerms = filter.includeDescendantTerms;
    this.similarity = filter.similarity;
    this.valuesAreParsed = filter.valuesAreParsed;
    this.idsAreParsed = filter.idsAreParsed;
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
      for (int i = 0; i < ids.length; i++) {
        String id = ids[i];
        ids[i] = !id.contains(":") ? id : id.substring(id.indexOf(":") + 1);
      }
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

  public void setValues(String[] values) {
    this.values = values;
  }

  public void setFilterType(FilterType filterType) {
    if (this.values != null) {
      for (String value : values) {
        if (filterType == FilterType.NUMERICAL) {
          try {
            Integer.parseInt(value);
          } catch (NumberFormatException e) {
            throw new MolgenisException("Invalid value expected Integer");
          }
        }
      }
    }
    this.filterType = filterType;
  }

  public FilterConceptVP getConcept() {
    return concept;
  }

  public void setConcept(FilterConceptVP concept) {
    this.concept = concept;
  }

  public Object getId() {
    return id;
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

  @JsonIgnore
  public String getGraphQlFilter() {
    StringBuilder filter = new StringBuilder();

    if (concept != null) {
      String graphQlQuery = concept.getGraphQlQuery().formatted(values);
      filter.append(graphQlQuery).append(",");
    }

    filter.deleteCharAt(filter.length() - 1);
    return filter.toString();
  }
}
