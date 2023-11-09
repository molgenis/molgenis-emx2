export const sayHello = (name: string): string => {
  return `Hello ${name}`;
};

export {
  buildRecordDetailsQueryFields,
  buildRecordListQueryFields,
  extractExternalSchemas,
  extractKeyFromRecord,
  buildFilterFromKeysObject,
  getTableMetaData,
} from "./tableQuery";

export type {
  IColumn,
  ILocale,
  ISetting,
  ISchemaMetaData,
  ITableMetaData,
  KeyObject,
} from "./types";

export { fieldTypes, isEmpty, isValueType } from "./fieldHelpers";
