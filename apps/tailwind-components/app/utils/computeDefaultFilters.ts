import type { IColumn } from "../../../metadata-utils/src/types";

const EXCLUDED_TYPES = [
  "HEADING",
  "SECTION",
  "FILE",
  "STRING",
  "STRING_ARRAY",
  "TEXT",
  "TEXT_ARRAY",
  "EMAIL",
  "EMAIL_ARRAY",
  "HYPERLINK",
  "HYPERLINK_ARRAY",
  "UUID",
  "UUID_ARRAY",
  "AUTO_ID",
  "JSON",
  "PERIOD",
  "PERIOD_ARRAY",
];

export function computeDefaultFilters(columns: IColumn[]): string[] {
  return columns
    .filter(
      (col) =>
        !EXCLUDED_TYPES.includes(col.columnType) && !col.id.startsWith("mg_")
    )
    .map((col) => col.id);
}
