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
  ISetting,
  ISchemaMetaData,
  ITableMetaData,
  KeyObject,
} from "./types";

export { fieldTypes, isEmpty, isValueType } from "./fieldHelpers";

export {
  expandModuleColumns,
  activeModules,
  isModuleColumn,
  omitInactiveModuleValues,
} from "./moduleColumns";

export type { IModuleColumnGroup } from "./moduleColumns";

export {
  getSelectableTableTypes,
  DEFAULT_TABLE_TYPE,
} from "./tableTypeHelpers";
