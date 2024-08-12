import type { IMgError } from "../interfaces/types";

const method = "POST";

const modelQuery = `
      query Collections($networksFilter:CollectionsFilter) {
        Collections(filter:$networksFilter){ datasets { name } }
      }`;

const headerQuery = `
      query HeaderQuery($networksFilter:CollectionsFilter, $variablesFilter:VariablesFilter) {
        Collections(filter:$networksFilter) {
          id,
          collections_agg { count }
          logo { url }
       }
       Variables_agg(filter:$variablesFilter) {
          count
       }
    }`;

export async function useHeaderData() {
  const route = useRoute();

  const apiPath = `/${route.params.schema}/graphql`;

  const { data, error } = await useAsyncData<any, IMgError>(
    `catalogue-${route.params.catalogue}`,
    async () => {
      let variables:
        | {
            networksFilter: { id: { equals: string | string[] } };
            variablesFilter?: {
              collection: { id: { equals: string | string[] } };
            };
          }
        | undefined;
      const scoped = route.params.catalogue !== "all";
      const catalogueRouteParam = route.params.catalogue;
      const networksFilter = scoped
        ? { id: { equals: catalogueRouteParam } }
        : undefined;
      const datasets = await $fetch(`/${route.params.schema}/graphql`, {
        method: "POST",
        body: {
          query: `
            query CollectionDatasets($filter:CollectionDatasetsFilter) {
              CollectionDatasets(filter:$filter){name}
            }`,
          variables: {
            filter: {
              _or: [
                { collection: networksFilter },
                { collection: { partOfCollections: networksFilter } },
              ],
            },
          },
        },
      });
      const variablesFilter = scoped
        ? {
            collection: { id: { equals: catalogueRouteParam } },
            dataset: {
              name: {
                equals: datasets.data.CollectionDatasets?.map(
                  (d: { name: string }) => d.name
                ).flat(),
              },
            },
          }
        : { collection: { type: { name: { equals: "Network" } } } };

      return $fetch(apiPath, {
        key: `header-${route.params.catalogue}`,
        method,
        body: {
          query: headerQuery,
          variables: {
            networksFilter,
            variablesFilter,
          },
        },
      });
    }
  );

  if (error.value) {
    const contextMsg = "Error on fetching page header data";
    logError(error.value, contextMsg);
    throw new Error(contextMsg);
  }

  const catalogue = data.value.data?.Collections
    ? data.value.data?.Collections[0]
    : null;
  const variableCount = data.value.data?.Variables_agg.count || 0;

  return { catalogue, variableCount };
}
