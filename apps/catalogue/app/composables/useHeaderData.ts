import { useRoute, useRuntimeConfig } from "#app";
import { logError } from "#imports";
import type { UIResource } from "../../interfaces/types";

interface Resp<T> {
  data: T;
  error?: any;
}

interface IHeaderQuery {
  Catalogues: UIResource[];
  Variables_agg: { count: number };
  Collections_agg: { count: number };
  Networks_agg: { count: number };
  _settings: { key: "CATALOGUE_LOGO_SRC"; value: string }[];
}

export async function useHeaderData() {
  const route = useRoute();
  const config = useRuntimeConfig();
  const schema = config.public.schema as string;
  const scoped = route.params.catalogue && route.params.catalogue !== "all";
  const catalogueRouteParam = route.params.catalogue;

  const { data, error } = await $fetch<Resp<IHeaderQuery>>(
    `/${schema}/graphql`,
    {
      method: "POST",
      body: {
        query: `
            query HeaderQuery($collectionsFilter:CollectionsFilter, $variablesFilter:VariablesFilter, $catalogueFilter:CataloguesFilter,$networkFilter:NetworksFilter) {
              Catalogues(filter:$catalogueFilter) {
                id,
                logo { url }
              }
              Variables_agg(filter:$variablesFilter) {
                  count
              }
              Collections_agg(filter:$collectionsFilter) {
                  count
              }
              Networks_agg(filter:$networkFilter) {
                  count
              }
              _settings (keys: [
                "CATALOGUE_LOGO_SRC"
              ]){
                key
                value
              }
            }`,
        variables: {
          catalogueFilter: scoped
            ? { id: { equals: catalogueRouteParam } }
            : { mainCatalogue: { equals: true } },
          collectionsFilter: scoped
            ? {
                _or: [
                  { partOfNetworks: { id: { equals: catalogueRouteParam } } },
                  {
                    partOfNetworks: {
                      parentNetworks: { id: { equals: catalogueRouteParam } },
                    },
                  },
                ],
              }
            : undefined,
          networkFilter: scoped
            ? {
                _or: [
                  { parentNetworks: { id: { equals: catalogueRouteParam } } },
                  {
                    parentNetworks: {
                      parentNetworks: { id: { equals: catalogueRouteParam } },
                    },
                  },
                ],
              }
            : undefined,
          variablesFilter: scoped
            ? {
                _or: [
                  { resource: { id: { equals: catalogueRouteParam } } },
                  //also include network of networks
                  {
                    resource: {
                      partOfNetworks: {
                        id: { equals: catalogueRouteParam },
                      },
                    },
                  },
                ],
              }
            : {
                resource: {
                  _or: [
                    { mg_tableclass: { equals: `${schema}.Networks` } },
                    { mg_tableclass: { equals: `${schema}.Catalogues` } },
                  ],
                },
              },
        },
      },
    }
  );

  if (error) {
    const contextMsg = "Error on fetching page header data";
    logError(error, contextMsg);
    throw new Error(contextMsg);
  }

  const catalogue = data.Catalogues ? data.Catalogues[0] : undefined;
  const variableCount = data.Variables_agg.count || 0;
  const collectionCount = data.Collections_agg.count || 0;
  const networkCount = data.Networks_agg.count || 0;
  const logoSrc =
    catalogue?.logo?.url ??
    (
      data._settings.find(
        (setting) => setting.key === "CATALOGUE_LOGO_SRC"
      ) || {
        value: "",
      }
    ).value;

  return { catalogue, variableCount, collectionCount, networkCount, logoSrc };
}
