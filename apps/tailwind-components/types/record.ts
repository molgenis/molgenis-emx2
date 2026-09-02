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

/** The grouped shape groupRecordSections() builds: a SECTION with its own fields and HEADINGs. */
export interface RecordSectionGroup {
  id: string;
  label: string | null;
  fields: RecordField[];
  headings: RecordHeading[];
}

/** What display/RecordSection.vue renders as its `section` prop: one section or one heading. */
export interface RecordSection {
  kind: "section" | "heading";
  id: string;
  label: string | null;
  fields: RecordField[];
}

export interface GroupRecordSectionsOptions {
  showMgColumns?: boolean;
  filterTerm?: string;
}
