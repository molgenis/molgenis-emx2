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

export { buildTree } from "./ontologyUtils";

export type {
  IColumn,
  ISetting,
  ISchemaMetaData,
  ITableMetaData,
  KeyObject,
  IOntologyItem,
  IOntologyParentTreeItem,
  IOntologyChildTreeItem,
} from "./types";

export { fieldTypes, isEmpty, isValueType } from "./fieldHelpers";
