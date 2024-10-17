import type { UIResource, UIResourceType } from "~/interfaces/types";

interface Resp<T> {
  data: T;
  error?: any;
}

interface IHeaderQuery {
  Resources: UIResource[];
  Variables_agg: { count: number };
  Resources_groupBy: UIResourceType[];
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
            query HeaderQuery($resourceFilter:ResourcesFilter, $variablesFilter:VariablesFilter, $networksFilter:ResourcesFilter) {
              Resources(filter:$networksFilter) {
                id,
                logo { url }
              }
              Variables_agg(filter:$variablesFilter) {
                  count
              }
              Resources_groupBy(filter:$resourceFilter) {
                type { name, definition }
                count
              }
            }`,
        variables: {
          networksFilter: scoped
            ? { id: { equals: catalogueRouteParam } }
            : undefined,
          resourceFilter: scoped
            ? {
                _or: [
                  { partOfResources: { id: { equals: catalogueRouteParam } } },
                  {
                    partOfResources: {
                      partOfResources: { id: { equals: catalogueRouteParam } },
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
  const resourceTypes = data.Resources_groupBy.filter(
    (resourceType: { count: number }) => resourceType.count > 0
  );

  return { catalogue, variableCount, resourceTypes };
}
