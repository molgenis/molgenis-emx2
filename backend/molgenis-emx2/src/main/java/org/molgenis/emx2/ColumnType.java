package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public enum ColumnType {

  // SIMPLE
  BOOL(Boolean.class, "xsd:boolean", EQUALITY_OPERATORS),
  BOOL_ARRAY(Boolean[].class, "xsd:list", EQUALITY_OPERATORS),
  UUID(java.util.UUID.class, "xsd:string", EQUALITY_OPERATORS),
  UUID_ARRAY(java.util.UUID[].class, "xsd:list", EQUALITY_OPERATORS),
  FILE(byte[].class, "xsd:base64Binary", EXISTS_OPERATIONS),

  // STRING
  STRING(String.class, "xsd:string", STRING_OPERATORS),
  STRING_ARRAY(String[].class, "xsd:list", STRING_OPERATORS),
  TEXT(String.class, "xsd:string", STRING_OPERATORS),
  TEXT_ARRAY(String[].class, "xsd:list", STRING_OPERATORS),

  // NUMERIC

  INT(Integer.class, "xsd:int", ORDINAL_OPERATORS),
  INT_ARRAY(Integer[].class, "xsd:list", ORDINAL_OPERATORS),
  DECIMAL(Double.class, "xsd:double", ORDINAL_OPERATORS),
  DECIMAL_ARRAY(Double[].class, "xsd:list", ORDINAL_OPERATORS),
  DATE(LocalDate.class, "xsd:date", ORDINAL_OPERATORS),
  DATE_ARRAY(LocalDate[].class, "xsd:list", ORDINAL_OPERATORS),
  DATETIME(LocalDateTime.class, "xsd:datetime", ORDINAL_OPERATORS),
  DATETIME_ARRAY(LocalDateTime[].class, "xsd:list", ORDINAL_OPERATORS),

  // COMPOSITE
  JSONB(org.jooq.JSONB.class, "xsd:string"),
  JSONB_ARRAY(org.jooq.JSONB[].class, "xsd:list"),

  // RELATIONSHIP
  REF(Object.class, "xsd:anySimpleType"),
  REF_ARRAY(Object[].class, "xsd:anySimpleType"),
  // @Deprecated
  // MREF(Object[].class),
  REFBACK(Object[].class, "xsd:anySimpleType");

  private Class javaType;
  private Operator[] operators;
  private String xsdType;

  ColumnType(Class javaType, String xsdType, Operator... operators) {
    this.javaType = javaType;
    this.xsdType = xsdType;
    this.operators = operators;
  }

  public Class<?> getType() {
    return this.javaType;
  }

  public Operator[] getOperators() {
    return this.operators;
  }

  public Object getXsdType() {
    return this.xsdType;
  }

  public boolean isArray() {
    return this.name().endsWith("ARRAY");
  }
}
