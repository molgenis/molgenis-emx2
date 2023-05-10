import axios, { Axios, AxiosError, AxiosResponse } from "axios";
import { ISchemaMetaData } from "../Interfaces/IMetaData";
import { IRow } from "../Interfaces/IRow";
import { ISetting } from "../Interfaces/ISetting";
import { ITableMetaData } from "../Interfaces/ITableMetaData";
import {
  convertToCamelCase,
  convertToPascalCase,
  deepClone,
} from "../components/utils";
import { IClient, INewClient } from "./IClient";
import { IQueryMetaData } from "./IQueryMetaData";
import { columnNames } from "./queryBuilder";

export { request };
const client: IClient = {
  newClient: (schemaName?: string, externalAxios?: Axios): INewClient => {
    const myAxios = externalAxios || axios;
    // use closure to have metaData cache private to client
    let schemaMetaData: ISchemaMetaData | null | void = null;
    let schemaNameCache: string = schemaName || "";

    return {
      insertDataRow,
      updateDataRow,
      deleteRow: async (rowKey: IRow, tableName: string) => {
        return deleteRow(rowKey, tableName, schemaNameCache);
      },
      deleteAllTableData: async (tableName: string) => {
        return deleteAllTableData(tableName, schemaNameCache);
      },
      fetchSchemaMetaData: async () => {
        schemaMetaData = await fetchSchemaMetaData(myAxios, schemaNameCache);
        if (!schemaNameCache) {
          schemaNameCache = schemaMetaData.name;
        }
        return deepClone(schemaMetaData);
      },
      fetchTableMetaData: async (
        tableName: string
      ): Promise<ITableMetaData> => {
        if (schemaMetaData === null) {
          schemaMetaData = await fetchSchemaMetaData(myAxios, schemaNameCache);
          if (schemaMetaData && !schemaNameCache) {
            schemaNameCache = schemaMetaData.name;
          }
        }
        return deepClone(schemaMetaData).tables.find(
          (table: ITableMetaData) =>
            table.id === convertToPascalCase(tableName) &&
            table.externalSchema === schemaNameCache
        );
      },
      fetchTableData: async (tableId: string, properties: IQueryMetaData) => {
        if (schemaMetaData === null) {
          schemaMetaData = await fetchSchemaMetaData(myAxios, schemaNameCache);
          if (schemaMetaData && !schemaNameCache) {
            schemaNameCache = schemaMetaData.name;
          }
        }
        if (!schemaMetaData) {
          throw "Schema meta data not found for schema: " + schemaNameCache;
        }
        return fetchTableData(
          tableId,
          properties,
          schemaMetaData,
          myAxios,
          schemaNameCache
        );
      },
      fetchTableDataValues: async (
        tableName: string,
        properties: IQueryMetaData
      ) => {
        const tableId = convertToPascalCase(tableName);
        if (schemaMetaData === null) {
          schemaMetaData = await fetchSchemaMetaData(myAxios, schemaNameCache);
          if (!schemaNameCache) {
            schemaNameCache = schemaMetaData.name;
          }
        }
        if (!schemaMetaData) {
          throw "Schema meta data not found for schema: " + schemaNameCache;
        }
        const dataResp = await fetchTableData(
          tableId,
          properties,
          schemaMetaData,
          myAxios,
          schemaNameCache
        );
        return dataResp[tableId];
      },
      fetchRowData: async (tableName: string, rowId: IRow) => {
        const tableId = convertToPascalCase(tableName);
        if (schemaMetaData === null) {
          schemaMetaData = await fetchSchemaMetaData(myAxios, schemaNameCache);
          if (!schemaNameCache) {
            schemaNameCache = schemaMetaData.name;
          }
        }
        if (!schemaMetaData) {
          throw "Schema meta data not found for schema: " + schemaNameCache;
        }
        const tableMetaData = schemaMetaData.tables.find(
          (table) =>
            table.id === tableId && table.externalSchema === schemaNameCache
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
            myAxios,
            schemaNameCache
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
        return request(graphqlURL(schemaNameCache), aggregateQuery, { filter });
      },
      saveTableSettings: async (settings: ISetting[]) => {
        return request(
          graphqlURL(schemaNameCache),
          `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){message}}`,
          { settings }
        );
      },
      fetchSettings: async () => {
        return fetchSettings(schemaNameCache);
      },
      fetchSettingValue: async (name: string) => {
        const settings = await fetchSettings(name);
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

        await request(
          graphqlURL(schemaNameCache),
          createMutation,
          variables
        ).catch((e) => {
          console.error(e);
        });
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

const graphqlURL = (schemaName: string) => {
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

const deleteRow = (key: IRow, tableName: string, schemaName: string) => {
  const tableId = convertToPascalCase(tableName);
  const query = `mutation delete($pkey:[${tableId}Input]){delete(${tableId}:$pkey){message}}`;
  const variables = { pkey: [key] };
  return axios.post(graphqlURL(schemaName), { query, variables });
};

const deleteAllTableData = (tableName: string, schemaName: string) => {
  const query = `mutation {truncate(tables:"${tableName}"){message}}`;
  return axios.post(graphqlURL(schemaName), { query });
};

const fetchSchemaMetaData = async (
  axios: Axios,
  schemaName: string
): Promise<ISchemaMetaData> => {
  return await axios
    .post(graphqlURL(schemaName), { query: metaDataQuery })
    .then((result: AxiosResponse<{ data: { _schema: ISchemaMetaData } }>) => {
      return result.data.data._schema;
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
  axios: Axios,
  schemaName: string,
  expandLevel: number = 1
) => {
  const tableId = convertToPascalCase(tableName);
  const limit = properties.limit ? properties.limit : 20;
  const offset = properties.offset ? properties.offset : 0;

  const search = properties.searchTerms
    ? ',search:"' + properties.searchTerms.trim() + '"'
    : "";

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

const fetchSettings = async (schemaName: string) => {
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
