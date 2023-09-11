export const fieldTypes = () => {
  return [
    "BOOL",
    "BOOL_ARRAY",
    "DATE",
    "DATE_ARRAY",
    "DATETIME",
    "AUTO_ID",
    "DATETIME_ARRAY",
    "DECIMAL",
    "DECIMAL_ARRAY",
    "EMAIL",
    "EMAIL_ARRAY",
    "FILE",
    "HEADING",
    "HYPERLINK",
    "HYPERLINK_ARRAY",
    "INT",
    "INT_ARRAY",
    "LONG",
    "LONG_ARRAY",
    "ONTOLOGY",
    "ONTOLOGY_ARRAY",
    "REF",
    "REF_ARRAY",
    "REFBACK",
    "STRING",
    "STRING_ARRAY",
    "TEXT",
    "TEXT_ARRAY",
    "UUID",
    "UUID_ARRAY",
  ];
};

export const isEmpty = (obj: object) => {
  for (const prop in obj) {
    if (Object.hasOwn(obj, prop)) {
      return false;
    }
  }

  return true;
};
