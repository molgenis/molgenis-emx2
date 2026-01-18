import { computed, ref, watch, type ComputedRef, type Ref } from "vue";
import type { ITableMetaData, IRow } from "../../../metadata-utils/src/types";
import fetchTableMetadata from "./fetchTableMetadata";
import fetchTableData from "./fetchTableData";

export type TableDataStatus = "idle" | "pending" | "success" | "error";

export interface UseTableDataOptions {
  pageSize: number;
  page: Ref<number>;
  filter?: object;
  searchTerms?: Ref<string>;
  orderby?: Ref<{ column: string; direction: "ASC" | "DESC" }>;
}

export interface UseTableDataReturn {
  metadata: ComputedRef<ITableMetaData | undefined>;
  rows: ComputedRef<IRow[]>;
  count: ComputedRef<number>;
  refresh: () => Promise<void>;
  status: Ref<TableDataStatus>;
}

export function useTableData(
  schemaId: string,
  tableId: string,
  options: UseTableDataOptions
): UseTableDataReturn {
  const { pageSize, page, filter, searchTerms, orderby } = options;

  const status = ref<TableDataStatus>("idle");
  const metadataRef = ref<ITableMetaData | undefined>(undefined);
  const rowsRef = ref<IRow[]>([]);
  const countRef = ref<number>(0);

  async function fetchData(): Promise<void> {
    status.value = "pending";
    try {
      const [tableMetadata, tableData] = await Promise.all([
        fetchTableMetadata(schemaId, tableId),
        fetchTableData(schemaId, tableId, {
          limit: pageSize,
          offset: (page.value - 1) * pageSize,
          orderby: orderby?.value?.column
            ? { [orderby.value.column]: orderby.value.direction }
            : {},
          searchTerms: searchTerms?.value || "",
          filter: filter,
        }),
      ]);

      metadataRef.value = tableMetadata;
      rowsRef.value = Array.isArray(tableData.rows) ? tableData.rows : [];
      countRef.value = tableData.count ?? 0;
      status.value = "success";
    } catch (error) {
      console.error("useTableData fetch error:", error);
      status.value = "error";
    }
  }

  // watch reactive params and trigger refresh
  watch(page, () => fetchData(), { immediate: false });
  watch(
    () => searchTerms?.value,
    () => fetchData(),
    { immediate: false }
  );
  watch(
    () => orderby?.value,
    () => fetchData(),
    { immediate: false, deep: true }
  );

  // initial fetch
  fetchData();

  const metadata = computed(() => metadataRef.value);
  const rows = computed(() => rowsRef.value);
  const count = computed(() => countRef.value);

  return {
    metadata,
    rows,
    count,
    refresh: fetchData,
    status,
  };
}
