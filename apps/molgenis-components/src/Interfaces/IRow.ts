import type { ITableMetaData } from "../../../metadata-utils/src";

export type IRow = Record<string, string | ITableMetaData | any>;
