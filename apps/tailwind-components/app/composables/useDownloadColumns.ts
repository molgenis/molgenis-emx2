import { computed, type Ref } from "vue";
import type { IColumn } from "../../../metadata-utils/src/types";

/**
 * Derives the `columns` query param of a csv/excel table download from the
 * columns shown in the table explorer.
 *
 * Only the column ids are sent: the backend expands a reference to a table
 * with a composite key into one `refColumn.keyColumn` column per key part, and
 * a file column into its file and filename columns.
 *
 */
export function useDownloadColumns(
  allColumns: Ref<IColumn[]>,
  visibleColumns: Ref<IColumn[]>
) {
  const columnIds = computed(() => visibleColumns.value.map((col) => col.id));

  /** true when the shown columns differ from the full table, in content or order */
  const isColumnSelectionActive = computed(() => {
    const allIds = allColumns.value.map((col) => col.id);
    return (
      columnIds.value.length > 0 &&
      (columnIds.value.length !== allIds.length ||
        columnIds.value.some((id, index) => id !== allIds[index]))
    );
  });

  function appendColumnsParam(queryParams: URLSearchParams) {
    queryParams.append("columns", columnIds.value.join(","));
  }

  return { columnIds, isColumnSelectionActive, appendColumnsParam };
}
