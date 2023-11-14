import { type DocumentNode } from "graphql";

const ONTOLOGIES_SCHEMA_NAME = "CatalogueOntologies";

export const fetchOntology = (
  query: string | DocumentNode,
  variables?: object
) => {
  return fetchGql(query, variables, ONTOLOGIES_SCHEMA_NAME);
};
