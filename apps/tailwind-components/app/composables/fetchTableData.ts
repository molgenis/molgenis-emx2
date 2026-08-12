import {
  isCollectionType,
  isFileType,
  isLayoutColumnType,
  isOntologyType,
  isTableRefType,
} from "../../../metadata-utils/src";
import type { IQueryMetaData } from "../../../metadata-utils/src/IQueryMetaData";
import type { columnValue, IColumn } from "../../../metadata-utils/src/types";
import { DATA_NOT_FOUND_ERROR } from "../utils/constants";
import { fetchErrorToNuxtError } from "../utils/fetchErrorToNuxtError";
import fetchMetadata from "./fetchMetadata";

export const DEFAULT_NESTED_LIMIT = 5;

export interface ITableDataResponse {
  rows: Record<string, columnValue>[];
  count: number;
}

export default async (
  schemaId: string,
  tableId: string,
  properties?: IQueryMetaData
): Promise<ITableDataResponse> => {
  const limit = properties?.limit ? properties?.limit : 20;
  const offset = properties?.offset ? properties?.offset : 0;
  const expandLevel =
    properties?.expandLevel || properties?.expandLevel == 0
      ? properties?.expandLevel
      : 2;
  const search = properties?.searchTerms
    ? ',search:"' + properties?.searchTerms.trim() + '"'
    : "";

  const columnIds: string = await getColumnIds(
    schemaId,
    tableId,
    expandLevel,
    properties?.columns,
    true
  );
  const query = `query ${tableId}( $filter:${tableId}Filter, $orderby:[${tableId}orderby] ) {
        ${tableId}(
          filter:$filter,
          limit:${limit},
          offset:${offset}${search},
          orderby:$orderby
          )
          {
            ${columnIds}
          }
          ${tableId}_agg( filter:$filter${search} ) {
            count
          }
        }`;

  const filter = properties?.filter ? properties?.filter : {};
  const orderby = properties?.orderby ? [properties?.orderby] : [];

  const { data } = await $fetch(`/${schemaId}/graphql`, {
    method: "POST",
    body: {
      query,
      variables: { filter, orderby },
    },
  }).catch((error) => {
    const message = `Could not fetch data for table: ${tableId} in schema: ${schemaId}. ${DATA_NOT_FOUND_ERROR}`;
    console.error(message, error);
    throw fetchErrorToNuxtError(error, message);
  });

  return { rows: data[tableId], count: data[`${tableId}_agg`].count };
};

const SUBCLASS_COLUMN_ID = "mg_tableclass";

function isSelectedAtEveryDepth(col: IColumn): boolean {
  return Boolean(col.key) || col.id === SUBCLASS_COLUMN_ID;
}

function buildScalarFieldGql(col: IColumn): string {
  if (isOntologyType(col.columnType)) {
    return ` ${col.id} {name, label, definition, order}`;
  } else if (isFileType(col.columnType)) {
    return ` ${col.id} { id, size, filename, extension, url }`;
  } else if (!isLayoutColumnType(col.columnType)) {
    return ` ${col.id}`;
  }
  return "";
}

function buildRefFieldGql(
  col: IColumn,
  subFields: string,
  rootLevel: boolean
): string {
  const isCollection = rootLevel && isCollectionType(col.columnType);
  const limitArg = isCollection ? `(limit: ${DEFAULT_NESTED_LIMIT})` : "";
  let result = ` ${col.id}${limitArg} {${subFields} }`;
  if (isCollection) result += ` ${col.id}_agg { count }`;
  return result;
}

export const getColumnIds = async (
  schemaId: string,
  tableId: string,
  expandLevel: number,
  columnFilter: IColumn[] = [],
  rootLevel = true
) => {
  const metadata = await fetchMetadata(schemaId);

  const columns = columnFilter?.length
    ? columnFilter
    : metadata.tables.find((table) => table.id === tableId)?.columns || [];

  const columnsForTable = async (
    refSchemaId: string,
    refTableId: string
  ): Promise<IColumn[]> => {
    const refMetadata = await fetchMetadata(refSchemaId);
    return (
      refMetadata.tables.find((table) => table.id === refTableId)?.columns || []
    );
  };

  return buildColumnGqlAsync(
    columns,
    columnsForTable,
    schemaId,
    tableId,
    rootLevel,
    expandLevel
  );
};

export async function buildColumnGqlAsync(
  columns: IColumn[],
  columnsForTable: (schemaId: string, tableId: string) => Promise<IColumn[]>,
  schemaId: string,
  tableId: string,
  rootLevel: boolean,
  expandLevel: number
): Promise<string> {
  let gqlFields = "";

  for (const col of columns) {
    if (expandLevel > 0 || isSelectedAtEveryDepth(col)) {
      if (!rootLevel && isCollectionType(col.columnType)) {
        // skip collection types at non-root levels
      } else if (isTableRefType(col.columnType)) {
        const subColumns = await columnsForTable(
          col.refSchemaId || schemaId,
          col.refTableId || tableId
        );
        const subFields = await buildColumnGqlAsync(
          subColumns,
          columnsForTable,
          col.refSchemaId || schemaId,
          col.refTableId || tableId,
          false,
          expandLevel - 1
        );
        gqlFields += buildRefFieldGql(col, subFields, rootLevel);
      } else {
        gqlFields += buildScalarFieldGql(col);
      }
    }
  }

  return gqlFields;
}
