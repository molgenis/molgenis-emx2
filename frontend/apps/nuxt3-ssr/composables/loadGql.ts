import { DocumentNode } from "graphql";

export const loadGql = (gql: DocumentNode) => {
  if (gql.loc?.source.body === undefined) {
    throw "unable to load query: " + gql.toString();
  }
  return gql.loc?.source.body;
};
