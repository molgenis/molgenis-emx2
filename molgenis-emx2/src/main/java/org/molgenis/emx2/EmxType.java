package org.molgenis.emx2;

public enum EmxType {
  // basic types
  STRING,
  INT,
  BOOL,
  DECIMAL,
  TEXT,
  DATE,
  DATETIME,
  UUID,
  // advanced types
  SELECT,
  RADIO,
  MSELECT,
  CHECKBOX,
  HYPERLINK,
  LONG,
  EMAIL,
  HTML,
  FILE,
  ENUM;
}
