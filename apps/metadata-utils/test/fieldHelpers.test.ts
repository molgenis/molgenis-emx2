import { describe, it, expect } from "vitest";
import {
  fieldTypes,
  isArrayType,
  isAutoIdType,
  isCollectionType,
  isFileType,
  isLayoutColumnType,
  isMultiValuedRefType,
  isMultiValuedType,
  isOntologyArrayType,
  isOntologyType,
  isOptionKeyRefType,
  isPartsType,
  isPlainHeadingType,
  isRefArrayType,
  isRefType,
  isRefbackType,
  isSectionType,
  isSingleOntologyType,
  isSingleRefType,
  isStoredMultiValuedType,
  isStoredTableRefType,
  isTableRefType,
  isValueArrayType,
  isValueType,
} from "../src/fieldHelpers";

const columnTypeClasses: Record<string, (columnType: string) => boolean> = {
  ref: isRefType,
  file: isFileType,
  layout: isLayoutColumnType,
  value: isValueType,
};

const refFlavors: Record<string, (columnType: string) => boolean> = {
  singleRef: isSingleRefType,
  refArray: isRefArrayType,
  refback: isRefbackType,
  ontology: isOntologyType,
};

const refTargets: Record<string, (columnType: string) => boolean> = {
  tableRef: isTableRefType,
  ontology: isOntologyType,
};

const refCardinality: Record<string, (columnType: string) => boolean> = {
  oneRow: (columnType) =>
    isSingleRefType(columnType) || isSingleOntologyType(columnType),
  aListOfRows: isMultiValuedRefType,
};

const tableRefStorage: Record<string, (columnType: string) => boolean> = {
  storedOnThisTable: isStoredTableRefType,
  derivedFromTheChildren: isRefbackType,
};

const ontologyFlavors: Record<string, (columnType: string) => boolean> = {
  singleOntology: isSingleOntologyType,
  ontologyArray: isOntologyArrayType,
};

const headingLevels: Record<string, (columnType: string) => boolean> = {
  aSectionAboveTheHeadings: isSectionType,
  aHeadingWithinASection: isPlainHeadingType,
};

const arrayFlavors: Record<string, (columnType: string) => boolean> = {
  valueArray: isValueArrayType,
  refArray: isRefArrayType,
  ontologyArray: isOntologyArrayType,
};

const columnTypesWhereBackendIsArrayReturnsTrue = new Set([
  "BOOL_ARRAY",
  "UUID_ARRAY",
  "STRING_ARRAY",
  "TEXT_ARRAY",
  "INT_ARRAY",
  "LONG_ARRAY",
  "DECIMAL_ARRAY",
  "DATE_ARRAY",
  "DATETIME_ARRAY",
  "PERIOD_ARRAY",
  "EMAIL_ARRAY",
  "HYPERLINK_ARRAY",
  "NON_NEGATIVE_INT_ARRAY",
  "REF_ARRAY",
  "ONTOLOGY_ARRAY",
  "CHECKBOX",
  "MULTISELECT",
  "REFBACK",
  "PARTS",
]);

const columnTypesWhereBackendIsRefbackReturnsTrue = new Set([
  "REFBACK",
  "PARTS",
]);

const matchingNames = (
  predicates: Record<string, (columnType: string) => boolean>,
  columnType: string
) =>
  Object.entries(predicates)
    .filter(([, isMember]) => isMember(columnType))
    .map(([name]) => name);

describe("every column type is classified", () => {
  it.each(fieldTypes())(
    "puts %s in exactly one of ref, file, layout, value",
    (columnType) => {
      const classes = matchingNames(columnTypeClasses, columnType);
      expect(classes.length, `${columnType} is in classes [${classes}]`).toBe(
        1
      );
    }
  );

  it.each(fieldTypes().filter(isRefType))(
    "puts ref type %s in exactly one of singleRef, refArray, refback, ontology",
    (columnType) => {
      const flavors = matchingNames(refFlavors, columnType);
      expect(flavors.length, `${columnType} is in flavors [${flavors}]`).toBe(
        1
      );
    }
  );

  it.each(fieldTypes().filter(isRefType))(
    "points ref type %s at exactly one of a table, an ontology",
    (columnType) => {
      const targets = matchingNames(refTargets, columnType);
      expect(targets.length, `${columnType} points at [${targets}]`).toBe(1);
    }
  );

  it.each(fieldTypes().filter(isRefType))(
    "values ref type %s as exactly one of one row, a list of rows",
    (columnType) => {
      const cardinality = matchingNames(refCardinality, columnType);
      expect(cardinality.length, `${columnType} yields [${cardinality}]`).toBe(
        1
      );
    }
  );

  it.each(fieldTypes().filter(isTableRefType))(
    "either stores table ref %s on this table or derives it from the children, never both",
    (columnType) => {
      const storage = matchingNames(tableRefStorage, columnType);
      expect(storage.length, `${columnType} is stored as [${storage}]`).toBe(1);
    }
  );

  it.each(fieldTypes().filter(isOntologyType))(
    "puts ontology type %s in exactly one of singleOntology, ontologyArray",
    (columnType) => {
      const flavors = matchingNames(ontologyFlavors, columnType);
      expect(flavors.length, `${columnType} is in flavors [${flavors}]`).toBe(
        1
      );
    }
  );

  it.each(fieldTypes().filter(isArrayType))(
    "puts array type %s in exactly one of valueArray, refArray, ontologyArray",
    (columnType) => {
      const flavors = matchingNames(arrayFlavors, columnType);
      expect(flavors.length, `${columnType} is in flavors [${flavors}]`).toBe(
        1
      );
    }
  );

  it.each(fieldTypes())(
    "calls %s multi valued exactly when the backend ColumnType.isArray() does",
    (columnType) => {
      expect(isMultiValuedType(columnType)).toBe(
        columnTypesWhereBackendIsArrayReturnsTrue.has(columnType)
      );
    }
  );

  it.each(fieldTypes())(
    "keeps the values of %s on the row itself exactly when the backend calls it an array but not a refback",
    (columnType) => {
      expect(isStoredMultiValuedType(columnType)).toBe(
        columnTypesWhereBackendIsArrayReturnsTrue.has(columnType) &&
          !columnTypesWhereBackendIsRefbackReturnsTrue.has(columnType)
      );
    }
  );

  it.each(fieldTypes().filter(isLayoutColumnType))(
    "puts layout column %s at exactly one of a section above the headings, a heading within a section",
    (columnType) => {
      const levels = matchingNames(headingLevels, columnType);
      expect(levels.length, `${columnType} is at levels [${levels}]`).toBe(1);
    }
  );

  it.each(fieldTypes())(
    "calls %s an ontology array only when it is both an ontology and an array",
    (columnType) => {
      expect(isOntologyArrayType(columnType)).toBe(
        isOntologyType(columnType) && isArrayType(columnType)
      );
    }
  );
});

describe("PARTS is a REFBACK flavor", () => {
  it("is a refback type, like REFBACK and unlike REF", () => {
    expect(isRefbackType("PARTS")).toBe(true);
    expect(isRefbackType("REFBACK")).toBe(true);
    expect(isRefbackType("REF")).toBe(false);
  });

  it("is a collection type, like REFBACK and unlike REF", () => {
    expect(isCollectionType("PARTS")).toBe(true);
    expect(isCollectionType("REFBACK")).toBe(true);
    expect(isCollectionType("REF")).toBe(false);
  });

  it("is a ref type, like REFBACK", () => {
    expect(isRefType("PARTS")).toBe(true);
    expect(isRefType("REFBACK")).toBe(true);
  });
});

const sortedMembers = (isMember: (columnType: string) => boolean) =>
  fieldTypes().filter(isMember).sort();

describe("ref flavors", () => {
  it("counts REF, SELECT and RADIO as single valued refs", () => {
    expect(sortedMembers(isSingleRefType)).toEqual(["RADIO", "REF", "SELECT"]);
  });

  it("counts REF_ARRAY, CHECKBOX and MULTISELECT as ref arrays", () => {
    expect(sortedMembers(isRefArrayType)).toEqual([
      "CHECKBOX",
      "MULTISELECT",
      "REF_ARRAY",
    ]);
  });

  it("counts ONTOLOGY and ONTOLOGY_ARRAY as ontology refs", () => {
    expect(sortedMembers(isOntologyType)).toEqual([
      "ONTOLOGY",
      "ONTOLOGY_ARRAY",
    ]);
  });

  it("counts every ref array and every refback as a collection", () => {
    expect(sortedMembers(isCollectionType)).toEqual([
      "CHECKBOX",
      "MULTISELECT",
      "PARTS",
      "REFBACK",
      "REF_ARRAY",
    ]);
  });

  it("counts every ref except the ontology ones as a ref at a table", () => {
    expect(sortedMembers(isTableRefType)).toEqual([
      "CHECKBOX",
      "MULTISELECT",
      "PARTS",
      "RADIO",
      "REF",
      "REFBACK",
      "REF_ARRAY",
      "SELECT",
    ]);
  });

  it("counts every ref valued as a list of rows, ontology arrays included", () => {
    expect(sortedMembers(isMultiValuedRefType)).toEqual([
      "CHECKBOX",
      "MULTISELECT",
      "ONTOLOGY_ARRAY",
      "PARTS",
      "REFBACK",
      "REF_ARRAY",
    ]);
  });

  it("counts the table refs that own their foreign key, ontologies excluded, as stored on the table", () => {
    expect(sortedMembers(isStoredTableRefType)).toEqual([
      "CHECKBOX",
      "MULTISELECT",
      "RADIO",
      "REF",
      "REF_ARRAY",
      "SELECT",
    ]);
  });

  it("counts only ONTOLOGY_ARRAY as an ontology array", () => {
    expect(sortedMembers(isOntologyArrayType)).toEqual(["ONTOLOGY_ARRAY"]);
  });

  it("counts only ONTOLOGY as a single valued ontology", () => {
    expect(sortedMembers(isSingleOntologyType)).toEqual(["ONTOLOGY"]);
  });

  it("counts only PARTS as a parts type, unlike its REFBACK sibling", () => {
    expect(sortedMembers(isPartsType)).toEqual(["PARTS"]);
    expect(isPartsType("REFBACK")).toBe(false);
  });
});

describe("multi valued storage", () => {
  it("counts every multi valued type except the refbacks as keeping its values on the row", () => {
    expect(sortedMembers(isStoredMultiValuedType)).toEqual([
      "BOOL_ARRAY",
      "CHECKBOX",
      "DATETIME_ARRAY",
      "DATE_ARRAY",
      "DECIMAL_ARRAY",
      "EMAIL_ARRAY",
      "HYPERLINK_ARRAY",
      "INT_ARRAY",
      "LONG_ARRAY",
      "MULTISELECT",
      "NON_NEGATIVE_INT_ARRAY",
      "ONTOLOGY_ARRAY",
      "PERIOD_ARRAY",
      "REF_ARRAY",
      "STRING_ARRAY",
      "TEXT_ARRAY",
      "UUID_ARRAY",
    ]);
  });

  it("excludes the refbacks, whose values are derived from the children", () => {
    expect(isStoredMultiValuedType("REFBACK")).toBe(false);
    expect(isStoredMultiValuedType("PARTS")).toBe(false);
    expect(isMultiValuedType("REFBACK")).toBe(true);
    expect(isMultiValuedType("PARTS")).toBe(true);
  });
});

describe("heading levels", () => {
  it("counts only SECTION as the level above the headings", () => {
    expect(sortedMembers(isSectionType)).toEqual(["SECTION"]);
  });

  it("counts only HEADING as a heading within a section", () => {
    expect(sortedMembers(isPlainHeadingType)).toEqual(["HEADING"]);
  });

  it("keeps both levels in the layout column family, and neither level inside the other", () => {
    expect(sortedMembers(isLayoutColumnType)).toEqual(["HEADING", "SECTION"]);
    expect(isSectionType("HEADING")).toBe(false);
    expect(isPlainHeadingType("SECTION")).toBe(false);
  });
});

describe("arrays of plain scalars", () => {
  it("counts an array of scalars, not the scalar itself", () => {
    expect(isValueArrayType("STRING_ARRAY")).toBe(true);
    expect(isValueArrayType("STRING")).toBe(false);
  });
});

describe("server generated values", () => {
  it("counts only AUTO_ID as an auto id, and keeps it a plain value type", () => {
    expect(sortedMembers(isAutoIdType)).toEqual(["AUTO_ID"]);
    expect(isValueType("AUTO_ID")).toBe(true);
  });
});

describe("option key refs", () => {
  it("counts RADIO and CHECKBOX as refs filtered by their option key", () => {
    expect(sortedMembers(isOptionKeyRefType)).toEqual(["CHECKBOX", "RADIO"]);
  });

  it("cuts across single valued refs and ref arrays", () => {
    expect(isSingleRefType("RADIO")).toBe(true);
    expect(isRefArrayType("CHECKBOX")).toBe(true);
  });

  it("excludes the ref flavors whose filter value is a ref object", () => {
    expect(isOptionKeyRefType("REF")).toBe(false);
    expect(isOptionKeyRefType("REF_ARRAY")).toBe(false);
    expect(isOptionKeyRefType("SELECT")).toBe(false);
    expect(isOptionKeyRefType("MULTISELECT")).toBe(false);
  });
});
