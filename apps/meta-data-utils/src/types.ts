export type KeyObject = {
  [key: string]: KeyObject | string;
};

export interface ISetting {
  key: string;
  value: string;
}

export interface ILocale {
  locale: string;
  value: string;
}

export interface IColumn {
  columnType: string;
  id: string;
  name: string;
  computed?: string;
  conditions?: string[];
  descriptions?: ILocale[];
  key?: number;
  labels?: ILocale[];
  position?: number;
  readonly?: boolean;
  refBack?: string;
  refLabel?: string;
  refLabelDefault?: string;
  refLink?: string;
  refSchema?: string;
  refTable?: string;
  required?: boolean;
  semantics?: string[];
  validation?: string;
  visible?: string;
}

export interface ITableMetaData {
  id: string;
  name: string;
  tableType: string;
  columns: IColumn[];
  descriptions?: ILocale[];
  externalSchema: string;
  labels?: ILocale[];
  semantics?: string[];
  settings?: ISetting[];
}

export interface ISchemaMetaData {
  name: string;
  tables: ITableMetaData[];
}
