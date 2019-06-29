package org.molgenis;

public interface Column {
  Table getTable();

  String getName();

  Type getType();

  Boolean isNullable();

  // TODO Display metatadata

  // TODO Boolean isReadonly();

  // TODO private String defaultValue;

  // TODO private String description;

  // TODO private String validation;

  // TODO private String visible;

  Table getRefTable();

  Column setRefTable(Table table);

  String getMrefTable();

  String getMrefBack();

  Column setNullable(boolean nillable) throws MolgenisException;

  boolean isReadonly();

  void setReadonly(boolean readonly);

  String getDescription();

  void setDescription(String description);

  String getVisible();

  void setVisible(String visible);

  String getValidation();

  void setValidation(String validation);

  boolean isUnique();

  String getDefaultValue();

  void setDefaultValue(String defaultValue);

  enum Type {
    UUID,
    STRING,
    BOOL,
    INT,
    DECIMAL,
    TEXT,
    DATE,
    DATETIME,
    ENUM,
    REF,
    MREF;
    // advanced types
    //    SELECT,
    //    RADIO,
    //    MSELECT,
    //    CHECKBOX,
    //    HYPERLINK,
    //    LONG,
    //    EMAIL,
    //    HTML,
    //    FILE,
    //    ENUM;
  }
}
