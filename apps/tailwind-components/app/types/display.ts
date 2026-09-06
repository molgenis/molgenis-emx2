import type { IColumn } from "../../../metadata-utils/src/types";

export const LAYOUTS = ["TABLE", "CARDS", "LIST", "LINKS", "BULLETS"] as const;
export type Layout = (typeof LAYOUTS)[number];

// What a caller writes: columns named by id, everything optional.
export interface DisplayConfig {
  layout: Layout;
  titleTemplate?: string; // "${acronym} ${name}", the syntax refLabel uses
  subtitleTemplate?: string;
  descriptionColumnId?: string;
  detailColumnIds?: string[]; // rendered in this order
  logoColumnId?: string;
}

// What every layout component receives: slots filled in, columns resolved.
export interface ResolvedDisplay {
  layout: Layout;
  titleTemplate: string;
  subtitleTemplate?: string;
  descriptionColumn?: IColumn;
  detailColumns: IColumn[];
  logoColumn?: IColumn;
}
