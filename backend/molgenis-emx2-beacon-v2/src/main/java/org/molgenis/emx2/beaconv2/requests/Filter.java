package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.Concept;
import org.molgenis.emx2.beaconv2.filter.FilterType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Filter {
  private Object id;
  private String operator;
  private Object value;

  private Concept concept;
  private FilterType filterType;

  boolean includeDescendantTerms;
  Similarity similarity;

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

  public FilterType getFilterType() {
    return filterType;
  }

  public void setFilterType(FilterType filterType) {
    if (this.getValues() != null) {
      for (String value : getValues()) {
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

  public Concept getConcept() {
    return concept;
  }

  public void setConcept(Concept concept) {
    this.concept = concept;
  }

  public void setIds(String[] ids) {
    this.ids = ids;
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

  public boolean filter(List<String> values) {
    if (filterType == FilterType.NUMERICAL) {
      for (String value : values) {
        if (value == null) return false;
        Period period = Period.parse(value);
        int age = period.getYears();
        for (String thisValue : this.getValues()) {
          int thisNumerical = Integer.parseInt(thisValue);
          switch (operator) {
            case ">":
              if (age > thisNumerical) return true;
            case ">=":
              if (age >= thisNumerical) return true;
            case "<":
              if (age < thisNumerical) return true;
            case "<=":
              if (age <= thisNumerical) return true;
            case "=":
              if (age == thisNumerical) return true;
          }
        }
      }
    }
    return false;
  }

  public String getGraphQlFilter() {
    StringBuilder filter = new StringBuilder();

    String[] filterTerms;
    if (filterType == FilterType.ONTOLOGY) {
      filterTerms = this.getIds();
    } else {
      filterTerms = this.getValues();
    }

    for (String id : filterTerms) {
      if (concept != null) {
        filter.append(this.concept.getGraphQlQuery().formatted(id)).append(",");
      }
    }
    filter.deleteCharAt(filter.length() - 1);
    return filter.toString();
  }
}
