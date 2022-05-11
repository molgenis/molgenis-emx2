import axios from "axios";

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
      refLink,
      refLabel,
      refBack,
      required,
      semantics,
      description,
      position
    }
  }
}}`;

/**
 *
 * @param {String} tableName
 * @param {Object} metaData - object that contains all schema meta data
 * @returns String of fields for use in gql query
 */
const columnNames = (tableName, metaData) => {
  let result = "";
  getTable(tableName, metaData.tables).columns.forEach((col) => {
    if (
      ["REF", "REF_ARRAY", "REFBACK", "ONTOLOGY", "ONTOLOGY_ARRAY"].includes(
        col.columnType
      ) > 0
    ) {
      result = result + " " + col.id + "{" + refGraphql(col, metaData) + "}";
    } else if (col.columnType === "FILE") {
      result = result + " " + col.id + "{id,size,extension,url}";
    } else if (col.columnType !== "HEADING") {
      result = result + " " + col.id;
    }
  });

  return result;
};

const refGraphql = (column, metaData) => {
  let graphqlString = "";
  const refTable = getTable(column.refTable, metaData.tables);
  refTable.columns.forEach((c) => {
    if (c.key == 1) {
      graphqlString += c.id + " ";
      if (
        ["REF", "REF_ARRAY", "REFBACK", "ONTOLOGY", "ONTOLOGY_ARRAY"].includes(
          c.columnType
        ) > 0
      ) {
        graphqlString += "{" + refGraphql(c, metaData) + "}";
      }
    }
  });
  return graphqlString;
};

const getTable = (tableName, tableStore) => {
  return tableStore.find((table) => table.name === tableName);
};

const fetchMetaData = async (axios, graphqlURL, onError) => {
  const url = graphqlURL ? graphqlURL : "graphql";
  const resp = await axios
    .post(url, { query: metaDataQuery })
    .catch((error) => {
      console.log(error);
      if (typeof onError === "function") {
        onError(error);
      }
    });
  return resp.data.data._schema;
};

const fetchTableData = async (
  tableId,
  properties,
  metaData,
  axios,
  graphqlURL,
  onError
) => {
  const url = graphqlURL ? graphqlURL : "graphql";
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
    properties.searchTerms !== ""
      ? ',search:"' + properties.searchTerms.trim() + '"'
      : "";

  const cNames = columnNames(tableId, metaData);
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
    .post(url, { query: tableDataQuery, variables: { filter, orderby } })
    .catch((error) => {
      console.log(error);
      if (typeof onError === "function") {
        onError(error);
      }
    });
  return resp.data.data;
};

const request = async (url, graqphl) => {
  const result = await axios.post(url, { query: graqphl }).catch((error) => {
    return error;
  });
  return result.data.data;
};

export { request };

export default {
  newClient: (graphqlURL, externalAxios) => {
    const myAxios = externalAxios || axios;
    // use closure to have metaData cache private to client
    let metaData = null;
    return {
      fetchMetaData: () => {
        return fetchMetaData(myAxios, graphqlURL).then((schema) => {
          metaData = schema;
          // node js may not have structuredClone function, then fallback to deep clone via JSON
          return typeof structuredClone === "function"
            ? structuredClone(metaData)
            : JSON.parse(JSON.stringify(metaData));
        });
      },
      fetchTableData: async (tableId, properties) => {
        if (metaData === null) {
          metaData = await fetchMetaData(myAxios, graphqlURL);
        }
        return fetchTableData(
          tableId,
          properties,
          metaData,
          myAxios,
          graphqlURL
        );
      },
    };
  },
};
