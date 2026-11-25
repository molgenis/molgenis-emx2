import type { ColumnDisplay, IColumn } from "../../../metadata-utils/src/types";

export const LAYOUTS = [
  "TABLE",
  "CARDS",
  "LIST",
  "LINKS",
  "BULLETS",
] as const satisfies readonly ColumnDisplay[];
export type Layout = ColumnDisplay;

export interface ResolvedDisplay {
  titleColumn?: IColumn;
  subtitleColumn?: IColumn;
  descriptionColumn?: IColumn;
  detailColumns: IColumn[];
  logoColumn?: IColumn;
}
