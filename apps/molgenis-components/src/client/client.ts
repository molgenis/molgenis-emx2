import axios, { Axios, AxiosError, AxiosResponse } from "axios";
import { deepClone, convertToPascalCase } from "../components/utils";
import { IRow } from "../Interfaces/IRow";
import { IMetaData } from "../Interfaces/IMetaData";
import { IColumn } from "../Interfaces/IColumn";
import { ITableMetaData } from "../Interfaces/ITableMetaData";
import { ISetting } from "../components/account/Interfaces";
import { IClientProperties } from "./IClientProperties";

export { request };

export default {
  newClient: (schemaName: string, externalAxios: Axios) => {
    const myAxios = externalAxios || axios;
    // use closure to have metaData cache private to client
    let metaData: IMetaData | null = null;

    return {
      insertDataRow,
      updateDataRow,
      deleteRow: async (rowKey: IRow, tableName: string) => {
        return deleteRow(rowKey, tableName, schemaName);
      },
      deleteAllTableData: async (tableName: string) => {
        return deleteAllTableData(tableName, schemaName);
      },
      fetchMetaData: async () => {
        const schema: IMetaData = await fetchMetaData(myAxios, schemaName);
        metaData = schema;
        return deepClone(metaData);
      },
      fetchTableMetaData: async (tableName: string) => {
        if (metaData === null) {
          metaData = await fetchMetaData(myAxios, schemaName);
          if (!schemaName) {
            schemaName = metaData.name;
          }
        }
        return deepClone(metaData).tables.find(
          (table: ITableMetaData) =>
            table.id === convertToPascalCase(tableName) &&
            table.externalSchema === schemaName
        );
      },
      fetchTableData: async (
        tableId: string,
        properties: IClientProperties
      ) => {
        if (metaData === null) {
          metaData = await fetchMetaData(myAxios, schemaName);
          if (!schemaName) {
            schemaName = metaData.name;
          }
        }
        return fetchTableData(
          tableId,
          properties,
          metaData,
          myAxios,
          schemaName
        );
      },
      fetchTableDataValues: async (
        tableName: string,
        properties: IClientProperties
      ) => {
        const tableId = convertToPascalCase(tableName) || tableName;
        if (metaData === null) {
          metaData = await fetchMetaData(myAxios, schemaName);
          if (!schemaName) {
            schemaName = metaData.name;
          }
        }
        const dataResp = await fetchTableData(
          tableId,
          properties,
          metaData,
          myAxios,
          schemaName
        );
        return dataResp[tableId];
      },
      fetchRowData: async (tableName: string, rowId: IRow) => {
        const tableId = convertToPascalCase(tableName) || tableName;
        if (metaData === null) {
          metaData = await fetchMetaData(myAxios, schemaName);
          if (!schemaName) {
            schemaName = metaData.name;
          }
        }
        const tableMetaData = metaData.tables.find(
          (table: ITableMetaData) =>
            table.id === tableId && table.externalSchema === schemaName
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
            metaData,
            myAxios,
            schemaName
          )
        )[tableId];

        if (!resultArray.length || resultArray.length !== 1) {
          return undefined;
        } else {
          return resultArray[0];
        }
      },
      saveTableSettings: async (settings: any) => {
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
        const settings = await fetchSettings(name);
        const setting = settings.find(
          (setting: ISetting) => setting.key == name
        );
        if (setting) {
          return JSON.parse(setting.value);
        }
      },
      async saveSetting(key: string, value: any) {
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
    };
  },
};

const metaDataQuery = `{
_schema {
  name,
  tables {
    name,
    tableType,
    id,
    description,
    externalSchema,
    semantics,
    columns {
      name,
      id,
      columnType,
      key,
      refTable,
      refSchema,
      refLink,
      refLabel,
      refBack,
      required,
      readonly,
      semantics,
      description,
      position,
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
  const tableId = convertToPascalCase(tableName);
  const query = `mutation {truncate(tables:"${tableId}"){message}}`;
  return axios.post(graphqlURL(schemaName), { query });
};

const fetchMetaData = async (
  axios: Axios,
  schemaName: string,
  onError?: (error: AxiosError) => void
) => {
  const resp = await axios
    .post(graphqlURL(schemaName), { query: metaDataQuery })
    .catch((error) => {
      console.log(error);
      if (typeof onError === "function") {
        onError(error);
      }
    });
  return resp?.data.data._schema as IMetaData;
};

const fetchTableData = async (
  tableName: string,
  properties: IClientProperties,
  metaData: IMetaData,
  axios: Axios,
  schemaName: string,
  onError?: (error: AxiosError) => void
) => {
  const tableId = convertToPascalCase(tableName) || tableName;
  const limit =
    properties && Object.prototype.hasOwnProperty.call(properties, "limit")
      ? properties.limit
      : 20;
  const offset =
    properties && Object.prototype.hasOwnProperty.call(properties, "offset")
      ? properties.offset
      : 0;

  const search =
    properties &&
    Object.prototype.hasOwnProperty.call(properties, "searchTerms") &&
    properties.searchTerms !== null &&
    properties.searchTerms !== ""
      ? ',search:"' + properties.searchTerms?.trim() + '"'
      : "";

  const cNames = columnNames(schemaName, tableId, metaData);
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

  const filter =
    properties && Object.prototype.hasOwnProperty.call(properties, "filter")
      ? properties.filter
      : {};
  const orderby =
    properties && Object.prototype.hasOwnProperty.call(properties, "orderby")
      ? properties.orderby
      : {};
  const resp = await axios
    .post(graphqlURL(schemaName), {
      query: tableDataQuery,
      variables: { filter, orderby },
    })
    .catch((error: AxiosError) => {
      console.log(error);
      if (typeof onError === "function") {
        onError(error);
      } else {
        throw error;
      }
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
    .catch((error: AxiosError) => {
      throw error;
    });
};

/**
 *
 * @param {String} tableName
 * @param {Object} metaData - object that contains all schema meta data
 * @returns String of fields for use in gql query
 */
const columnNames = (
  schemaName: string,
  tableName: string,
  metaData: IMetaData
) => {
  let result = "";
  getTable(schemaName, tableName, metaData.tables)?.columns?.forEach((col) => {
    if (
      ["REF", "REF_ARRAY", "REFBACK", "ONTOLOGY", "ONTOLOGY_ARRAY"].includes(
        col.columnType
      )
    ) {
      result =
        result +
        " " +
        col.id +
        "{" +
        refGraphql(schemaName, col, metaData) +
        "}";
    } else if (col.columnType === "FILE") {
      result = result + " " + col.id + "{id,size,extension,url}";
    } else if (col.columnType !== "HEADING") {
      result = result + " " + col.id;
    }
  });

  return result;
};

const refGraphql = (
  schemaName: string,
  column: IColumn,
  metaData: IMetaData
) => {
  let graphqlString = "";
  schemaName = column.refSchema ? column.refSchema : schemaName;
  const refTable = getTable(schemaName, column.refTable, metaData.tables);
  refTable?.columns?.forEach((c) => {
    if (c.key == 1) {
      graphqlString += c.id + " ";
      if (
        ["REF", "REF_ARRAY", "REFBACK", "ONTOLOGY", "ONTOLOGY_ARRAY"].includes(
          c.columnType
        )
      ) {
        graphqlString += "{" + refGraphql(schemaName, c, metaData) + "}";
      }
    }
  });
  return graphqlString;
};

const getTable = (
  schemaName: string,
  tableName: string,
  tableStore: ITableMetaData[]
) => {
  const result = tableStore.find(
    (table) =>
      table.id === convertToPascalCase(tableName) &&
      table.externalSchema === schemaName
  );
  return result;
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
