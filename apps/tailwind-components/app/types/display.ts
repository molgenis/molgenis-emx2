import type { IColumn } from "../../../metadata-utils/src/types";

export const LAYOUTS = ["TABLE", "CARDS", "LIST", "LINKS"] as const;
export type Layout = (typeof LAYOUTS)[number];

export interface DisplayConfig {
  layout: Layout;
  titleTemplate?: string; // "${acronym} ${name}", the syntax refLabel uses
  descriptionTemplate?: string;
  detailColumns?: string[]; // column ids, rendered in this order
  logoColumn?: string; // column id
}

export interface ResolvedDisplay {
  layout: Layout;
  titleTemplate: string;
  descriptionTemplate?: string;
  detailColumns: IColumn[];
  logoColumn?: IColumn;
}
