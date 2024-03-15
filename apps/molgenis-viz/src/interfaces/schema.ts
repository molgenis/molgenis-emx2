export interface baseIF {
  name: String;
}

export interface ColumnSchema {
  name: string;
  label?: string;
  columnType: string;
}

export interface TableSchema {
  name: string;
  label?: String;
  columns: ColumnSchema[];
}

export interface SchemaMeta {
  name: string;
  tables: TableSchema[];
}

export interface onChangeIF {
  for: String;
  value: String;
}
