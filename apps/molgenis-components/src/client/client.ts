import axios, { AxiosError, AxiosResponse } from "axios";
import { IColumn } from "../Interfaces/IColumn";
import { ISchemaMetaData } from "../Interfaces/IMetaData";
import { IRow } from "../Interfaces/IRow";
import { ISetting } from "../Interfaces/ISetting";
import { ITableMetaData } from "../Interfaces/ITableMetaData";
import { convertToPascalCase, deepClone } from "../components/utils";
import { IClient, INewClient } from "./IClient";
import { IQueryMetaData } from "./IQueryMetaData";
import { columnNames } from "./queryBuilder";

// application wide cache for schema meta data
const schemaCache = new Map<string, ISchemaMetaData>();

export { request };
const client: IClient = {
  newClient: (schemaName?: string): INewClient => {
    return {
      insertDataRow,
      updateDataRow,
      deleteRow: async (rowKey: IRow, tableName: string) => {
        return deleteRow(rowKey, tableName, schemaName);
      },
      deleteAllTableData: async (tableName: string) => {
        return deleteAllTableData(tableName, schemaName);
      },
      fetchSchemaMetaData: async () => {
        return fetchSchemaMetaData(schemaName);
      },
      fetchTableMetaData: async (
        tableName: string
      ): Promise<ITableMetaData> => {
        const schema = await fetchSchemaMetaData(schemaName);
        return deepClone(schema).tables.find(
          (table: ITableMetaData) => table.id === convertToPascalCase(tableName)
        );
      },
      fetchTableData: async (
        tableId: string,
        properties: IQueryMetaData = {}
      ) => {
        const schemaMetaData = await fetchSchemaMetaData(schemaName);
        return fetchTableData(tableId, properties, schemaMetaData);
      },
      fetchTableDataValues: async (
        tableName: string,
        properties: IQueryMetaData = {}
      ) => {
        const schemaMetaData = await fetchSchemaMetaData(schemaName);
        const tableId = convertToPascalCase(tableName);
        const dataResp = await fetchTableData(
          tableId,
          properties,
          schemaMetaData
        );
        return dataResp[tableId];
      },
      fetchRowData: async (
        tableName: string,
        rowId: IRow,
        expandLevel: number = 1
      ) => {
        const tableId = convertToPascalCase(tableName);
        const schemaMetaData = await fetchSchemaMetaData(schemaName);
        const tableMetaData = schemaMetaData.tables.find(
          (table) =>
            table.id === tableId && table.externalSchema === schemaMetaData.name
        );
        const filter = tableMetaData?.columns
          ?.filter((column) => column.key === 1)
          .reduce((accum: any, column) => {
            accum[column.id] = { equals: rowId[column.id] };
            return accum;
          }, {});
        const resultArray = (
          await fetchTableData(
            tableName,
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
        tableName: string,
        selectedColumn: { name: string; column: string },
        selectedRow: { name: string; column: string },
        filter: Object
      ) => {
        const aggregateQuery = `
        query ${tableName}_groupBy($filter: ${tableName}Filter){
          ${tableName}_groupBy(filter: $filter) {
            count,
            ${selectedColumn.name} {
              ${selectedColumn.column}
            },
            ${selectedRow.name} {
              ${selectedRow.column}
            }
          }
        }`;
        return request(graphqlURL(schemaName), aggregateQuery, { filter });
      },
      saveTableSettings: async (settings: ISetting[]) => {
        return request(
          graphqlURL(schemaName),
          `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){message}}`,
          { settings }
        );
      },
      fetchSettings: async () => {
        return fetchSettings(schemaName);
      },
      fetchSettingValue: async (name: string) => {
        const settings = await fetchSettings(schemaName);
        const setting = settings.find(
          (setting: ISetting) => setting.key == name
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

        await request(graphqlURL(schemaName), createMutation, variables).catch(
          (e) => {
            console.error(e);
          }
        );
      },
      clearCache: () => {
        schemaCache.clear();
      },
      convertRowToPrimaryKey: async (row: IRow, tableName: string) => {
        return convertRowToPrimaryKey(row, tableName, schemaName);
      },
    };
  },
};
export default client;

const metaDataQuery = `{
_schema {
  name,
  tables {
    name,
    labels {
      locale,
      value
    },
    tableType,
    id,
    descriptions {
      locale,
      value
    },
    externalSchema,
    semantics,
    columns {
      name,
      labels {
        locale,
        value
      },
      id,
      columnType,
      key,
      refTable,
      refSchema,
      refLink,
      refLabel,
      refLabelDefault,
      refBack,
      required,
      readonly,
      semantics,
      descriptions{
        locale,
        value
      },
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
}}`;

const graphqlURL = (schemaName?: string) => {
  return schemaName ? "/" + schemaName + "/graphql" : "graphql";
};

const insertDataRow = (
  rowData: IRow,
  tableName: string,
  schemaName: string
) => {
  const tableId = convertToPascalCase(tableName);
  const formData = toFormData(rowData);
  const query = `mutation insert($value:[${tableId}Input]){insert(${tableId}:$value){message}}`;
  formData.append("query", query);
  return axios.post(graphqlURL(schemaName), formData);
};

const updateDataRow = (
  rowData: IRow,
  tableName: string,
  schemaName: string
) => {
  const tableId = convertToPascalCase(tableName);
  const formData = toFormData(rowData);
  const query = `mutation update($value:[${tableId}Input]){update(${tableId}:$value){message}}`;
  formData.append("query", query);
  return axios.post(graphqlURL(schemaName), formData);
};

const deleteRow = async (row: IRow, tableName: string, schemaName?: string) => {
  const tableId = convertToPascalCase(tableName);
  const query = `mutation delete($pkey:[${tableId}Input]){delete(${tableId}:$pkey){message}}`;
  const key = await convertRowToPrimaryKey(row, tableName, schemaName);
  const variables = { pkey: [key] };
  return axios.post(graphqlURL(schemaName), { query, variables });
};

const deleteAllTableData = (tableName: string, schemaName?: string) => {
  const query = `mutation {truncate(tables:"${tableName}"){message}}`;
  return axios.post(graphqlURL(schemaName), { query });
};

const fetchSchemaMetaData = async (
  schemaName?: string
): Promise<ISchemaMetaData> => {
  const currentSchemaName = schemaName ? schemaName : "CACHE_OF_CURRENT_SCHEMA";
  if (schemaCache.has(currentSchemaName)) {
    return schemaCache.get(currentSchemaName) as ISchemaMetaData;
  }
  return await axios
    .post(graphqlURL(schemaName), { query: metaDataQuery })
    .then((result: AxiosResponse<{ data: { _schema: ISchemaMetaData } }>) => {
      const schema = result.data.data._schema;
      if (schemaName == null) {
        schemaCache.set(currentSchemaName, schema);
      }
      schemaCache.set(schema.name, schema);
      return deepClone(schema);
    })
    .catch((error: AxiosError) => {
      console.log(error);
      throw error;
    });
};

const fetchTableData = async (
  tableName: string,
  properties: IQueryMetaData,
  metaData: ISchemaMetaData,
  expandLevel: number = 2
) => {
  const tableId = convertToPascalCase(tableName);
  const limit = properties.limit ? properties.limit : 20;
  const offset = properties.offset ? properties.offset : 0;

  const search = properties.searchTerms
    ? ',search:"' + properties.searchTerms.trim() + '"'
    : "";

  const schemaName = metaData.name;
  const cNames = columnNames(schemaName, tableId, metaData, expandLevel);
  const tableDataQuery = `query ${tableId}( $filter:${tableId}Filter, $orderby:${tableId}orderby ) {
        ${tableId}(
          filter:$filter,
          limit:${limit}, 
          offset:${offset}${search},
          orderby:$orderby
          )
          {
            ${cNames}
          }
          ${tableId}_agg( filter:$filter${search} ) {
            count
          }
        }`;

  const filter = properties.filter ? properties.filter : {};
  const orderby = properties.orderby ? properties.orderby : {};
  const resp = await axios
    .post(graphqlURL(schemaName), {
      query: tableDataQuery,
      variables: { filter, orderby },
    })
    .catch((error: AxiosError) => {
      console.log(error);
      throw error;
    });
  return resp?.data.data;
};

const fetchSettings = async (schemaName?: string) => {
  return (await request(graphqlURL(schemaName), "{_settings{key, value}}"))
    ._settings;
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
  tableName: string,
  schemaName?: string
): Promise<Record<string, any>> {
  const schema = await fetchSchemaMetaData(schemaName);
  const tableMetadata = schema.tables.find(
    (table: ITableMetaData) =>
      table.id === convertToPascalCase(tableName) &&
      table.externalSchema === schema.name
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
            column.refSchema || schema.name
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
    schemaName: string
  ) {
    if (typeof cellValue === "string") {
      return cellValue;
    } else {
      if (column.refTable) {
        return await convertRowToPrimaryKey(
          cellValue,
          column.refTable,
          schemaName
        );
      }
    }
  }
}
