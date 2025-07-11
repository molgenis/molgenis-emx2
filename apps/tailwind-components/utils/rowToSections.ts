import type {
  columnValue,
  IColumn,
  IRow,
  ITableMetaData,
} from "../../metadata-utils/src/types";

export function rowToSections(
  rowData: IRow,
  metadata: ITableMetaData,
  options: { showDataOwner?: boolean } = { showDataOwner: false }
) {
  if (!metadata) {
    return [];
  }

  return metadata.columns
    .map((column) => {
      return {
        key: column.id,
        value: rowData[column.id],
        metadata: column,
      };
    })
    .filter((item) => {
      return !item.key.startsWith("mg_") || options.showDataOwner;
    })
    .filter((item) => {
      return (
        rowData.hasOwnProperty(item.key) ||
        item.metadata.columnType === "HEADING"
      );
    })
    .reduce((acc, item) => {
      if (item.metadata.columnType === "HEADING") {
        // If the item is a heading, create a new section
        acc.push({ heading: item.metadata.label as string, fields: [] });
      } else {
        // If first item is not a section heading, create a default section
        if (acc.length === 0) {
          acc.push({ heading: "", fields: [] });
        }
        // Add the item to the last section
        acc[acc.length - 1].fields.push(item);
      }
      return acc;
    }, [] as { heading: string; fields: { key: string; value: columnValue; metadata: IColumn }[] }[])
    .filter((section) => {
      // Filter out empty sections
      return section.fields.length > 0;
    });
}
