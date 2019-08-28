package org.molgenis.emx2;

import java.time.LocalDate;
import java.time.LocalDateTime;

public enum Type {
  UUID(java.util.UUID.class),
  UUID_ARRAY(java.util.UUID[].class),
  STRING(String.class),
  STRING_ARRAY(String[].class),
  BOOL(Boolean.class),
  BOOL_ARRAY(Boolean[].class),
  INT(Integer.class),
  INT_ARRAY(Integer[].class),
  DECIMAL(Double.class),
  DECIMAL_ARRAY(Double[].class),
  TEXT(String.class),
  TEXT_ARRAY(String[].class),
  DATE(LocalDate.class),
  DATE_ARRAY(LocalDate[].class),
  DATETIME(LocalDateTime.class),
  DATETIME_ARRAY(LocalDateTime[].class),
  REF(Object.class),
  REF_ARRAY(Object[].class),
  MREF(Object[].class);

  private Class javaType;

  Type(Class javaType) {
    this.javaType = javaType;
  }

  public Class getType() {
    return this.javaType;
  }
}
