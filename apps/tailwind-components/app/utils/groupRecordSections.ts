import type {
  columnValue,
  IColumn,
  IRow,
  ITableMetaData,
} from "../../../metadata-utils/src/types";

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

/** `label` is null for the section the backend synthesises above the first column. */
export interface RecordSection {
  id: string;
  label: string | null;
  fields: RecordField[];
  headings: RecordHeading[];
}

/** One rendered box. `kind` sets its title level, so the two-level tree survives flattening. */
export interface RecordBox {
  kind: "section" | "heading";
  id: string;
  label: string | null;
  fields: RecordField[];
}

const TOP_SECTION_ID = "mg_top_of_form";

/**
 * Groups a row's columns into the two-level tree the backend describes: SECTION
 * first, HEADING second. Drops what the reader cannot use: fields with no value,
 * sections and headings left empty, and `mg_` columns unless asked for.
 */
export function groupRecordSections(
  metadata: ITableMetaData,
  rowData: IRow | undefined | null,
  options: { showMgColumns?: boolean } = {}
): RecordSection[] {
  const sections: RecordSection[] = [];
  let heading: RecordHeading | undefined;

  for (const column of metadata.columns) {
    if (column.columnType === "SECTION") {
      sections.push(newSection(column.id, column.label));
      heading = undefined;
      continue;
    }
    if (column.columnType === "HEADING") {
      heading = { id: column.id, label: column.label, fields: [] };
      currentSection(sections).headings.push(heading);
      continue;
    }
    if (!isVisibleField(column, rowData, options.showMgColumns)) {
      continue;
    }
    const field = {
      id: column.id,
      label: column.label,
      metadata: column,
      value: rowData?.[column.id],
    };
    (heading ?? currentSection(sections)).fields.push(field);
  }

  return sections
    .map((section) => ({
      ...section,
      headings: section.headings.filter((h) => h.fields.length > 0),
    }))
    .filter(
      (section) => section.fields.length > 0 || section.headings.length > 0
    );
}

function newSection(id: string, label: string): RecordSection {
  return {
    id,
    label: id === TOP_SECTION_ID ? null : label,
    fields: [],
    headings: [],
  };
}

/** A column before the first SECTION belongs to a section of its own. */
function currentSection(sections: RecordSection[]): RecordSection {
  const last = sections[sections.length - 1];
  if (last) {
    return last;
  }
  const top = newSection(TOP_SECTION_ID, "");
  sections.push(top);
  return top;
}

function isVisibleField(
  column: IColumn,
  rowData: IRow | undefined | null,
  showMgColumns: boolean | undefined
): boolean {
  if (column.id.startsWith("mg_") && !showMgColumns) {
    return false;
  }
  // A column the row does not carry reads as undefined, so this covers that too.
  return !isEmptyValue(rowData?.[column.id]);
}

function isEmptyValue(value: columnValue): boolean {
  if (value === null || value === undefined || value === "") {
    return true;
  }
  return Array.isArray(value) && value.length === 0;
}
