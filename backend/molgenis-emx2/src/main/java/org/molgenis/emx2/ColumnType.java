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
  LONG(Long.class, "xsd:long", ORDINAL_OPERATORS),
  LONG_ARRAY(Long[].class, "xsd:list", ORDINAL_OPERATORS),
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
  ONTOLOGY_ARRAY(REF_ARRAY),
  // RFC 5322, see http://emailregex.com/
  EMAIL(
      STRING,
      "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]"
          + "+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\""
          + "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")"
          + "@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"
          + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\"
          + "[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"),
  // thank you to
  // https://www.geeksforgeeks.org/check-if-an-url-is-valid-or-not-using-regular-expression/
  HYPERLINK(
      STRING,
      "((http|https)://)(www.)?"
          + "[a-zA-Z0-9@:%._\\+~#?&//=]"
          + "{2,256}\\.[a-z]"
          + "{2,6}\\b([-a-zA-Z0-9@:%"
          + "._\\+~#?&//=]*)");
  private Class javaType;
  private ColumnType baseType;
  private Operator[] operators;
  private String xsdType;
  private String validationRegexp;

  ColumnType(Class javaType, String xsdType, Operator... operators) {
    this.javaType = javaType;
    this.xsdType = xsdType;
    this.operators = operators;
  }

  ColumnType(ColumnType baseType) {
    if (this.baseType != null) throw new RuntimeException("Cannot extend an extended type");
    this.baseType = baseType; // use to extend a base type
  }

  ColumnType(ColumnType baseType, String validationRegexp) {
    if (this.baseType != null) throw new RuntimeException("Cannot extend an extended type");
    this.baseType = baseType; // use to extend a base type
    this.validationRegexp = validationRegexp;
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

  /** throws exception when invalid */
  public void validate(Object value) {
    if (validationRegexp == null) return;
    if (value != null) {
      if (!value.toString().matches(validationRegexp)) {
        throw new MolgenisException("Validation failed: " + value + " is not valid " + name());
      }
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

  public boolean isAtomicType() {
    return !isFile() && !isReference() && !isHeading();
  }
}
