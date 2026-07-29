import { ISetting } from "../../../metadata-utils/src/types";
import type { ICranioSchemas } from "../types";

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

  const query: string = `{ _settings { key value }}`;
  const response = await fetch("../api/graphql", {
    method: "POST",
    body: JSON.stringify({ query: query }),
  });

  const responseJson = await response.json();
  const results: ISetting[] = responseJson.data._settings.filter(
    (row: ISetting) => {
      return targetSchemas.includes(row.key);
    }
  );

  const schemaNames = Object.fromEntries(
    results.map((result: ISetting) => {
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
