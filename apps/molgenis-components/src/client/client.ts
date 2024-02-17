import axios, { AxiosError, AxiosResponse } from "axios";
import { IRow } from "../Interfaces/IRow";
import {
  IColumn,
  ITableMetaData,
  ISchemaMetaData,
  ISetting,
} from "meta-data-utils";
import { deepClone } from "../components/utils";
import { IClient, INewClient } from "./IClient";
import { IQueryMetaData } from "./IQueryMetaData";
import { getColumnIds } from "./queryBuilder";
import type { aggFunction } from "./IClient";

// application wide cache for schema meta data
const schemaCache = new Map<string, ISchemaMetaData>();

export { request };
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
          (table: ITableMetaData) =>
            table.id === tableId || table.label === tableId
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
        expandLevel: number = 1
      ) => {
        const schemaMetaData = await fetchSchemaMetaData(schemaId);
        const tableMetaData = schemaMetaData.tables.find(
          (table: ITableMetaData) =>
            table.id === tableId && table.schemaId === schemaMetaData.id
        );
        const filter = tableMetaData?.columns
          ?.filter((column: IColumn) => column.key === 1)
          .reduce((accum: any, column: IColumn) => {
            accum[column.id] = { equals: rowId[column.id] };
            return accum;
          }, {});
        const resultArray = (
          await fetchTableData(
            tableId,
            {
              filter,
            },
            schemaMetaData,
            expandLevel
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
        aggFunction?: aggFunction,
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
      saveSetting: async (key: string, value: any) => {
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
      fetchSubclassTables: async (tableId: string) => {
        return fetchSubclassTables(tableId, schemaId);
      },
    };
  },
};
export default client;

const metaDataQuery = `{
  _schema {
    id,
    tables {
      schemaId,
      id,
      label, 
      description,
      tableType,
      schemaId,
      semantics,
      inheritId,
      columns {
        id,
        name,
        label,
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
  const query = `mutation {truncate(tables:"${tableId}"){message}}`;
  return axios.post(graphqlURL(schemaId), { query });
};

const fetchSchemaMetaData = async (
  schemaId?: string
): Promise<ISchemaMetaData> => {
  const currentschemaId = schemaId ? schemaId : "CACHE_OF_CURRENT_SCHEMA";
  if (schemaCache.has(currentschemaId)) {
    return schemaCache.get(currentschemaId) as ISchemaMetaData;
  }
  return await axios
    .post(graphqlURL(schemaId), { query: metaDataQuery })
    .then((result: AxiosResponse<{ data: { _schema: ISchemaMetaData } }>) => {
      const schema = result.data.data._schema;
      if (schemaId == null) {
        schemaCache.set(currentschemaId, schema);
      }
      schemaCache.set(schema.id, schema);
      return deepClone(schema);
    })
    .catch((error: AxiosError) => {
      console.log(error);
      throw error;
    });
};

const fetchTableData = async (
  tableId: string,
  properties: IQueryMetaData,
  metaData: ISchemaMetaData,
  expandLevel: number = 2
) => {
  const limit = properties.limit ? properties.limit : 20;
  const offset = properties.offset ? properties.offset : 0;

  const search = properties.searchTerms
    ? ',search:"' + properties.searchTerms.trim() + '"'
    : "";

  const schemaId = metaData.id;
  const columnIds = getColumnIds(schemaId, tableId, metaData, expandLevel);
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

const fetchSubclassTables = async (
  tableId: string,
  schemaId: string | undefined
): Promise<ITableMetaData[]> => {
  const schema = await fetchSchemaMetaData(schemaId);
  const result: ITableMetaData[] = [];
  for (const table of schema.tables) {
    if (table.inheritId === tableId) {
      result.push(table);
      result.push(...(await fetchSubclassTables(table.id, schemaId)));
    }
  }
  return result;
};

const request = async (url: string, graphql: string, variables?: any) => {
  const data: { query: string; variables?: any } = { query: graphql };
  if (variables) {
    data.variables = variables;
  }
  return axios
    .post(url, data)
    .then((result: AxiosResponse) => {
      return result?.data?.data;
    })
    .catch((error: AxiosError<any>): string => {
      const detailedErrorMessage = error.response?.data?.errors
        ?.map((error: { message: string }) => {
          return error.message;
        })
        .join(". ");
      throw detailedErrorMessage || error.message;
    });
};

const isFileValue = (value: File) => {
  if (window && "File" in window) {
    return value instanceof File;
  } else {
    throw "Files can only be uploaded via a browser client";
  }
};

const toFormData = (rowData: IRow) => {
  if (!FormData) {
    throw "Files can only be uploaded via a browser client";
  }
  const formData = new FormData();
  let nonFileValue: { [key: string]: string } = {};
  let fileValues: { [key: string]: string } = {};

  // split into file and non-file entries
  for (const [key, value] of Object.entries(rowData)) {
    isFileValue(value)
      ? (fileValues[key] = value)
      : (nonFileValue[key] = value);
  }

  // add the file objects to the formData and place a link to the object in the variables
  for (const [key, value] of Object.entries(fileValues)) {
    const id = Math.random().toString(36);
    formData.append(id, value);
    nonFileValue[key] = id;
  }

  formData.append("variables", JSON.stringify({ value: [nonFileValue] }));

  return formData;
};

async function convertRowToPrimaryKey(
  row: IRow,
  tableId: string,
  schemaId?: string
): Promise<Record<string, any>> {
  const schema = await fetchSchemaMetaData(schemaId);
  const tableMetadata = schema.tables.find(
    (table: ITableMetaData) =>
      table.id === tableId && table.schemaId === schema.id
  );
  if (!tableMetadata?.columns) {
    throw new Error("Empty columns in metadata");
  } else {
    return await tableMetadata.columns.reduce(
      async (accumPromise: Promise<IRow>, column: IColumn): Promise<IRow> => {
        let accum: IRow = await accumPromise;
        const cellValue = row[column.id];
        if (column.key === 1 && cellValue) {
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

  async function getKeyValue(
    cellValue: any,
    column: IColumn,
    schemaId: string
  ) {
    if (typeof cellValue === "string") {
      return cellValue;
    } else {
      if (column.refTableId) {
        return await convertRowToPrimaryKey(
          cellValue,
          column.refTableId,
          schemaId
        );
      }
    }
  }
}
