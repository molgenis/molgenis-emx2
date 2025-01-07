import type { ITableMetaData, IColumn } from "../../metadata-utils/dist";

export const toHeadings = (tableMetaData: ITableMetaData) => {
  return tableMetaData.columns.filter(
    (column) => column.columnType === "HEADING"
  );
};

export const toSectionsMap = (tableMetaData: ITableMetaData) => {
  let currentSectionName = "DEFAULT_SECTION";
  let sections: Record<string, IColumn[]> = { DEFAULT_SECTION: [] };

  for (const column of tableMetaData.columns) {
    if (column.columnType === "HEADING") {
      currentSectionName = column.id;
      sections[currentSectionName] = [];
    } else {
      sections[currentSectionName].push(column);
    }
  }

  return sections;
};
