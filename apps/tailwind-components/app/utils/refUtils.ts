import type { columnValue, IColumn } from "../../../metadata-utils/src/types";
import { rowToString } from "./rowToString";

export function getLabel(
  metadata: IColumn,
  data: Record<string, columnValue>
): string | undefined {
  const labelTemplate = (
    metadata.refLabel ? metadata.refLabel : metadata.refLabelDefault
  ) as string;
  return rowToString(data, labelTemplate);
}
