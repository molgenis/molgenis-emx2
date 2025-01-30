package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.Operator.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public enum ColumnType {
  // SIMPLE
  BOOL(Boolean.class, EQUALITY_OPERATORS),
  BOOL_ARRAY(Boolean[].class, EQUALITY_ARRAY_OPERATORS),
  UUID(java.util.UUID.class, EQUALITY_OPERATORS),
  UUID_ARRAY(java.util.UUID[].class, EQUALITY_ARRAY_OPERATORS),
  FILE(byte[].class, EXISTS_OPERATIONS),

  // STRING
  STRING(String.class, STRING_OPERATORS),
  STRING_ARRAY(String[].class, STRING_ARRAY_OPERATORS),
  TEXT(String.class, STRING_OPERATORS),
  TEXT_ARRAY(String[].class, STRING_ARRAY_OPERATORS),
  JSON(org.jooq.JSONB.class, STRING_OPERATORS),

  // NUMERIC
  INT(Integer.class, ORDINAL_OPERATORS),
  INT_ARRAY(Integer[].class, ORDINAL_ARRAY_OPERATORS),
  LONG(Long.class, ORDINAL_OPERATORS),
  LONG_ARRAY(Long[].class, ORDINAL_ARRAY_OPERATORS),
  DECIMAL(Double.class, ORDINAL_OPERATORS),
  DECIMAL_ARRAY(Double[].class, ORDINAL_ARRAY_OPERATORS),
  DATE(LocalDate.class, ORDINAL_OPERATORS),
  DATE_ARRAY(LocalDate[].class, ORDINAL_ARRAY_OPERATORS),
  DATETIME(LocalDateTime.class, ORDINAL_OPERATORS),
  DATETIME_ARRAY(LocalDateTime[].class, ORDINAL_ARRAY_OPERATORS),
  PERIOD(Period.class, ORDINAL_OPERATORS),
  PERIOD_ARRAY(Period[].class, ORDINAL_ARRAY_OPERATORS),

  // RELATIONSHIP
  REF(Object.class, MATCH_ANY, EQUALS, MATCH_NONE, IS),
  REF_ARRAY(Object[].class, MATCH_ANY, MATCH_ALL, EQUALS, MATCH_NONE, IS),
  REFBACK(Object[].class, REF_ARRAY.operators), // same as ref_array

  // LAYOUT and other constants
  HEADING(String.class), // use for layout elements or constant values

  // format flavors that extend a baseType
  AUTO_ID(STRING),
  ONTOLOGY(REF, MATCH_ANY_INCLUDING_CHILDREN, MATCH_ANY_INCLUDING_PARENTS, MATCH_PATH),
  ONTOLOGY_ARRAY(REF_ARRAY, MATCH_ANY_INCLUDING_CHILDREN, MATCH_ANY_INCLUDING_PARENTS, MATCH_PATH),
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

  ColumnType(ColumnType baseType, Operator... operators) {
    if (this.baseType != null) throw new RuntimeException("Cannot extend an extended type");
    this.baseType = baseType; // use to extend a base type
    this.operators =
        Stream.concat(Arrays.stream(baseType.operators), Arrays.stream(operators))
            .toArray(Operator[]::new);
  }

  ColumnType(ColumnType baseType, String validationRegexp) {
    if (this.baseType != null) throw new RuntimeException("Cannot extend an extended type");
    this.baseType = baseType; // use to extend a base type
    this.operators = this.baseType.operators;
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
    return this.operators;
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

  public boolean isStringyType() {
    return STRING.equals(getBaseType())
        || STRING_ARRAY.equals(getBaseType())
        || TEXT.equals(getBaseType())
        || TEXT_ARRAY.equals(getBaseType());
  }
}
