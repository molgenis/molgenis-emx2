import type { IColumn } from "../../../metadata-utils/src/types";
import type { IFilterValue } from "../../types/filters";
import fetchTableMetadata from "../composables/fetchTableMetadata";

export async function resolveFilterLabels(
  filters: Map<string, IFilterValue>,
  columns: IColumn[]
): Promise<Map<string, string>> {
  const result = new Map<string, string>();

  for (const columnId of filters.keys()) {
    if (!columnId.includes(".")) {
      const column = columns.find((c) => c.id === columnId);
      if (column) {
        result.set(
          columnId,
          (column as any).displayConfig?.label || column.label || column.id
        );
      }
      continue;
    }

    const segments = columnId.split(".");
    let currentColumns: IColumn[] = columns;
    let currentSchemaId = "";
    const labels: string[] = [];

    for (let depth = 0; depth < segments.length; depth++) {
      const segment = segments[depth]!;
      const column = currentColumns.find((c) => c.id === segment);
      if (!column) break;

      labels.push(
        (column as any).displayConfig?.label || column.label || segment
      );

      if (depth < segments.length - 1 && column.refTableId) {
        const refSchemaId = column.refSchemaId || currentSchemaId;
        try {
          const metadata = await fetchTableMetadata(
            refSchemaId,
            column.refTableId
          );
          currentColumns = metadata.columns;
          currentSchemaId = refSchemaId;
        } catch {
          break;
        }
      }
    }

    if (labels.length === segments.length) {
      result.set(columnId, labels.join(" → "));
    }
  }

  return result;
}
