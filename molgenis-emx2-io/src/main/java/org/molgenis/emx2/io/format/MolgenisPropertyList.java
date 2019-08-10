package org.molgenis.emx2.io.format;

import org.molgenis.Type;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MolgenisPropertyList {
  private static final Pattern pattern =
      Pattern.compile("([a-z]+)(\\((.*?(?<!\\\\))\\))?"); // NOSONAR

  private Map<String, String> termParameterMap = new LinkedHashMap<>();

  public MolgenisPropertyList() {
    // null constructor
  }

  public MolgenisPropertyList(String definitionString) {
    if (definitionString == null || "".equals(definitionString)) return;
    // else
    Matcher matcher = pattern.matcher(definitionString);
    while (matcher.find()) {
      String name = matcher.group(1).toLowerCase();
      String value = matcher.group(3);
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
    return Arrays.asList(termParameterMap.get(term).split(","));
  }

  public MolgenisPropertyList add(String name) {
    termParameterMap.put(name, null);
    return this;
  }

  public MolgenisPropertyList add(String name, String parameterValue) {
    termParameterMap.put(name, parameterValue);
    return this;
  }

  public MolgenisPropertyList add(String name, Collection<String> parameterValues) {
    return this.add(name, join(parameterValues));
  }

  public MolgenisPropertyList add(Type type) {
    return this.add(type.toString().toLowerCase());
  }

  public MolgenisPropertyList add(Type type, String parameterValue) {
    return this.add(type.toString().toLowerCase(), parameterValue);
  }

  public MolgenisPropertyList add(Type type, Collection<String> parameterValues) {
    return this.add(type, join(parameterValues));
  }

  private static String join(Collection collection) {
    return (String) collection.stream().map(Object::toString).collect(Collectors.joining(","));
  }

  public boolean contains(String term) {
    return termParameterMap.containsKey(term.toLowerCase());
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (String term : getTerms()) {
      sb.append(" ").append(term);
      String value = getParamterValue(term);
      if (value != null) sb.append("(").append(value).append(")");
    }
    return sb.toString().trim();
  }

  public void remove(String term) {
    termParameterMap.remove(term);
  }
}
