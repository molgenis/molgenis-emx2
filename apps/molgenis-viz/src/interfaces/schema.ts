export interface baseIF {
  name: string;
}

export interface ColumnSchema {
  name: string;
  label?: string;
  columnType: string;
}

export interface TableSchema {
  name: string;
  label?: string;
  columns: ColumnSchema[];
}

export interface SchemaMeta {
  name: string;
  tables: TableSchema[];
}

export interface onChangeIF {
  for: string;
  value: string;
}
