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
  REFBACK(Object[].class, "xsd:anySimpleType"),

  // LAYOUT and other constants
  HEADING(String.class, null), // use for layout elements or constant values

  // format flavors that extend a baseType
  ONTOLOGY(REF),
  ONTOLOGY_ARRAY(REF_ARRAY);

  private Class javaType;
  private ColumnType baseType;
  private Operator[] operators;
  private String xsdType;

  ColumnType(Class javaType, String xsdType, Operator... operators) {
    this.javaType = javaType;
    this.xsdType = xsdType;
    this.operators = operators;
  }

  ColumnType(ColumnType baseType) {
    if (this.baseType != null) throw new RuntimeException("Cannot extend an extended type");
    this.baseType = baseType; // use to extend a base type
  }

  public ColumnType getBaseType() {
    if (baseType != null) {
      return baseType;
    } else {
      return this;
    }
  }

  public Class<?> getType() {
    if (baseType != null) {
      return baseType.getType();
    } else {
      return this.javaType;
    }
  }

  public Operator[] getOperators() {
    if (baseType != null) {
      return this.baseType.getOperators();
    } else {
      return this.operators;
    }
  }

  /** Check if value will be an array */
  public boolean isArray() {
    return this.getBaseType().name().endsWith("ARRAY") || this.isRefback();
  }

  /** Check basetype is REF, REF_ARRAY, REF_BACK */
  public boolean isReference() {
    return isRef() || isRefArray() || isRefback();
  }

  /** Check basetype is REF */
  public boolean isRef() {
    return REF.equals(getBaseType());
  }

  /** Check basetype is REF_ARRAY */
  public boolean isRefArray() {
    return REF_ARRAY.equals(getBaseType());
  }

  /** Check base type is REFBACK */
  public boolean isRefback() {
    return REFBACK.equals(getBaseType());
  }

  /** Check base type is FILE */
  public boolean isFile() {
    return FILE.equals(getBaseType());
  }

  /** Check base type is HEADING */
  public boolean isHeading() {
    return HEADING.equals(getBaseType());
  }
}
