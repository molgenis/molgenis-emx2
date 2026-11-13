import { getContainersQuery } from "../gql/cmsPages";

import type { IConfigurablePages, IDeveloperPages } from "../../types/cms";
import type {
  IContainerMetadata,
  FetchGraphqlResponse,
} from "../../types/CmsComponents";

export function randomId(): string {
  return crypto.randomUUID();
}

export async function getPage(
  schema: string,
  page: string
): Promise<IContainerMetadata> {
  const { data } = await $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: getContainersQuery,
      variables: { filter: { name: { equals: page } } },
    },
  });

  const currentPage = data.Containers[0] as
    | IConfigurablePages
    | IDeveloperPages;
  return { page: currentPage, metadata: data._schema.tables };
}

export function setCmsPageType(value?: string): string | undefined {
  if (!value || typeof value === "undefined") {
    return undefined;
  } else if (value.endsWith(".Configurable pages")) {
    return "Landing page";
  } else if (value.endsWith(".Developer pages")) {
    return "Dev page";
  } else {
    return undefined;
  }
}

export function setCmsEditorUrl(
  schema: string,
  value: string,
  page: string
): string {
  if (value.endsWith(".Developer pages")) {
    return `/${schema}/pages/${page}/editor`;
  } else {
    return `/${schema}/pages/${page}/configure`;
  }
}

export function setCmsViewUrl(schema: string, page: string): string {
  return `/${schema}/pages/${page}/`;
}

export async function cmsFetch(
  schema: string,
  query: string,
  variables?: any
): Promise<FetchGraphqlResponse> {
  const url: string = `/${schema}/graphql`;
  const response = (await $fetch(url, {
    method: "POST",
    body: { query: query, variables: variables },
  })) as unknown as FetchGraphqlResponse;

  if (response?.errors?.[0]?.message) {
    console.error(response.errors[0].message);
  }

  return response;
}

export function parsePageText(value?: string): string {
  const val = value || "";
  return val.replace(/(^"{1,})|("{1,}$)/g, "");
}

export function pageCopyDate(): string {
  const date = new Date().toISOString();
  return date.replace("T", " ").split(".")[0] as string;
}

export async function updateCMSfield(
  schema: string,
  typeName: string,
  id: string,
  text: string,
  fieldName: string = "text"
) {
  const updateQuery = `mutation update($value:[${typeName}Input]) {
      update(${typeName}:$value) {
        message
      }
    }`;

  const updateVars = { value: [{ id, [fieldName]: text }] };
  await cmsFetch(schema, updateQuery, updateVars);
}
