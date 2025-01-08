// import gql from "graphql-tag";
import { request, gql } from "graphql-request";
import type { ISchemaResponse, ISchema } from "../types/index";

export async function getSchemaName(): Promise<string> {
  const query = gql`
    {
      _schema {
        name
      }
    }
  `;

  const result: ISchemaResponse = await request("../api/graphql", query);
  return (result._schema as ISchema).name;
}
