package org.molgenis.emx2.io.format;

import org.molgenis.Column;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public enum EmxDefinitionTerm {
  // for columns and tables
  UNIQUE(true),
  // for column types
  STRING(false),
  INT(false),
  LONG(false),
  SELECT(true),
  RADIO(true),
  BOOL(false),
  DECIMAL(false),
  TEXT(false),
  DATE(false),
  DATETIME(false),
  MSELECT(true),
  CHECKBOX(true),
  UUID(false),
  HYPERLINK(false),
  EMAIL(false),
  HTML(false),
  FILE(false),
  ENUM(true),
  // for column settings
  NILLABLE(false),
  DEFAULT(true),
  READONLY(false),
  VISIBLE(true),
  VALIDATION(true),
  // for tables
  ABSTRACT(false),
  LABEL(true),
  EXTENDS(true);

  // TODO: FILE, CASCADE, OM, CHECK
  private boolean hasParameter;
  private String parameterValue;

  EmxDefinitionTerm(Boolean hasParameter) {
    this.hasParameter = hasParameter;
  }

  public boolean hasParameter() {
    return hasParameter;
  }

  public String getParameterValue() {
    return parameterValue;
  }

  public List<String> getParameterList() {
    List<String> result = new ArrayList();
    if (getParameterValue() != null) {
      for (String el : getParameterValue().split(",")) {
        result.add(el.trim());
      }
    }
    return result;
  }

  public EmxDefinitionTerm setParameterValue(String parameterValue) {
    this.parameterValue = parameterValue;
    return this;
  }

  @Override
  public String toString() {
    if (hasParameter && getParameterValue() != null) {
      return this.name().toLowerCase() + "(" + parameterValue + ")";
    } else {
      return this.name().toLowerCase();
    }
  }

  public static EmxDefinitionTerm valueOf(Column.Type type) {
    return valueOf(type.toString());
  }

  public EmxDefinitionTerm setParameterList(Collection<String> columnNames) {
    this.parameterValue =
        columnNames.stream().map(Object::toString).collect(Collectors.joining(","));
    return this;
  }
}
