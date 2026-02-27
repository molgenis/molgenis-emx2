import axios from "axios";
import type { AxiosError, AxiosResponse } from "axios";
import type {
  columnValue,
  IColumn,
  ISchemaMetaData,
  ISetting,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import type { IRow } from "../Interfaces/IRow";
import { deepClone, getKeyValue } from "../components/utils";
import type { AggFunction } from "./IClient";
import type { IClient, INewClient } from "./IClient";
import type { IQueryMetaData } from "../../../metadata-utils/src/IQueryMetaData";
import { getColumnIds } from "./queryBuilder";
import { toFormData } from "../../../metadata-utils/src/toFormData";

// application wide cache for schema meta data
const schemaCache = new Map<string, Promise<ISchemaMetaData>>();

export { request, fetchSchemaMetaData, convertRowToPrimaryKey };
const client: IClient = {
  newClient: (schemaId?: string): INewClient => {
    return {
      insertDataRow,
      updateDataRow,
      deleteRow: async (rowKey: IRow, tableId: string) => {
        return deleteRow(rowKey, tableId, schemaId);
      },
      deleteAllTableData: async (tableId: string) => {
        return deleteAllTableData(tableId, schemaId);
      },
      fetchSchemaMetaData: async () => {
        return fetchSchemaMetaData(schemaId);
      },
      fetchTableMetaData: async (tableId: string): Promise<ITableMetaData> => {
        const schema = await fetchSchemaMetaData(schemaId);
        return deepClone(schema).tables.find(
          (table: ITableMetaData) => table.id === tableId
        );
      },
      fetchTableData: async (
        tableId: string,
        properties: IQueryMetaData = {}
      ) => {
        const schemaMetaData = await fetchSchemaMetaData(schemaId);
        return fetchTableData(tableId, properties, schemaMetaData);
      },
      fetchTableDataValues: async (
        tableId: string,
        properties: IQueryMetaData = {}
      ) => {
        const schemaMetaData = await fetchSchemaMetaData(schemaId);
        const dataResp = await fetchTableData(
          tableId,
          properties,
          schemaMetaData
        );
        return dataResp[tableId];
      },
      fetchRowData: async (
        tableId: string,
        rowId: IRow,
        expandLevel: number = 2
      ) => {
        const schemaMetaData = await fetchSchemaMetaData(schemaId);
        const tableMetaData = schemaMetaData.tables.find(
          (table: ITableMetaData) => table.id === tableId
        );
        const filter = tableMetaData?.columns
          ?.filter((column: IColumn) => column.key === 1)
          .reduce(
            (
              accum: Record<string, { equals: columnValue }>,
              column: IColumn
            ) => {
              accum[column.id] = { equals: rowId[column.id] };
              return accum;
            },
            {}
          );
        const resultArray = (
          await fetchTableData(
            tableId,
            {
              filter,
              expandLevel,
            },
            schemaMetaData
          )
        )[tableId];

        if (!resultArray.length || resultArray.length !== 1) {
          return undefined;
        } else {
          return resultArray[0];
        }
      },
      fetchAggregateData: async (
        tableId: string,
        selectedColumn: { id: string; column: string }, //should these be id?
        selectedRow: { id: string; column: string }, //should these be id?
        filter: Object,
        aggFunction?: AggFunction,
        aggField?: string
      ) => {
        const aggregateQuery = `
        query ${tableId}_groupBy($filter: ${tableId}Filter){
          ${tableId}_groupBy(filter: $filter) {
            ${aggFunction} ${aggFunction === "_sum" ? `{ ${aggField} }` : ""},
            ${selectedColumn.id} {
              ${selectedColumn.column}
            },
            ${selectedRow.id} {
              ${selectedRow.column}
            }
          }
        }`;
        return request(graphqlURL(schemaId), aggregateQuery, { filter });
      },
      saveTableSettings: async (settings: ISetting[]) => {
        return request(
          graphqlURL(schemaId),
          `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){message}}`,
          { settings }
        );
      },
      fetchSettings: async () => {
        return fetchSettings(schemaId);
      },
      fetchSettingValue: async (name: string) => {
        const settings = await fetchSettings(schemaId);
        const setting = settings.find(
          (setting: ISetting) => setting.key === name
        );
        if (setting) {
          return JSON.parse(setting.value);
        }
      },
      saveSetting: async (key: string, value: unknown) => {
        const createMutation = `mutation change($settings: [MolgenisSettingsInput]) {
            change(settings: $settings) {
              message
            }
          }`;

        const variables = {
          settings: {
            key: key,
            value: JSON.stringify(value),
          },
        };

        await request(graphqlURL(schemaId), createMutation, variables).catch(
          (e) => {
            console.error(e);
          }
        );
      },
      clearCache: () => {
        schemaCache.clear();
      },
      convertRowToPrimaryKey: async (row: IRow, tableId: string) => {
        return convertRowToPrimaryKey(row, tableId, schemaId);
      },
      fetchOntologyOptions: async (tableName: string) => {
        return fetchOntologyOptions(tableName, schemaId);
      },
      getPrimaryKeyFields,
    };
  },
};
export default client;

async function getPrimaryKeyFields(
  schemaId: string,
  tableId: string
): Promise<string[]> {
  return fetchSchemaMetaData(schemaId).then((schema) => {
    const table = schema.tables.find((table) => table.id === tableId);
    if (!table) {
      throw new Error(`Table ${tableId} not found in schema ${schemaId}`);
    }
    const keyFields = table.columns
      .filter((column) => column.key === 1)
      .map((column) => column.id);
    return keyFields;
  });
}

const metadataQuery = `{
  _schema {
    id,
    tables {
      id,
      name,
      label, 
      description,
      tableType,
      semantics,
      columns {
        id,
        name,
        label,
        formLabel,
        description,
        columnType,
        key,
        refTableId,
        refSchemaId,
        refLinkId,
        refLabel,
        refLabelDefault,
        refBackId,
        required,
        defaultValue,
        readonly,
        semantics,
        position,
        computed,
        visible,
        validation
      }
      settings { 
        key,
        value 
      }
    }
  }
}`;

const graphqlURL = (schemaId?: string) => {
  return schemaId ? "/" + schemaId + "/graphql" : "graphql";
};

const insertDataRow = (rowData: IRow, tableId: string, schemaId: string) => {
  const formData = toFormData(rowData);
  const query = `mutation insert($value:[${tableId}Input]){insert(${tableId}:$value){message}}`;
  formData.append("query", query);
  return axios.post(graphqlURL(schemaId), formData);
};

const updateDataRow = (rowData: IRow, tableId: string, schemaId: string) => {
  const formData = toFormData(rowData);
  const query = `mutation update($value:[${tableId}Input]){update(${tableId}:$value){message}}`;
  formData.append("query", query);
  return axios.post(graphqlURL(schemaId), formData);
};

const deleteRow = async (row: IRow, tableId: string, schemaId?: string) => {
  const query = `mutation delete($pkey:[${tableId}Input]){delete(${tableId}:$pkey){message}}`;
  const key = await convertRowToPrimaryKey(row, tableId, schemaId);
  const variables = { pkey: [key] };
  return axios.post(graphqlURL(schemaId), { query, variables });
};

const deleteAllTableData = (tableId: string, schemaId?: string) => {
  const query = `mutation {truncate(tables:"${tableId}" async:true){taskId message}}`;
  return axios.post(graphqlURL(schemaId), { query });
};

const fetchSchemaMetaData = async (
  schemaId?: string
): Promise<ISchemaMetaData> => {
  const currentschemaId = schemaId ? schemaId : "CACHE_OF_CURRENT_SCHEMA";
  if (schemaCache.has(currentschemaId)) {
    return schemaCache.get(currentschemaId) as Promise<ISchemaMetaData>;
  }

  const promise = axios
    .post(graphqlURL(schemaId), { query: metadataQuery })
    .then((result: AxiosResponse<{ data: { _schema: ISchemaMetaData } }>) => {
      const schema = result.data.data._schema;
      return deepClone(schema);
    })
    .catch((error: AxiosError) => {
      console.log(error);
      schemaCache.delete(currentschemaId);
      throw error;
    });

  schemaCache.set(currentschemaId, promise);
  return promise;
};

const fetchTableData = async (
  tableId: string,
  properties: IQueryMetaData,
  metadata: ISchemaMetaData
) => {
  const limit = properties.limit ? properties.limit : 20;
  const offset = properties.offset ? properties.offset : 0;
  const expandLevel = properties.expandLevel ?? 2;
  const search = properties.searchTerms
    ? ',search:"' + properties.searchTerms.trim() + '"'
    : "";

  const schemaId = metadata.id;
  const columnIds = await getColumnIds(schemaId, tableId, expandLevel);
  const tableDataQuery = `query ${tableId}( $filter:${tableId}Filter, $orderby:${tableId}orderby ) {
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

  const filter = properties.filter ? properties.filter : {};
  const orderby = properties.orderby ? properties.orderby : {};
  const resp = await axios
    .post(graphqlURL(schemaId), {
      query: tableDataQuery,
      variables: { filter, orderby },
    })
    .catch((error: AxiosError) => {
      console.log(error);
      throw error;
    });
  return resp?.data.data;
};

const fetchOntologyOptions = async (
  tableId: string,
  schemaId: string | undefined
) => {
  const tableDataQuery = `query ${tableId} {
        ${tableId}(
          limit:100000
          orderby: { order: ASC }
          )
          {
          	order 
            name 
            label 
            parent { order name label } 
            children { order name label parent { name } } 
          }
        }`;
  const resp = await axios
    .post(graphqlURL(schemaId), {
      query: tableDataQuery,
    })
    .catch((error: AxiosError) => {
      console.log(error);
      throw error;
    });
  return resp?.data.data[tableId];
};

const fetchSettings = async (schemaId?: string) => {
  return (await request(graphqlURL(schemaId), "{_settings{key, value}}"))
    ._settings;
};

const request = async (
  url: string,
  graphql: string,
  variables?: Record<string, unknown>
) => {
  const data: { query: string; variables?: Record<string, unknown> } = {
    query: graphql,
  };
  if (variables) {
    data.variables = variables;
  }
  return axios
    .post(url, data)
    .then((result: AxiosResponse) => {
      return result?.data?.data;
    })
    .catch((error: AxiosError<{ errors?: { message: string }[] }>): string => {
      const detailedErrorMessage = error.response?.data?.errors
        ?.map((error: { message: string }) => {
          return error.message;
        })
        .join(". ");
      throw detailedErrorMessage || error.message;
    });
};

async function convertRowToPrimaryKey(
  row: IRow,
  tableId: string,
  schemaId?: string
): Promise<IRow> {
  const schema = await fetchSchemaMetaData(schemaId);
  const tableMetadata = schema.tables.find(
    (table: ITableMetaData) => table.id === tableId
  );
  if (!tableMetadata?.columns) {
    throw new Error("Empty columns in metadata");
  } else {
    return await tableMetadata.columns.reduce(
      async (accumPromise: Promise<IRow>, column: IColumn): Promise<IRow> => {
        let accum: IRow = await accumPromise;
        const cellValue = row[column.id];
        if (column.key === 1 && (cellValue || cellValue === 0)) {
          accum[column.id] = await getKeyValue(
            cellValue,
            column,
            column.refSchemaId || schema.id
          );
        }
        return accum;
      },
      Promise.resolve({})
    );
  }
}
