package org.molgenis;

public interface Column {
  Column addColumn(String name, Type type) throws MolgenisException;

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

  Column setRef(String table, String column);

  String getRefTable();

  Column setRefTable(String table);

  String getRefColumn();

  Column setRefColumn(String column);

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
