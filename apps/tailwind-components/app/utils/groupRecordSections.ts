import type {
  columnValue,
  IColumn,
  IRow,
} from "../../../metadata-utils/src/types";
import type { RecordHeading, RecordSectionGroup } from "../../types/record";

const TOP_SECTION_ID = "mg_top_of_form";

export function groupRecordSections(
  columns: IColumn[],
  row: IRow
): RecordSectionGroup[] {
  const sections: RecordSectionGroup[] = [];
  let heading: RecordHeading | undefined;

  for (const column of columns) {
    if (column.columnType === "SECTION") {
      sections.push(newSection(column.id, column.label));
      heading = undefined;
      continue;
    }
    if (column.columnType === "HEADING") {
      heading = { id: column.id, label: column.label, fields: [] };
      currentOrTopSection(sections).headings.push(heading);
      continue;
    }
    if (isEmptyValue(row[column.id])) {
      continue;
    }
    const field = {
      id: column.id,
      label: column.label,
      metadata: column,
      value: row[column.id],
    };
    (heading ?? currentOrTopSection(sections)).fields.push(field);
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

function newSection(id: string, label: string): RecordSectionGroup {
  return {
    id,
    label: id === TOP_SECTION_ID ? null : label,
    fields: [],
    headings: [],
  };
}

function currentOrTopSection(
  sections: RecordSectionGroup[]
): RecordSectionGroup {
  const last = sections[sections.length - 1];
  if (last) {
    return last;
  }
  const top = newSection(TOP_SECTION_ID, "");
  sections.push(top);
  return top;
}

function isEmptyValue(value: columnValue): boolean {
  if (value === null || value === undefined || value === "") {
    return true;
  }
  return Array.isArray(value) && value.length === 0;
}
