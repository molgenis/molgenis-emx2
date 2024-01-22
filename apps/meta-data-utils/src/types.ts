export type KeyObject = {
  [key: string]: KeyObject | string;
};

export interface ISetting {
  key: string;
  value: string;
}

export interface IColumn {
  columnType: string;
  id: string;
  label: string;
  computed?: string;
  conditions?: string[];
  description?: string;
  key?: number;
  position?: number;
  readonly?: string;
  refBackId?: string;
  refLabel?: string;
  refLabelDefault?: string;
  refLinkId?: string;
  refSchemaId?: string;
  refTableId?: string;
  required?: string;
  semantics?: string[];
  validation?: string;
  visible?: string;
  table?: string;
  name?: string;
  inherited?: boolean;
  defaultValue?: string;
}

export interface ITableMetaData {
  id: string;
  label: string;
  description?: string;
  tableType: string;
  columns: IColumn[];
  schemaId: string;
  inheritId?: string;
  semantics?: string[];
  settings?: ISetting[];
}

export interface ISchemaMetaData {
  id: string;
  label: string;
  description?: string;
  tables: ITableMetaData[];
}
