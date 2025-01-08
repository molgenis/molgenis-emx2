// import gql from "graphql-tag";
import { request, gql } from "graphql-request";
import type {
  ISettings,
  ISettingsResponse,
  ICranioSchemas,
} from "../types/index";

/**
@name getCranioSchemas
@description Retrieve the names of the other schemas that are used to populate the dashboards and landing pages. All schemas in the cranio provider dashboard must have the following settings: CRANIO_PUBLIC_SCHEMA and CRANIO_PROVIDER_SCHEMA.

@returns An object containing where each key is a schema setting and the value is the name of the schema that was used to create the database
*/

export async function getCranioSchemaNames(): Promise<ICranioSchemas> {
  const targetSchemas: string[] = [
    "CRANIO_PUBLIC_SCHEMA",
    "CRANIO_PROVIDER_SCHEMA",
  ];

  const query = gql`
    {
      _settings {
        key
        value
      }
    }
  `;
  const response: ISettingsResponse = await request("../api/graphql", query);
  const results: ISettings[] = response._settings.filter((row: ISettings) => {
    return targetSchemas.includes(row.key);
  });

  const schemaNames = Object.fromEntries(
    results.map((result: ISettings) => {
      return Object.values(result);
    })
  );

  if (Object.keys(schemaNames).length === 2) {
    return schemaNames;
  } else {
    const message: string =
      "Missing the names of the schemas that control the Cranio Provider dashboard in the schema settings. Add these settings as keys: 'CRANIO_PUBLIC_SCHEMA' and 'CRANIO_PROVIDER_SCHEMA'. ";
    throw new Error(message);
  }
}
