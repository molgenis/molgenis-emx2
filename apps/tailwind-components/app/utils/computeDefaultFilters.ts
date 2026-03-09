import type { IColumn } from "../../../metadata-utils/src/types";

const ONTOLOGY_TYPES = ["ONTOLOGY", "ONTOLOGY_ARRAY"];
const REF_TYPES_FOR_DEFAULT = [
  "REF",
  "REF_ARRAY",
  "SELECT",
  "RADIO",
  "CHECKBOX",
  "MULTISELECT",
  "REFBACK",
];
const MAX_DEFAULT_FILTERS = 5;

export function computeDefaultFilters(columns: IColumn[]): string[] {
  const unfilterable = ["HEADING", "SECTION"];
  const filterable = columns.filter(
    (col) => !unfilterable.includes(col.columnType) && !col.id.startsWith("mg_")
  );

  const ontologyCols = filterable.filter((c) =>
    ONTOLOGY_TYPES.includes(c.columnType)
  );
  const refCols = filterable.filter((c) =>
    REF_TYPES_FOR_DEFAULT.includes(c.columnType)
  );

  const defaults = ontologyCols.slice(0, MAX_DEFAULT_FILTERS).map((c) => c.id);
  if (defaults.length < MAX_DEFAULT_FILTERS) {
    const remaining = MAX_DEFAULT_FILTERS - defaults.length;
    defaults.push(...refCols.slice(0, remaining).map((c) => c.id));
  }
  return defaults;
}
