export type KeyObject = {
  [key: string]: KeyObject | string;
};

export interface ISetting {
  key: string;
  value: string;
}

type HeadingType = "HEADING";

export type CellValueType =
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
  | "PERIOD"
  | "PERIOD_ARRAY"
  | "JSON"
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

export type ColumnType = CellValueType | HeadingType;
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
  refTableId?: string | boolean;
  required?: boolean;
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
  name?: string;
  label: string;
  description?: string;
  tableType: string;
  columns: IColumn[];
  semantics?: string[];
  settings?: ISetting[];
}

export interface ISchemaMetaData {
  id: string;
  label: string;
  description?: string;
  tables: ITableMetaData[];
}

export interface IFieldError {
  message: string;
}

export interface IFormLegendSection {
  label: string;
  domId: string;
  isActive?: boolean;
  errorCount?: number;
}

export type columnId = string;
export type columnValue =
  | string
  | number
  | boolean
  | null
  | columnValueObject
  | columnValue[];

export interface columnValueObject {
  [x: string]: columnValue;
}

export type IInputValue = string | number | boolean;

export type IInputValueLabel = {
  value: IInputValue | IInputValue[] | null;
  label?: string;
};
