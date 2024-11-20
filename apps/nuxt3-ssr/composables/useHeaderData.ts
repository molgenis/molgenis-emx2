import type { UIResource } from "~/interfaces/types";

interface Resp<T> {
  data: T;
  error?: any;
}

interface IHeaderQuery {
  Resources: UIResource[];
  Variables_agg: { count: number };
  Collections_agg: { count: number };
  Networks_agg: { count: number };
}

export async function useHeaderData() {
  const route = useRoute();
  const scoped = route.params.catalogue !== "all";
  const catalogueRouteParam = route.params.catalogue;

  const { data, error } = await $fetch<Resp<IHeaderQuery>>(
    `/${route.params.schema}/graphql`,
    {
      method: "POST",
      body: {
        query: `
            query HeaderQuery($collectionsFilter:ResourcesFilter, $variablesFilter:VariablesFilter, $networksFilter:ResourcesFilter,$networkFilter:ResourcesFilter) {
              Resources(filter:$networksFilter) {
                id,
                logo { url }
              }
              Variables_agg(filter:$variablesFilter) {
                  count
              }
              Collections_agg: Resources_agg(filter:$collectionsFilter) {
                  count
              }
              Networks_agg: Resources_agg(filter:$networkFilter) {
                  count
              }
            }`,
        variables: {
          networksFilter: scoped
            ? { id: { equals: catalogueRouteParam } }
            : undefined,
          collectionsFilter: scoped
            ? {
                type: { tags: { equals: "collection" } },
                _or: [
                  { partOfResources: { id: { equals: catalogueRouteParam } } },
                  {
                    partOfResources: {
                      partOfResources: { id: { equals: catalogueRouteParam } },
                    },
                  },
                ],
              }
            : { type: { tags: { equals: "collection" } } },
          networkFilter: scoped
            ? {
                type: { tags: { equals: "network" } },
                _or: [
                  { partOfResources: { id: { equals: catalogueRouteParam } } },
                  {
                    partOfResources: {
                      partOfResources: { id: { equals: catalogueRouteParam } },
                    },
                  },
                ],
              }
            : { type: { tags: { equals: "network" } } },
          variablesFilter: scoped
            ? {
                _or: [
                  { resource: { id: { equals: catalogueRouteParam } } },
                  //also include network of networks
                  {
                    resource: {
                      type: { name: { equals: "Network" } },
                      partOfResources: {
                        id: { equals: catalogueRouteParam },
                      },
                    },
                  },
                ],
              }
            : //should only include harmonised variables
              { resource: { type: { name: { equals: "Network" } } } },
        },
      },
    }
  );

  if (error) {
    const contextMsg = "Error on fetching page header data";
    logError(error, contextMsg);
    throw new Error(contextMsg);
  }

  const catalogue = data.Resources ? data.Resources[0] : undefined;
  const variableCount = data.Variables_agg.count || 0;
  const collectionCount = data.Collections_agg.count || 0;
  const networkCount = data.Networks_agg.count || 0;
  return { catalogue, variableCount, collectionCount, networkCount };
}
