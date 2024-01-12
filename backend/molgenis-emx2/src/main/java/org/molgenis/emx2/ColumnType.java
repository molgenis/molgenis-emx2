package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public enum ColumnType {
  // SIMPLE
  BOOL(Boolean.class, EQUALITY_OPERATORS),
  BOOL_ARRAY(Boolean[].class, EQUALITY_OPERATORS),
  UUID(java.util.UUID.class, EQUALITY_OPERATORS),
  UUID_ARRAY(java.util.UUID[].class, EQUALITY_OPERATORS),
  FILE(byte[].class, EXISTS_OPERATIONS),

  // STRING
  STRING(String.class, STRING_OPERATORS),
  STRING_ARRAY(String[].class, STRING_OPERATORS),
  TEXT(String.class, STRING_OPERATORS),
  TEXT_ARRAY(String[].class, STRING_OPERATORS),

  // NUMERIC
  INT(Integer.class, ORDINAL_OPERATORS),
  INT_ARRAY(Integer[].class, ORDINAL_OPERATORS),
  LONG(Long.class, ORDINAL_OPERATORS),
  LONG_ARRAY(Long[].class, ORDINAL_OPERATORS),
  DECIMAL(Double.class, ORDINAL_OPERATORS),
  DECIMAL_ARRAY(Double[].class, ORDINAL_OPERATORS),
  DATE(LocalDate.class, ORDINAL_OPERATORS),
  DATE_ARRAY(LocalDate[].class, ORDINAL_OPERATORS),
  DATETIME(LocalDateTime.class, ORDINAL_OPERATORS),
  DATETIME_ARRAY(LocalDateTime[].class, ORDINAL_OPERATORS),

  // COMPOSITE
  JSONB(org.jooq.JSONB.class),
  JSONB_ARRAY(org.jooq.JSONB[].class),

  // RELATIONSHIP
  REF(Object.class),
  REF_ARRAY(Object[].class),
  REFBACK(Object[].class),

  // LAYOUT and other constants
  HEADING(String.class), // use for layout elements or constant values

  // format flavors that extend a baseType
  AUTO_ID(STRING),
  ONTOLOGY(REF),
  ONTOLOGY_ARRAY(REF_ARRAY),
  EMAIL(STRING, EMAIL_REGEX),
  EMAIL_ARRAY(STRING_ARRAY, EMAIL_REGEX),
  HYPERLINK(STRING, HYPERLINK_REGEX),
  HYPERLINK_ARRAY(STRING_ARRAY, HYPERLINK_REGEX);

  private Class javaType;
  private ColumnType baseType;
  private Operator[] operators;
  private String validationRegexp;

  ColumnType(Class javaType, Operator... operators) {
    this.javaType = javaType;
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
    return Objects.requireNonNullElse(baseType, this);
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
      if (isArray()) {
        validate((Object[]) value);
      } else {
        if (!value.toString().matches(validationRegexp)) {
          throw new MolgenisException("Validation failed: " + value + " is not valid " + name());
        }
      }
    }
  }

  /** throws exception when invalid */
  public void validate(Object[] values) {
    for (Object value : values) {
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
