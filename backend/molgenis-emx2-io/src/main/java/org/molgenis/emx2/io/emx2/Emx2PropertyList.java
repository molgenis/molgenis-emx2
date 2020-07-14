package org.molgenis.emx2.io.emx2;

import org.molgenis.emx2.ColumnType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Emx2PropertyList {
  public static final String KEY = "key";
  public static final String NULLABLE = "nullable";
  public static final String VALIDATE = "validate";
  public static final String CASCADE_DELETE = "cascadeDelete";

  private static final Pattern pattern =
      // Pattern.compile("([a-zA-Z_]+)(\\((.*?(?<!\\\\))\\))?"); // NOSONAR
      // raw
      // ([a-zA-Z_]+)(?:\(([^')"\s\\]*(?:\s+[^')"\s\\]+)*|\s*"([^"\\]*(\\[\S\s][^"\\]*)*)"\s*)?\))?
      Pattern.compile(
          "([a-zA-Z_]+)(?:\\(([^')\"\\s\\\\]*(?:\\s+[^')\"\\s\\\\]+)*|\\s*\"([^\"\\\\]*(\\\\[\\S\\s][^\"\\\\]*)*)\"\\s*)?\\))?"); // nosonar
  private Map<String, String> termParameterMap = new LinkedHashMap<>();

  public Emx2PropertyList() {
    // null constructor
  }

  public Emx2PropertyList(String definitionString) {
    if (definitionString == null || "".equals(definitionString)) return;
    // else
    Matcher matcher = pattern.matcher(definitionString);
    while (matcher.find()) {
      String name = matcher.group(1).toLowerCase();
      String value = matcher.group(2);
      if (matcher.group(3) != null) {
        value = matcher.group(3);
      }
      this.add(name, value);
    }
  }

  public Set<String> getTerms() {
    return Collections.unmodifiableSet(termParameterMap.keySet());
  }

  public String getParamterValue(String term) {
    return termParameterMap.get(term);
  }

  public List<String> getParameterList(String term) {
    if (termParameterMap.get(term) == null) return new ArrayList<>();
    String[] values = termParameterMap.get(term).split(",");
    List<String> result = new ArrayList<>();
    for (String value : values) result.add(value.trim());
    return result;
  }

  public Emx2PropertyList add(String name) {
    termParameterMap.put(name, null);
    return this;
  }

  public Emx2PropertyList add(String name, String parameterValue) {
    termParameterMap.put(name, parameterValue);
    return this;
  }

  public Emx2PropertyList add(String name, String... parameterValues) {
    return this.add(name, join(parameterValues));
  }

  public Emx2PropertyList add(String name, Collection<String> parameterValues) {
    return this.add(name, join(parameterValues));
  }

  public Emx2PropertyList add(ColumnType columnType) {
    return this.add(columnType.toString().toLowerCase());
  }

  public Emx2PropertyList add(ColumnType columnType, String parameterValue) {
    return this.add(columnType.toString().toLowerCase(), parameterValue);
  }

  public Emx2PropertyList add(ColumnType columnType, Collection<String> parameterValues) {
    return this.add(columnType, join(parameterValues));
  }

  private static String join(Collection<String> parameterValues) {
    return join(parameterValues.toArray(new String[parameterValues.size()]));
  }

  private static String join(String[] parameterValues) {
    return Stream.of(parameterValues).map(Object::toString).collect(Collectors.joining(","));
  }

  public boolean contains(String term) {
    return termParameterMap.containsKey(term.toLowerCase());
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (String term : getTerms()) {
      sb.append(" ").append(term);
      String value = getParamterValue(term);
      if (value != null) sb.append("(").append(this.escape(value)).append(")");
    }
    return sb.toString().trim();
  }

  private String escape(String value) {
    if (value.contains(")") || value.contains("\"")) return "\"" + value + "\"";
    else return value;
  }

  public String[] getParameterArray(String term) {
    List<String> parameterList = getParameterList(term);
    return parameterList.toArray(new String[parameterList.size()]);
  }

  public List<String> getParameterList(ColumnType columnType) {
    return getParameterList(columnType.toString().toLowerCase());
  }
}
