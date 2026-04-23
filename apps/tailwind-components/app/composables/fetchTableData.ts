import { createError } from "#app";
import { fetchMetadata } from "#imports";
import type { columnValue, IColumn } from "../../../metadata-utils/src/types";
import type { IQueryMetaData } from "../../../metadata-utils/src/IQueryMetaData";

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
    true,
    properties?.nestedLimit
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
    const message = `Could not fetch data for table ${tableId} in schema ${schemaId}`;
    console.error(message, error);
    throw createError({
      ...error,
      statusMessage: message,
    });
  });

  return { rows: data[tableId], count: data[`${tableId}_agg`].count };
};

const COLLECTION_TYPES = ["REF_ARRAY", "REFBACK", "MULTISELECT", "CHECKBOX"];
const REF_TYPES = [
  "REF",
  "REF_ARRAY",
  "REFBACK",
  "MULTISELECT",
  "CHECKBOX",
  "SELECT",
  "RADIO",
];
const ONTOLOGY_TYPES = ["ONTOLOGY", "ONTOLOGY_ARRAY"];

export function buildColumnGql(
  columns: IColumn[],
  columnsForTable: (tableId: string) => IColumn[],
  rootLevel: boolean,
  expandLevel: number,
  nestedLimit: number | undefined
): string {
  let gqlFields = "";

  for (const col of columns) {
    if (expandLevel > 0 || col.key) {
      if (!rootLevel && COLLECTION_TYPES.includes(col.columnType)) {
        // skip collection types at non-root levels
      } else if (REF_TYPES.includes(col.columnType)) {
        const subColumns = columnsForTable(col.refTableId || "");
        const subFields = buildColumnGql(
          subColumns,
          columnsForTable,
          false,
          expandLevel - 1,
          undefined
        );
        const isCollection =
          rootLevel && COLLECTION_TYPES.includes(col.columnType);
        const limitArg =
          isCollection && nestedLimit != null ? `(limit: ${nestedLimit})` : "";
        gqlFields += ` ${col.id}${limitArg} {${subFields} }`;
        if (isCollection) {
          gqlFields += ` ${col.id}_agg { count }`;
        }
      } else if (ONTOLOGY_TYPES.includes(col.columnType)) {
        gqlFields +=
          " " +
          col.id +
          " {name, label, definition, order, parent {name, label, definition, order, parent {name, label, definition, order, parent {name, label, definition, order}}}}";
      } else if (col.columnType === "FILE") {
        gqlFields += ` ${col.id} { id, size, filename, extension, url }`;
      } else if (!["HEADING", "SECTION"].includes(col.columnType)) {
        gqlFields += ` ${col.id}`;
      }
    }
  }

  return gqlFields;
}

export const getColumnIds = async (
  schemaId: string,
  tableId: string,
  expandLevel: number,
  columnFilter: IColumn[] = [],
  rootLevel = true,
  nestedLimit?: number
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
    expandLevel,
    nestedLimit
  );
};

async function buildColumnGqlAsync(
  columns: IColumn[],
  columnsForTable: (schemaId: string, tableId: string) => Promise<IColumn[]>,
  schemaId: string,
  tableId: string,
  rootLevel: boolean,
  expandLevel: number,
  nestedLimit: number | undefined
): Promise<string> {
  let gqlFields = "";

  for (const col of columns) {
    if (expandLevel > 0 || col.key) {
      if (!rootLevel && COLLECTION_TYPES.includes(col.columnType)) {
        // skip collection types at non-root levels
      } else if (REF_TYPES.includes(col.columnType)) {
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
          expandLevel - 1,
          undefined
        );
        const isCollection =
          rootLevel && COLLECTION_TYPES.includes(col.columnType);
        const limitArg =
          isCollection && nestedLimit != null ? `(limit: ${nestedLimit})` : "";
        gqlFields += ` ${col.id}${limitArg} {${subFields} }`;
        if (isCollection) {
          gqlFields += ` ${col.id}_agg { count }`;
        }
      } else if (ONTOLOGY_TYPES.includes(col.columnType)) {
        gqlFields +=
          " " +
          col.id +
          " {name, label, definition, order, parent {name, label, definition, order, parent {name, label, definition, order, parent {name, label, definition, order}}}}";
      } else if (col.columnType === "FILE") {
        gqlFields += ` ${col.id} { id, size, filename, extension, url }`;
      } else if (!["HEADING", "SECTION"].includes(col.columnType)) {
        gqlFields += ` ${col.id}`;
      }
    }
  }

  return gqlFields;
}
