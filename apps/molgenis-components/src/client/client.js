import axios from "axios";
import RecursiveIterator from "recursive-iterator";
import objectPath from "object-path";

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
      insertVariables(graphqlURL, tableName, isDraft, values) {
        return executeSaveCommand(
          graphqlURL,
          tableName,
          isDraft,
          values,
          "insert"
        );
      },
      updateVariables(graphqlURL, tableName, isDraft, values) {
        return executeSaveCommand(
          graphqlURL,
          tableName,
          isDraft,
          values,
          "update"
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
  const limit = properties?.limit ? properties.limit : 20;
  const offset = properties?.offset ? properties.offset : 0;

  const search = properties?.searchTerms
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

  const filter = properties?.filter ? properties.filter : {};
  const orderby = properties?.orderby ? properties.orderby : {};
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

const request = async (url, graphql) => {
  const result = await axios.post(url, { query: graphql }).catch((error) => {
    return error;
  });
  return result.data.data;
};

function executeSaveCommand(graphqlURL, tableName, isDraft, values, action) {
  values.mg_draft = isDraft;
  const variables = { value: [values] };
  const query = `mutation ${action}($value:[${tableName}Input]){${action}(${tableName}:$value){message}}`;
  const formData = getFormData(variables, query);
  return requestMultipart(graphqlURL, query, formData);
}

function getFormData(variables, query) {
  let formData = createFormDataWithFiles(variables);
  formData.append("query", query);
  formData.append("variables", JSON.stringify(variables || {}));
  return formData;
}

function createFormDataWithFiles(variables) {
  let formData = new FormData();
  for (let { node, path } of new RecursiveIterator(variables)) {
    if (node instanceof File) {
      const id = Math.random().toString(36);
      formData.append(id, node);
      objectPath.set(variables, path.join("."), id);
    }
  }
  return formData;
}

function requestMultipart(url, query, formData) {
  return new Promise((resolve, reject) => {
    //thanks to https://medium.com/@danielbuechele/file-uploads-with-graphql-and-apollo-5502bbf3941e
    fetch(url, {
      body: formData,
      method: "POST",
    })
      .then((response) => {
        multipartSuccessHandler(response, resolve, reject);
      })
      .catch((error) => {
        alert("catch: " + error);
        reject({ status: error, query: query });
      });
  });
}

function multipartSuccessHandler(response, resolve, reject) {
  response.json().then((result) => {
    if (response.ok && !result.errors && result.data) {
      resolve({
        data: result.data,
      });
    } else {
      reject({
        errors: result.errors,
      });
    }
  });
}
