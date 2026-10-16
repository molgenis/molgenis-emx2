import type { columnValue, IColumn } from "../../metadata-utils/src/types";

export interface RecordField {
  id: string;
  label: string;
  metadata: IColumn;
  value: columnValue;
}

export interface RecordHeading {
  id: string;
  label: string;
  fields: RecordField[];
}

export type RecordLayout = "CARDS" | "PLAIN";

export interface RecordSectionGroup {
  id: string;
  label: string | null;
  fields: RecordField[];
  headings: RecordHeading[];
}

export interface RecordSection {
  kind: "section" | "heading";
  id: string;
  label: string | null;
  fields: RecordField[];
}
