import type { IColumn } from "../../../metadata-utils/src/types";

export const LAYOUTS = ["TABLE", "CARDS", "LIST", "LINKS", "BULLETS"] as const;
export type Layout = (typeof LAYOUTS)[number];

export interface DisplayConfig {
  layout: Layout;
  titleTemplate?: string;
  subtitleTemplate?: string;
  descriptionColumnId?: string;
  detailColumnIds?: string[];
  logoColumnId?: string;
}

export interface ResolvedDisplay {
  layout: Layout;
  titleTemplate: string;
  subtitleTemplate?: string;
  descriptionColumn?: IColumn;
  detailColumns: IColumn[];
  logoColumn?: IColumn;
}
