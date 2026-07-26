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

export { DISPLAY_TYPES } from "./types";

export type {
  DisplayType,
  IColumn,
  ISetting,
  ISchemaMetaData,
  ITableMetaData,
  KeyObject,
} from "./types";

export {
  fieldTypes,
  isEmpty,
  isArrayType,
  isAutoIdType,
  isCollectionType,
  isFileType,
  isLayoutColumnType,
  isMultiValuedRefType,
  isMultiValuedType,
  isOntologyArrayType,
  isOntologyType,
  isOptionKeyRefType,
  isPartsType,
  isPlainHeadingType,
  isRangeConditionType,
  isRefArrayType,
  isRefType,
  isRefbackType,
  isSectionType,
  isSingleOntologyType,
  isSingleRefType,
  isStoredMultiValuedType,
  isStoredTableRefType,
  isTableRefType,
  isValueArrayType,
  isValueType,
} from "./fieldHelpers";
