import type { ComputedRef } from "vue";
export type KeyObject = {
  [key: string]: KeyObject | string;
};

export interface ISetting {
  key: string;
  value: string;
}

export type HeadingType = "HEADING" | "SECTION";

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
  | "PERIOD_ARRAY"
  | "JSON"
  | "REF"
  | "REF_ARRAY"
  | "REFBACK"
  | "RADIO"
  | "SELECT"
  | "HEADING"
  | "SECTION"
  | "AUTO_ID"
  | "ONTOLOGY"
  | "ONTOLOGY_ARRAY"
  | "EMAIL"
  | "EMAIL_ARRAY"
  | "HYPERLINK"
  | "HYPERLINK_ARRAY"
  | "CHECKBOX"
  | "MULTISELECT";

export type ColumnType = CellValueType | HeadingType;
export interface IColumn {
  columnType: ColumnType;
  id: columnId;
  label: string;
  section?: string;
  heading?: string;
  computed?: string;
  conditions?: string[];
  description?: string;
  formLabel?: string;
  key?: number;
  position?: number;
  readonly?: string;
  refBackId?: string;
  refLabel?: string;
  refLabelDefault?: string;
  refLinkId?: string;
  refSchemaId?: string; //should always be provided when refTableId is set even if in same schema
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

export interface IRefColumn extends IColumn {
  refTableId: string;
  refSchemaId: string;
  refLabel: string;
  refLabelDefault: string;
  refLinkId: string;
}

export interface ITableMetaData {
  id: string;
  schemaId: string;
  name: string;
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

interface LegendItem {
  id: string;
  label: string;
  type: HeadingType;
  errorCount: ComputedRef<number>;
  isVisible: ComputedRef<boolean>;
  isActive: ComputedRef<boolean>;
}
export interface LegendSection extends LegendItem {
  type: "SECTION";
  headers: LegendHeading[];
}
export interface LegendHeading extends LegendItem {
  type: "HEADING";
}

export type columnId = string;
export type columnValue =
  | string
  | number
  | boolean
  | null
  | undefined
  | columnValueObject
  | columnValue[]
  | fileValue;

export type recordValue = Record<string, columnValue>;

export interface columnValueObject {
  [x: string]: columnValue;
}

export function isColumnValueObject(
  value: columnValue
): value is columnValueObject {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value)
  );
}

export function isColumnValueObjectArray(
  value: columnValue
): value is columnValueObject[] {
  return (
    Array.isArray(value) &&
    value.every(
      (item) =>
        typeof item === "object" &&
        item !== null &&
        !Array.isArray(item)
    )
  );
}


export type fileValue = {
  id: string;
  size: number;
  filename: string;
  extension: string;
  url: string;
};

export function isFileValue(value: columnValue): value is fileValue {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value) &&
    "id" in value &&
    "size" in value &&
    "filename" in value &&
    "extension" in value &&
    "url" in value
  );
}

export type IInputValue = string | number | boolean;

export type IInputValueLabel = {
  value: IInputValue | IInputValue[] | null;
  label?: string;
};

export type IRow = Record<columnId, columnValue>;

export type DateValue = Date | string | undefined | null;
