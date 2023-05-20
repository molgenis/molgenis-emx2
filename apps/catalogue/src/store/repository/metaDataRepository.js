import { request } from "graphql-request";
import schemaMetaQuery from "../query/schemaMetaData.js";

const fetchSchemaMetaData = async () => {
  return request("graphql", schemaMetaQuery).catch((e) => {
    console.error(`schemaMetaQuery not found`);
    console.error(e);
  });
};

export { fetchSchemaMetaData };
