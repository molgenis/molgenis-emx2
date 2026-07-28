import type { IColumn } from "../../../../metadata-utils/src/types";

export const stringColumn: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
};

export const intColumn: IColumn = {
  id: "age",
  label: "Age",
  columnType: "INT",
};

export const dateColumn: IColumn = {
  id: "birthdate",
  label: "Birth Date",
  columnType: "DATE",
};

export const boolColumn: IColumn = {
  id: "active",
  label: "Active",
  columnType: "BOOL",
};

export const refColumn: IColumn = {
  id: "pet",
  label: "Pet",
  columnType: "REF",
  refSchemaId: "test",
  refTableId: "Pet",
};

export const ontologyColumn: IColumn = {
  id: "disease",
  label: "Disease",
  columnType: "ONTOLOGY",
  refTableId: "Diseases",
  refSchemaId: "Ontologies",
};

export const ontologyArrayColumn: IColumn = {
  id: "tags",
  label: "Tags",
  columnType: "ONTOLOGY_ARRAY",
  refTableId: "Tag",
  refSchemaId: "Ontologies",
};

export function makeColumn(
  overrides: Partial<IColumn> & Pick<IColumn, "id" | "columnType">
): IColumn {
  return {
    label: overrides.id,
    ...overrides,
  };
}
