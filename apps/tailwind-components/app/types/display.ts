import type { IColumn } from "../../../metadata-utils/src/types";

export interface ResolvedDisplay {
  titleColumn?: IColumn;
  subtitleColumn?: IColumn;
  descriptionColumn?: IColumn;
  detailColumns: IColumn[];
  logoColumn?: IColumn;
}
