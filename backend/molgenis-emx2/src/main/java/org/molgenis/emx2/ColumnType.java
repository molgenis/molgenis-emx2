package org.molgenis.emx2;

import org.jooq.JSONB;
import org.jooq.tools.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.molgenis.emx2.Constants.*;

public enum ColumnType {
  // SIMPLE
  BOOL(Boolean.class, EQUALITY_OPERATORS),
  BOOL_ARRAY(Boolean[].class, EQUALITY_OPERATORS),
  UUID(java.util.UUID.class, EQUALITY_OPERATORS),
  UUID_ARRAY(java.util.UUID[].class, EQUALITY_OPERATORS),
  // STRING
  STRING(String.class, STRING_OPERATORS),
  STRING_ARRAY(String[].class, STRING_OPERATORS),
  TEXT(String.class, STRING_OPERATORS),
  TEXT_ARRAY(String[].class, STRING_OPERATORS),
  // NUMERIC
  INT(Integer.class, ORDINAL_OPERATORS),
  INT_ARRAY(Integer[].class, ORDINAL_OPERATORS),
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
  MREF(Object[].class),
  REFBACK(Object[].class);

  private Class javaType;
  private Operator[] operators;

  ColumnType(Class javaType, Operator... operators) {
    this.javaType = javaType;
    this.operators = operators;
  }

  public Class getType() {
    return this.javaType;
  }

  public Operator[] getOperators() {
    return this.operators;
  }
}
