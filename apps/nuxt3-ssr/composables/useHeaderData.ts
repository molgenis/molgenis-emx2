export async function useHeaderData() {
  const route = useRoute();
  const scoped = route.params.catalogue !== "all";
  const catalogueRouteParam = route.params.catalogue;

  const { data, error } = await $fetch(`/${route.params.schema}/graphql`, {
    key: `header-${route.params.catalogue}`,
    method: "POST",
    body: {
      query: `
            query HeaderQuery($networksFilter:ResourcesFilter, $variablesFilter:VariablesFilter) {
              Resources(filter:$networksFilter) {
                id,
                resources_agg { count }
                resources_groupBy { count , type {name}}
                logo { url }
            }
            Variables_agg(filter:$variablesFilter) {
                count
            }
          }`,
      variables: {
        networksFilter: scoped
          ? { id: { equals: catalogueRouteParam } }
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
  });

  if (error) {
    const contextMsg = "Error on fetching page header data";
    logError(error, contextMsg);
    throw new Error(contextMsg);
  }

  const catalogue = data.Resources ? data.Resources[0] : null;
  const variableCount = data.Variables_agg.count || 0;

  return { catalogue, variableCount };
}
