export type KeyObject = {
  [key: string]: KeyObject | string;
};

export interface ISetting {
  key: string;
  value: string;
}

export type HeadingType = "HEADING";

export type ValueType =
  | "BOOL"
  | "BOOL_ARRAY"
  | "UUID"
  | "UUID_ARRAY"
  | "FILE"
  | "STRING"
  | "STRING_ARRAY"
  | "TEXT"
  | "TEXT_ARRAY"
  | "INT"
  | "INT_ARRAY"
  | "LONG"
  | "LONG_ARRAY"
  | "DECIMAL"
  | "DECIMAL_ARRAY"
  | "DATE"
  | "DATE_ARRAY"
  | "DATETIME"
  | "DATETIME_ARRAY"
  | "PERIOD"
  | "JSONB"
  | "JSONB_ARRAY"
  | "REF"
  | "REF_ARRAY"
  | "REFBACK"
  | "HEADING"
  | "AUTO_ID"
  | "ONTOLOGY"
  | "ONTOLOGY_ARRAY"
  | "EMAIL"
  | "EMAIL_ARRAY"
  | "HYPERLINK"
  | "HYPERLINK_ARRAY";

export type ColumnType = ValueType | HeadingType;
export interface IColumn {
  columnType: ColumnType;
  id: columnId;
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
  required?: string | boolean;
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
  semantics?: string[];
  settings?: ISetting[];
}

export interface ISchemaMetaData {
  id: string;
  label: string;
  description?: string;
  tables: ITableMetaData[];
}

export type columnId = string;
export type columnValue = string | number | boolean | Object;
