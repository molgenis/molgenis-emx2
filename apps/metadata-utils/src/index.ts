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
